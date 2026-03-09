class EmotionMapper:
    # 1. 텍스트 모델 결과 -> 표준 라벨
    TEXT_TO_UNIFIED = {
        '분노': '분노',
        '슬픔': '슬픔',
        '상처': '슬픔',  # 상처는 표정상 슬픔으로 통합
        '불안': '불안',
        '당황': '당황',
        '기쁨': '기쁨'
    }

    # 2. 영상 모델 결과(DeepFace) -> 표준 라벨
    VIDEO_TO_UNIFIED = {
        'angry': '분노',
        'sad': '슬픔',
        'fear': '불안',
        'surprise': '당황',
        'happy': '기쁨',
        'neutral': '평온',
        'disgust': '분노' # 혐오는 주로 분노 계열로 통합
    }

    @classmethod
    def get_unified_text(cls, text_emotion):
        return cls.TEXT_TO_UNIFIED.get(text_emotion, '알 수 없음')

    @classmethod
    def get_unified_video(cls, video_emotion):
        return cls.VIDEO_TO_UNIFIED.get(video_emotion, '알 수 없음')