package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AssistantDialog : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.assistant_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val messageInput = view.findViewById<EditText>(R.id.messageInput)
        val sendButton = view.findViewById<Button>(R.id.sendButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val chatContainer = view.findViewById<LinearLayout>(R.id.chatContainer)

        // Добавляем приветственное сообщение
        addMessage(chatContainer, "Привет! Я помогу вам с вопросами о парковках в Москве.", false)

        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                // Добавляем сообщение пользователя
                addMessage(chatContainer, message, true)
                messageInput.text.clear()

                // Показываем прогресс бар
                progressBar.visibility = View.VISIBLE

                // Имитируем ответ ассистента (заглушка)
                chatContainer.postDelayed({
                    progressBar.visibility = View.GONE
                    addMessage(chatContainer, "Это тестовый ответ ассистента. Реальная интеграция с API будет добавлена позже.", false)

                    // Прокручиваем вниз
                    val scrollView = view.findViewById<ScrollView>(R.id.chatScrollView)
                    scrollView.post {
                        scrollView.fullScroll(View.FOCUS_DOWN)
                    }
                }, 2000)
            }
        }
    }

    private fun addMessage(container: LinearLayout, text: String, isUser: Boolean) {
        val inflater = LayoutInflater.from(context)
        val messageView = inflater.inflate(
            if (isUser) R.layout.message_user else R.layout.message_assistant,
            container,
            false
        ) as TextView

        messageView.text = text
        container.addView(messageView)
    }
}