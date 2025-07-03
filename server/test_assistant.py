import requests
import uuid
import json
from app.core.config import get_settings

# Отключение предупреждений о SSL
import urllib3

urllib3.disable_warnings()

settings = get_settings()


def get_giga_token() -> str:
    """Получает токен доступа для GigaChat API"""
    url = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
    headers = {
        'Content-Type': 'application/x-www-form-urlencoded',
        'RqUID': str(uuid.uuid4()),
        'Authorization': f'Basic {settings.sber_auth}'
    }
    payload = {'scope': 'GIGACHAT_API_PERS'}

    response = requests.post(url, headers=headers, data=payload, verify=False)
    response.raise_for_status()
    return response.json()['access_token']


def ask_gigachat(question: str) -> str:
    """Отправляет вопрос в GigaChat и возвращает ответ"""
    token = get_giga_token()
    url = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"
    headers = {
        'Content-Type': 'application/json',
        'Authorization': f'Bearer {token}'
    }
    payload = json.dumps({
        "model": "GigaChat",
        "messages": [{"role": "user", "content": question}],
        "temperature": 1,
        "max_tokens": 1024
    })

    response = requests.post(url, headers=headers, data=payload, verify=False)
    response.raise_for_status()
    return response.json()['choices'][0]['message']['content']


if __name__ == "__main__":
    print("Ассистент готов. Задайте вопрос (exit для выхода):")
    while True:
        try:
            question = input("> ")
            if question.lower() == 'exit':
                break
            response = ask_gigachat(question)
            print(f"\n🤖 {response}\n")
        except Exception as e:
            print(f"⚠️ Ошибка: {str(e)}")