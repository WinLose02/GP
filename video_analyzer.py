import cv2
import numpy as np
import mediapipe as mp
import torch
import torch.nn.functional as F
from mediapipe.tasks import python
from mediapipe.tasks.python import vision
from deepface import DeepFace

class VideoAnalyzer:
    def __init__(self, model_path='detector.tflite'):
        # 1. MediaPipe 설정
        base_options = python.BaseOptions(model_asset_path=model_path)
        options = vision.FaceDetectorOptions(
            base_options=base_options,
            running_mode=vision.RunningMode.VIDEO
        )
        self.detector = vision.FaceDetector.create_from_options(options)
        
    def clamp_bbox(self, x, y, w, h, W, H):
        x1, y1 = max(0, int(x)), max(0, int(y))
        x2, y2 = min(W, int(x + w)), min(H, int(y + h))
        return (x1, y1, x2, y2) if x2 > x1 and y2 > y1 else None
    
    def label_mapper(self, raw_emo):
        #분노, 슬픔, 불안, 당황, 기쁨
        mapped = [0.0, 0.0, 0.0, 0.0, 0.0]

        mapped[0] += raw_emo.get('angry', 0)
        mapped[0] += raw_emo.get('disgust', 0)
        mapped[1] += raw_emo.get('sad', 0)
        mapped[2] += raw_emo.get('fear', 0)
        mapped[3] += raw_emo.get('surprise', 0)
        mapped[4] += raw_emo.get('happy', 0)

        confidence_score = torch.tensor(mapped)
        return confidence_score

    def predict(self, face_rgb):
        # DeepFace 분석 후 EmotionMapper로 라벨 통합
        backends = ["skip", "opencv"]
        for backend in backends:
            try:
                res = DeepFace.analyze(
                    img_path=face_rgb,
                    actions=["emotion"],
                    enforce_detection=False,
                    detector_backend=backend,
                    silent=True
                )
                if isinstance(res, list): res = res[0]
                
                raw_emo = res.get("emotion", {})
                probs = self.label_mapper(raw_emo)
                result = F.softmax(probs, dim=0)
                return result
            except:
                continue
        return None

    def analyze_video(self, input_path, sample_rate=10):
        """
        영상 내 감정 분석 결과만 반환
        sample_rate: 모든 프레임을 분석하려면 1, 10프레임당 1번 하려면 10 설정
        """
        cap = cv2.VideoCapture(input_path)
        if not cap.isOpened():
            print(f"Error: {input_path}를 열 수 없습니다.")
            return []

        fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
        frame_idx = 0
        results = []

        print(f"분석 시작: {input_path}")

        while True:
            ret, frame_bgr = cap.read()
            if not ret: break

            # 성능을 위해 sample_rate에 따라 프레임 건너뛰기
            if frame_idx % sample_rate == 0:
                frame_rgb = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB)
                timestamp_ms = int((frame_idx / fps) * 1000)
                mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=frame_rgb)
                
                detection_result = self.detector.detect_for_video(mp_image, timestamp_ms)

                for detection in detection_result.detections:
                    bbox = detection.bounding_box
                    c = self.clamp_bbox(bbox.origin_x, bbox.origin_y, bbox.width, bbox.height, 
                                        frame_bgr.shape[1], frame_bgr.shape[0])
                    if not c: continue
                    x1, y1, x2, y2 = c

                    face_rgb_crop = frame_rgb[y1:y2, x1:x2]
                    if face_rgb_crop.size == 0: continue
                    
                    confidence_score = self.predict(face_rgb_crop)
                    
                    if confidence_score is not None:
                        results.append({
                            'frame': frame_idx,
                            'timestamp_sec': round(frame_idx / fps, 2),
                            'prob': confidence_score
                        })

            frame_idx += 1
            if frame_idx % 100 == 0:
                print(f"--- {frame_idx} 프레임 분석 중 ---")

        cap.release()
        print(f"분석 완료. 총 {len(results)}개의 감정 데이터 추출됨.")
        return results