import requests
import uuid
import json
import logging
from app.core.config import get_settings

log = logging.getLogger("assistant")


class GigaChatAssistant:
    def __init__(self, system_prompt: str, max_tokens: int, temperature: float, top_p: float):
        settings = get_settings()
        self.sber_auth = settings.sber_auth
        self.system_prompt = system_prompt
        self.max_tokens = max_tokens
        self.temperature = temperature
        self.top_p = top_p

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

        messages = [
            {"role": "system", "content": self.system_prompt},
            {"role": "user", "content": question}
        ]

        payload = json.dumps({
            "model": "GigaChat",
            "messages": messages,
            "temperature": self.temperature,
            "max_tokens": self.max_tokens,
            "top_p": self.top_p
        })

        try:
            response = requests.post(url, headers=headers, data=payload, verify=False, timeout=15)
            response.raise_for_status()
            response_data = response.json()

            # Проверка наличия ожидаемой структуры ответа
            if 'choices' not in response_data or not response_data['choices']:
                log.error("Invalid response structure from GigaChat API")
                return "Не удалось обработать ответ сервиса"

            return response_data['choices'][0]['message']['content']

        except requests.exceptions.RequestException as e:
            log.error(f"GigaChat API request failed: {str(e)}")
            return "Ошибка соединения с сервисом"
        except json.JSONDecodeError:
            log.error("Failed to parse GigaChat API response")
            return "Ошибка обработки ответа сервиса"