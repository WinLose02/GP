import torch
import torch.nn as nn
from transformers import AutoTokenizer, AutoModel
import torch.nn.functional as F

# 1. 학습 때와 동일한 모델 구조 정의
class Classifier(nn.Module):
    def __init__(self, model, num_labels):
        super().__init__()
        self.bert = model
        self.dropout = nn.Dropout(p=0.1)
        self.classifier = nn.Linear(768, num_labels)

    def forward(self, input_ids, attention_mask):
        outputs = self.bert(input_ids=input_ids, attention_mask=attention_mask)
        # ELECTRA/BERT의 [CLS] 토큰 위치는 0번입니다.
        cls_token = outputs.last_hidden_state[:, 0, :]
        x = self.dropout(cls_token)
        logits = self.classifier(x)
        return logits

# 2. 설정 및 환경 준비
MODEL_NAME = "beomi/KcELECTRA-base-v2022"
MAX_LEN = 128
DEVICE = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# 레이블 맵 (학습 때와 순서가 반드시 동일해야 합니다)
LABELMAP = {0: '분노', 1: '슬픔', 2: '불안', 3: '상처', 4: '당황', 5: '기쁨'}

# 3. 모델 및 토크나이저 로드
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
base_model = AutoModel.from_pretrained(MODEL_NAME)

# 모델 인스턴스 생성 후 저장된 가중치 로드
model = Classifier(base_model, num_labels=len(LABELMAP))
model.load_state_dict(torch.load("best_model_2.pt", map_location=DEVICE))
model.to(DEVICE)
model.eval() # 추론 모드로 설정 (드롭아웃 비활성화)

print(f"모델 로드 완료 ({DEVICE})")

# 4. 예측 함수 정의
def predict_emotion(text):
    # 입력 텍스트 토큰화
    encoding = tokenizer(
        text,
        add_special_tokens=True,
        max_length=MAX_LEN,
        padding='max_length',
        truncation=True,
        return_tensors='pt'
    )

    input_ids = encoding['input_ids'].to(DEVICE)
    attention_mask = encoding['attention_mask'].to(DEVICE)

    with torch.no_grad():
        logits = model(input_ids, attention_mask)
        # Softmax를 통해 확률값으로 변환
        probs = F.softmax(logits, dim=1)
        # 가장 높은 확률의 인덱스 추출
        pred_idx = torch.argmax(probs, dim=1).item()
        confidence = probs[0][pred_idx].item()

    return LABELMAP[pred_idx], confidence

# 5. 사용자 입력 루프
print("\n--- 감정 분류 테스트 시작 (종료하려면 'exit' 입력) ---")
while True:
    user_input = input("\n분석할 문장을 입력하세요: ")
    if user_input.lower() == 'exit':
        break
    
    emotion, score = predict_emotion(user_input)
    print(f"결과: [{emotion}] (확신도: {score*100:.2f}%)")