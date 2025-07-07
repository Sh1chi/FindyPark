from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.services.assistant import GigaChatAssistant

router = APIRouter()
assistant = GigaChatAssistant()

class QuestionRequest(BaseModel):
    question: str

@router.post("/ask")
async def ask_question(request: QuestionRequest):
    try:
        answer = assistant.ask_gigachat(request.question)
        return {"answer": answer}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))