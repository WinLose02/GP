import torch
import torch.nn as nn
import torch.nn.functional as F
from transformers import AutoTokenizer, AutoModel

# 1. 내부 모델 구조 정의 (Private 클래스 취급)
class _Classifier(nn.Module):
    def __init__(self, model, num_labels):
        super().__init__()
        self.bert = model
        self.dropout = nn.Dropout(p=0.1)
        self.classifier = nn.Linear(768, num_labels)

    def forward(self, input_ids, attention_mask):
        outputs = self.bert(input_ids=input_ids, attention_mask=attention_mask)
        cls_token = outputs.last_hidden_state[:, 0, :]
        x = self.dropout(cls_token)
        logits = self.classifier(x)
        return logits

# 2. 메인 분석기 클래스
class TextAnalyzer:
    def __init__(self, model_path="best_model_2.pt", model_name="beomi/KcELECTRA-base-v2022", max_len=128):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.max_len = max_len
        self.label_map = {0: '분노', 1: '슬픔', 2: '불안', 3: '상처', 4: '당황', 5: '기쁨'}
        
        print(f"[{self.device}] 모델 로딩 중...")
        
        # 토크나이저 및 베이스 모델 로드
        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        base_model = AutoModel.from_pretrained(model_name)
        
        # 커스텀 분류기 생성 및 가중치 로드
        self.model = _Classifier(base_model, num_labels=len(self.label_map))
        self.model.load_state_dict(torch.load(model_path, map_location=self.device))
        self.model.to(self.device)
        self.model.eval()
        
        print("모델 로드 완료.")

    def predict(self, text):
        encoding = self.tokenizer(
            text,
            add_special_tokens=True,
            max_length=self.max_len,
            padding='max_length',
            truncation=True,
            return_tensors='pt'
        )

        input_ids = encoding['input_ids'].to(self.device)
        attention_mask = encoding['attention_mask'].to(self.device)

        with torch.no_grad():
            logits = self.model(input_ids, attention_mask)
            probs = F.softmax(logits, dim=1)
        
        probs[0, 1] += probs[0, 3]
        keep = [0, 1, 2, 4, 5]
        result = probs[:, keep]
        result = result / result.sum(dim=1, keepdim=True)  # 재정규화
        return result.squeeze(0)