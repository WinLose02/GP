from fastapi import FastAPI, UploadFile, File, Form, HTTPException, Request
from contextlib import asynccontextmanager
import datetime
import shutil
import os
import uuid
import torch
import firebase_admin
from firebase_admin import credentials, firestore

from text_analyzer import TextAnalyzer
from video_analyzer import VideoAnalyzer

# =============================================================================
# Firestore 설정
# TODO: 서비스 계정 키 JSON 파일명을 아래에 입력하세요
# Firebase 콘솔 → 프로젝트 설정 → 서비스 계정 → 새 비공개 키 생성
# =============================================================================
SERVICE_ACCOUNT_KEY = "path/to/serviceAccountKey.json"  # ← 여기에 JSON 파일명 입력

cred = credentials.Certificate(SERVICE_ACCOUNT_KEY)
firebase_admin.initialize_app(cred)
db = firestore.client()

# =============================================================================
# 앱 생명주기 — 서버 시작 시 AI 모델 1회 로드
# =============================================================================
@asynccontextmanager
async def lifespan(app: FastAPI):
    print("=== 서버 시작: AI 모델 로딩 중... ===")
    app.state.text_analyzer = TextAnalyzer()
    app.state.video_analyzer = VideoAnalyzer()
    print("=== 모델 로드 완료. 서버 준비됨. ===")
    yield
    print("=== 서버 종료 ===")

# =============================================================================
# FastAPI 앱 설정
# =============================================================================
app = FastAPI(lifespan=lifespan)

LABEL_MAP = {0: '분노', 1: '슬픔', 2: '불안', 3: '당황', 4: '기쁨'}

# =============================================================================
# 감정 집계 / 결정 로직
# =============================================================================
def aggregate_video_probs(frame_results: list):
    """
    프레임별 감정 텐서를 평균 내어 영상 전체의 감정 확률 반환.
    - 반환값: Tensor(5,) 또는 None (검출된 얼굴 없을 때)
    """
    if not frame_results:
        return None
    stacked = torch.stack([r['prob'] for r in frame_results])  # (N, 5)
    return stacked.mean(dim=0)                                  # (5,)

def get_dominant_emotion(t_prob, v_prob, text_weight=0.6, video_weight=0.4):
    """
    텍스트/영상 감정 확률을 가중 평균하여 최종 감정 레이블 반환.
    - t_prob : Tensor(5,) — TextAnalyzer.predict() 결과
    - v_prob : Tensor(5,) 또는 None — 영상 분석 실패 시 텍스트 단독 사용
    """
    if v_prob is None:
        combined = t_prob
    else:
        combined = text_weight * t_prob + video_weight * v_prob

    dominant_idx = combined.argmax().item()
    return LABEL_MAP[dominant_idx], combined

def get_single_emotion(prob):
    if prob is None:
        return None
    idx = prob.argmax().item()
    return LABEL_MAP[idx]


# =============================================================================
# API 엔드포인트
# =============================================================================
@app.post("/analyze")
async def create_analysis(
    request: Request,
    text: str = Form(...),
    video: UploadFile = File(...),
):
    # 1. 영상 파일 저장
    upload_dir = "uploads"
    os.makedirs(upload_dir, exist_ok=True)
    ext = os.path.splitext(video.filename)[-1]
    unique_name = f"{uuid.uuid4().hex}{ext}"
    file_path = os.path.join(upload_dir, unique_name)

    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(video.file, buffer)

    try:
        # 2. 분석기 인스턴스 가져오기
        text_analyzer: TextAnalyzer = request.app.state.text_analyzer
        video_analyzer: VideoAnalyzer = request.app.state.video_analyzer

        # 3. 텍스트 감정 분석
        t_prob = text_analyzer.predict(text)            # Tensor(5,)
        t_emo = get_single_emotion(t_prob)

        # 4. 영상 감정 분석
        frame_results = video_analyzer.analyze_video(file_path)
        v_prob = aggregate_video_probs(frame_results)   # Tensor(5,) or None
        v_emo = get_single_emotion(v_prob)

        # 5. 최종 감정 결정
        emotion_label, combined_prob = get_dominant_emotion(t_prob, v_prob)

        # 6. Firestore에 결과 저장
        prob_dict = {LABEL_MAP[i]: round(combined_prob[i].item(), 4) for i in range(5)}
        doc_ref = db.collection("analysis_results").add({
            "text_content": text,
            "video_path": file_path,
            "emotion_label": emotion_label,
            "prob": prob_dict,
            "text_emotion": t_emo,
            "video_emotion": v_emo,
            "created_at": datetime.datetime.utcnow()
        })
        doc_id = doc_ref[1].id  # Firestore 자동 생성 문서 ID

        return {
            "status": "success",
            "emotion": emotion_label,
            "prob": prob_dict,
            "db_id": doc_id
        }

    except Exception as e:
        # 예외 발생 시 저장된 영상 파일 정리
        if os.path.exists(file_path):
            os.remove(file_path)
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)