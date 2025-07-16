package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class AssistantDialog : BottomSheetDialogFragment() {

    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var chatContainer: LinearLayout
    private lateinit var scrollView: ScrollView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.assistant_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация UI элементов
        messageInput = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)
        progressBar = view.findViewById(R.id.progressBar)
        chatContainer = view.findViewById(R.id.chatContainer)
        scrollView = view.findViewById(R.id.chatScrollView)

        // Добавляем приветственное сообщение
        addMessage("Привет, меня зовут Паркин! Я помогу вам с вопросами об интерфейсе приложения: Личный кабинет, Меню, Карта.", false)

        // Обработчик отправки сообщений
        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                // Добавляем сообщение пользователя
                addMessage(message, true)
                messageInput.text.clear()

                // Показываем прогресс бар
                progressBar.visibility = View.VISIBLE

                // Отправляем запрос на сервер
                sendQuestionToServer(message)
            }
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        val inflater = LayoutInflater.from(context)
        val layoutRes = if (isUser) R.layout.message_user else R.layout.message_assistant
        val messageView = inflater.inflate(layoutRes, chatContainer, false) as TextView

        messageView.text = text
        chatContainer.addView(messageView)

        // Прокручиваем вниз
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun sendQuestionToServer(question: String) {
        val client = OkHttpClient()

        // Формируем JSON запрос
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val json = JSONObject().apply {
            put("prompt", SYSTEM_PROMPT) // Добавляем системный промт
            put("question", question)
        }
        val body = json.toString().toRequestBody(mediaType)

        // Замените YOUR_SERVER_URL на реальный URL вашего сервера
        val request = Request.Builder()
            .url("http://192.168.0.10:8000/assistant/ask")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    progressBar.visibility = View.GONE
                    addMessage("Ошибка сети: ${e.message}", false)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseBody ->
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val answer = jsonResponse.getString("answer")

                        activity?.runOnUiThread {
                            progressBar.visibility = View.GONE
                            addMessage(answer, false)
                        }
                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            progressBar.visibility = View.GONE
                            addMessage("Ошибка обработки ответа сервера", false)
                        }
                    }
                } ?: run {
                    activity?.runOnUiThread {
                        progressBar.visibility = View.GONE
                        addMessage("Пустой ответ от сервера", false)
                    }
                }
            }




        })


    }
    private val SYSTEM_PROMPT = """
    Ты — ассистент в приложении для поиска парковок "FindyPark". Твоя единственная задача — отвечать на вопросы пользователей строго о функционале и интерфейсе текущей версии приложения. Текущий интерфейс включает только: Личный кабинет (ЛК), Карту парковок, Настройки темы (светлая/темная), Основное Меню приложения.

    Ключевые правила:
    1. Отвечай ТОЛЬКО на вопросы, связанные с использованием ЛК, Карты или Меню
    2. Ответы должны быть предельно краткими
    3. На вопрос "Что ты умеешь?" отвечай ТОЛЬКО: "Я могу помочь с разделами: Личный кабинет, Карта парковок, Меню"
    4. На все другие темы отвечай: "Извините, я могу помочь только с вопросами об интерфейсе приложения"
    5. Не предлагай идей и не предсказывай будущий функционал
    6. Используй только чистый текст без форматирования
""".trimIndent()


}