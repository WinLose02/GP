from fastapi import FastAPI, UploadFile, File, Form, HTTPException, Depends, Request
from sqlalchemy import create_engine, Column, Integer, String, Text, DateTime
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session
from contextlib import asynccontextmanager
import datetime
import shutil
import os
import uuid
import torch

from text_analyzer import TextAnalyzer
from video_analyzer import VideoAnalyzer

# --- [DB 설정] ---
# 사용자 정보에 맞게 수정하세요: 'mysql+pymysql://user:password@host:port/dbname'
DATABASE_URL = "mysql+pymysql://root:password@localhost:3306/emotion_db"

engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# --- [DB 테이블 정의] ---
class EmotionResult(Base):
    __tablename__ = "analysis_results"
    id = Column(Integer, primary_key=True, index=True)
    text_content = Column(Text)
    emotion_label = Column(String(50))
    video_path = Column(String(255))
    created_at = Column(DateTime, default=datetime.datetime.utcnow)

Base.metadata.create_all(bind=engine)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 서버 시작 시 한 번만 실행
    print(" 서버 시작: AI 모델 로딩 중...")
    app.state.text_analyzer = TextAnalyzer()
    app.state.video_analyzer = VideoAnalyzer()
    print(" 모델 로드 완료. 서버 준비됨.")
    yield
    print(" 서버 종료 ")
    # 서버 종료 시 정리 (필요하면)

# --- [FastAPI 앱 설정] ---
app = FastAPI(lifespan=lifespan)

LABEL_MAP = {0: '분노', 1: '슬픔', 2: '불안', 3: '당황', 4: '기쁨'}

def aggregate_video_probs(frame_results: list):
    """
    프레임별 감정 텐서를 평균 내어 영상 전체의 감정 확률 반환.
    - 반환값: Tensor(5,) 또는 None (검출된 얼굴 없을 때)
    """
    if not frame_results:
        return None
    stacked = torch.stack([r['prob'] for r in frame_results])  # (N, 5)
    return stacked.mean(dim=0) 

# --- [최종 감정 결정 로직] ---
def get_dominant_emotion(t_prob, v_prob, text_weight=0.6, video_weight=0.4):
    """
    텍스트/영상 감정 확률을 가중 평균하여 최종 감정 레이블 반환.
    - t_prob : Tensor(5,) — TextAnalyzer.predict() 결과
    - v_prob : Tensor(5,) 또는 None — 영상 분석 결과 (얼굴 미검출 시 None)
    - 영상 분석 실패 시 텍스트 결과만 사용
    """
    if v_prob is None:
        # 영상에서 얼굴을 검출하지 못한 경우 텍스트 단독 사용
        combined = t_prob
    else:
        combined = text_weight * t_prob + video_weight * v_prob  # (5,)

    dominant_idx = combined.argmax().item()
    return LABEL_MAP[dominant_idx], combined

@app.post("/analyze")
async def create_analysis(
    request: Request,
    text: str = Form(...), 
    video: UploadFile = File(...),
    db: Session = Depends(get_db)   
):
    # 1. 영상 파일 저장
    upload_dir = "uploads"
    os.makedirs(upload_dir, exist_ok=True)
    ext = os.path.splitext(video.filename)[-1]          # 확장자 추출 (.mp4 등)
    unique_name = f"{uuid.uuid4().hex}{ext}"
    file_path = os.path.join(upload_dir, unique_name)
    
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(video.file, buffer)

    try:
        # --- 2. 분석기 인스턴스 가져오기 ---
        text_analyzer: TextAnalyzer = request.app.state.text_analyzer
        video_analyzer: VideoAnalyzer = request.app.state.video_analyzer

        # --- 3. 텍스트 감정 분석 ---
        t_prob = text_analyzer.predict(text)            # Tensor(5,)

        # --- 4. 영상 감정 분석 ---
        frame_results = video_analyzer.analyze_video(file_path)
        v_prob = aggregate_video_probs(frame_results)   # Tensor(5,) or None

        # --- 5. 최종 감정 결정 ---
        emotion_label, combined_prob = get_dominant_emotion(t_prob, v_prob)

        # --- 6. DB 저장 ---
        new_record = EmotionResult(
            text_content=text,
            emotion_label=emotion_label,
            video_path=file_path
        )
        db.add(new_record)
        db.commit()
        db.refresh(new_record)

        return {
            "status": "success",
            "emotion": emotion_label,
            "prob": {
                LABEL_MAP[i]: round(combined_prob[i].item(), 4) for i in range(5)
            },
            "db_id": new_record.id
        }

    except Exception as e:
        # 예외 발생 시 저장된 영상 파일 정리
        if os.path.exists(file_path):
            os.remove(file_path)
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
