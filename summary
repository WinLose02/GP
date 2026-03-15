# core/summry.py
from openai import OpenAI

client = OpenAI()

# ==========================
# 1) Prompts
# ==========================

EMOTION_PROMPT = """
너는 사용자의 일기를 읽고 오늘의 감정을 분석하는 역할이다.

규칙:

1. 아래 7가지 중 하나만 선택한다:
   기쁨, 슬픔, 분노, 불안, 놀람, 혐오, 중립

2. 가장 지배적인 감정 하나만 선택한다.

3. 아래 형식을 정확히 따른다:

오늘의 감정은 [감정] 이야!

그 감정이 나타난 이유를 일기 내용에 근거하여 1문장으로 설명한다.
- 원문에 없는 정보를 추가하지 않는다.
- 과도한 해석을 하지 않는다.
- 일기에 적힌 내용을 기반으로 분석한다

행운의 메신져:
- 포춘쿠키처럼 짧고 가볍게 작성한다.
- 조언이나 지시 형태로 작성하지 않는다.
- 2문장 이내로 작성한다.
- 내일의 사용자에게 응원의 메세지로 작성한다.
- 너무 뻔한 멘트는 작성하지 않는다.
- 가볍고 자연스러운 비유/은유를 사용해 짧지만 여운이 남게 작성한다.
- 그날의 일기에서 나온 [감정]에 따라 작성한다.
[기쁨]
- 밝고 가벼운 이미지로 “좋은 흐름이 이어지는 느낌”을 준다.
[슬픔]
- 따뜻하고 조용한 이미지로 “부드럽게 감싸주는 느낌”을 준다.
[분노]
- 뜨거운 감정이 지나가며 정리되는 이미지로 “가라앉는 여운”을 준다.
[불안]
- 안정감을 주는 이미지로 “길이 보이는 느낌”을 준다.
[놀람]
- 변화/전환의 이미지로 “새로운 국면”의 느낌을 준다.
[혐오]
- 거리두기/정리의 이미지로 “개운한 환기” 느낌을 준다.
[중립]
- 담백하고 균형 잡힌 이미지로 “차분한 흐름”을 준다.
"""

REFINE_PROMPT = """
너는 사용자가 작성한 일기를
의미를 변경하지 않고 읽기 좋게 정리하는 역할이다.

규칙:
- 원문의 의미와 감정을 유지한다.
- 새로운 정보나 해석을 추가하지 않는다.
- 문장을 자연스럽게 다듬는다.
- 과장하거나 감정을 증폭하지 않는다.
- 문단 흐름이 자연스럽도록 정리한다.
- 3문장 이내로 정리한다.
"""

BRIEF_SUMMARY_PROMPT = """
너는 사용자의 일기를 매우 간결하게 요약하는 역할이다.

규칙:
- 1~2문장으로 작성한다.
- 핵심 사건과 감정을 중심으로 작성한다.
- 판단, 해석, 조언을 추가하지 않는다.
- 원문에 없는 내용을 추가하지 않는다.
- 기록용으로 중립적인 톤을 유지한다.
"""

KEYWORD_PROMPT = """
너는 사용자의 '일기 요약문'에서 기억 저장과 이후 상담 참조에 사용할 핵심 키워드를 추출하는 역할이다.

규칙:
- 요약문에 담긴 핵심 주제, 고민, 관계, 상황을 드러내는 키워드 3~5개를 추출한다.
- 너무 일반적인 단어(예: 마음, 생각, 하루, 느낌)는 제외한다.
- 요약문에 실제로 드러난 표현을 우선 사용한다.
- 짧은 단어 또는 짧은 구 형태로 작성한다.
- 결과는 쉼표로 구분된 키워드 목록만 출력한다.
- 설명은 덧붙이지 않는다.
"""

COUNSEL_SUMMARY_PROMPT = """
너는 사용자의 '상담 대화'를 짧게 정리하는 역할이다.

규칙:
- 일기 원문 자체보다, 상담에서 드러난 사용자의 '핵심 고민'을 중심으로 요약한다.
- 사용자가 반복해서 말한 생각/걱정/바라는 점을 우선적으로 반영한다.
- 상담에서 나온 방향성(다음에 해볼 행동/관점)이 있다면 1문장 정도 포함한다.
- 단순히 일기 내용을 다시 줄이는 것이 아니라, 상담의 핵심 흐름을 정리한다.
- 2~3문장으로 작성한다.
- 기록용으로 차분하고 정리된 문체를 사용한다.
"""

# ==========================
# 2) Helpers
# ==========================

def _safe_text(text: str, limit: int = 800) -> str:
    return (text or "").strip()[:limit]


def _chat(system_prompt: str, user_text: str, temperature: float = 0.2, model: str = "gpt-4o-mini") -> str:
    user_text = _safe_text(user_text)

    print("LLM 호출 시작")

    try:
        resp = client.chat.completions.create(
            model=model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_text},
            ],
            temperature=temperature,
        )

        result = (resp.choices[0].message.content or "").strip()
        print("LLM 호출 완료")
        return result

    except Exception as e:
        print("OpenAI 호출 오류:", e)
        raise


# ==========================
# 3) Public APIs
# ==========================

def detect_emotion(diary_text: str) -> dict:
    raw_text = _chat(EMOTION_PROMPT, diary_text, temperature=0.0)

    emotion_label = "중립"
    reason = ""
    fortune = ""

    try:
        lines = [ln.strip() for ln in raw_text.splitlines() if ln.strip()]

        if lines and "오늘의 감정은" in lines[0]:
            emotion_label = (
                lines[0]
                .replace("오늘의 감정은", "")
                .replace("이야!", "")
                .replace("입니다", "")
                .strip()
            )

        if len(lines) >= 2:
            reason = lines[1]

        if "행운의 메신져:" in raw_text:
            fortune = raw_text.split("행운의 메신져:", 1)[1].strip()

    except Exception as e:
        print("emotion parsing error:", e)

    return {
        "label": emotion_label,
        "reason": reason,
        "fortune": fortune
    }


def refine_diary(diary_text: str) -> str:
    return _chat(REFINE_PROMPT, diary_text, temperature=0.3)


def brief_summary(diary_text: str) -> str:
    return _chat(BRIEF_SUMMARY_PROMPT, diary_text, temperature=0.2)


def extract_keywords(summary_text: str) -> list[str]:
    raw = _chat(KEYWORD_PROMPT, summary_text, temperature=0.2)
    keywords = [k.strip() for k in raw.split(",") if k.strip()]

    seen = set()
    clean = []

    for k in keywords:
        if k not in seen:
            seen.add(k)
            clean.append(k)

    return clean[:5]


def summarize_counsel(counsel_dialogue_text: str) -> str:
    return _chat(COUNSEL_SUMMARY_PROMPT, counsel_dialogue_text, temperature=0.2)
