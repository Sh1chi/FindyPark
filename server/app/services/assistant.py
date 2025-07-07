import requests
import uuid
import json
import logging
from app.core.config import get_settings

log = logging.getLogger("assistant")

class GigaChatAssistant:
    def __init__(self):
        settings = get_settings()
        self.sber_auth = settings.sber_auth
        if not self.sber_auth:
            raise RuntimeError("SBER_AUTH not configured")

    def get_giga_token(self) -> str:
        url = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
        headers = {
            'Content-Type': 'application/x-www-form-urlencoded',
            'RqUID': str(uuid.uuid4()),
            'Authorization': f'Basic {self.sber_auth}'
        }
        payload = {'scope': 'GIGACHAT_API_PERS'}

        response = requests.post(url, headers=headers, data=payload, verify=False, timeout=10)
        response.raise_for_status()
        return response.json()['access_token']

    def ask_gigachat(self, question: str) -> str:
        token = self.get_giga_token()
        url = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"
        headers = {
            'Content-Type': 'application/json',
            'Authorization': f'Bearer {token}'
        }
        payload = json.dumps({
            "model": "GigaChat",
            "messages": [{"role": "user", "content": question}],
            "temperature": 1,
            "max_tokens": 512
        })

        response = requests.post(url, headers=headers, data=payload, verify=False, timeout=15)
        response.raise_for_status()
        return response.json()['choices'][0]['message']['content']