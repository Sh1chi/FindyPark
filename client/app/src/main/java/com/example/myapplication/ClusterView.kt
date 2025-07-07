import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

class ClusterView(context: Context) : FrameLayout(context) {

    private val textView: TextView

    init {
        // Настройка круглого фона
        val backgroundDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.WHITE) // Синий фон
            setStroke(5, Color.RED) // Белая обводка

        }

        // Сам текст (число)
        textView = TextView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                setPadding(32, 32, 32, 32)
            }
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            textSize = 16f
            setBackgroundDrawable(backgroundDrawable)
        }

        addView(textView)
    }

    fun setText(text: String) {
        textView.text = text
    }
}
