package com.example.vcs_project5

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.*
import android.text.style.*
import android.view.View
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.example.vcs_project5.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val currentStyles = mutableMapOf<String, Any>()
    private var isUpdating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initToolbar()
        initTyping()
    }
    private fun initToolbar() {
        binding.btnBold.setOnClickListener {
            toggleStyle("BOLD", StyleSpan(Typeface.BOLD), binding.btnBold)
        }
        binding.btnItalic.setOnClickListener {
            toggleStyle("ITALIC", StyleSpan(Typeface.ITALIC), binding.btnItalic)
        }
        binding.btnUnderline.setOnClickListener {
            toggleStyle("UNDERLINE", UnderlineSpan(), binding.btnUnderline)
        }
        binding.btnColor.setOnClickListener { showColorPicker() }
        binding.btnSize.setOnClickListener { showSizePicker() }
    }
    private fun toggleStyle(key: String, span: Any, btn: View) {
        val (start, end) = getSelection()
        val text = binding.editText.text
        if (start == end) {
            if (currentStyles.containsKey(key)) {
                currentStyles.remove(key)
                setActive(btn, false)
            } else {
                currentStyles[key] = span
                setActive(btn, true)
            }
            return
        }
        val existing = text.getSpans(start, end, span.javaClass)
        val target = if (span is StyleSpan) {
            existing.filterIsInstance<StyleSpan>()
                .find { it.style == span.style }
        } else {
            existing.firstOrNull()
        }
        if (target != null) {
            text.removeSpan(target)
            setActive(btn, false)
        } else {
            text.setSpan(
                copySpan(span),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            setActive(btn, true)
        }
    }
    private fun applyStyle(span: Any) {
        val (start, end) = getSelection()
        val key = getKey(span)
        if (start == end) {
            currentStyles[key] = span
        } else {
            binding.editText.text.setSpan(
                copySpan(span),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    private fun initTyping() {
        binding.editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating || s == null) return
                val cursor = binding.editText.selectionStart
                if (cursor <= 0) return
                isUpdating = true

                val lastChar = s[cursor - 1]
                if (lastChar == '\n') {
                    val prevIndex = cursor - 2
                    if (prevIndex >= 0) {
                        val spans = s.getSpans(prevIndex, prevIndex + 1, Any::class.java)
                        spans.forEach { span ->
                            s.setSpan(
                                copySpan(span),
                                cursor,
                                cursor,
                                Spanned.SPAN_INCLUSIVE_INCLUSIVE
                            )
                        }
                    }
                }
                currentStyles.values.forEach { span ->
                    val exist = s.getSpans(cursor - 1, cursor, span.javaClass)
                    val applied = if (span is StyleSpan) {
                        exist.filterIsInstance<StyleSpan>()
                            .any { it.style == span.style }
                    } else {
                        exist.isNotEmpty()
                    }
                    if (!applied) {
                        s.setSpan(
                            copySpan(span),
                            cursor - 1,
                            cursor,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
                isUpdating = false
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
    @SuppressLint("InflateParams")
    private fun showSizePicker() {
        val view = layoutInflater.inflate(R.layout.dialog_size_picker, null)
        val list = view.findViewById<ListView>(R.id.listSize)
        val sizes = listOf(10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 40)
        list.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            sizes.map { "Size $it" }
        )

        val dialog = createDialog(view)
        list.setOnItemClickListener { _, _, pos, _ ->
            val span = AbsoluteSizeSpan(sizes[pos], true)
            currentStyles["SIZE"] = span
            applyStyle(span)
            dialog.dismiss()
        }
        dialog.show()
    }
    @SuppressLint("InflateParams")
    private fun showColorPicker() {
        val view = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val grid = view.findViewById<GridLayout>(R.id.colorGrid)

        val colors = listOf(
            Color.BLACK, Color.DKGRAY, Color.GRAY, Color.LTGRAY,
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
            Color.CYAN, Color.MAGENTA,
            "#FF9800".toColorInt(),
            "#9C27B0".toColorInt(),
            "#009688".toColorInt(),
            "#795548".toColorInt(),
            "#607D8B".toColorInt()
        )

        val dialog = createDialog(view)

        colors.forEach { color ->
            grid.addView(createColorItem(color) {
                val span = ForegroundColorSpan(color)
                currentStyles["COLOR"] = span
                applyStyle(span)
                dialog.dismiss()
            })
        }

        dialog.show()
    }
    private fun createDialog(view: View): AlertDialog {
        return AlertDialog.Builder(this)
            .setView(view)
            .create()
            .apply {
                window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            }
    }
    private fun createColorItem(color: Int, onClick: () -> Unit): View {
        return View(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = resources.displayMetrics.widthPixels / 6
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = 20f
            }
            setOnClickListener { onClick() }
        }
    }
    private fun getSelection(): Pair<Int, Int> {
        val start = binding.editText.selectionStart
        val end = binding.editText.selectionEnd
        return if (start <= end) {
            start to end
        } else {
            end to start
        }
    }
    private fun setActive(view: View, active: Boolean) {
        view.setBackgroundColor(
            if (active) "#AAAAAA".toColorInt()
            else "#DDDDDD".toColorInt()
        )
    }
    private fun getKey(span: Any): String {
        return when (span) {
            is ForegroundColorSpan -> "COLOR"
            is AbsoluteSizeSpan -> "SIZE"
            is StyleSpan -> when (span.style) {
                Typeface.BOLD -> "BOLD"
                Typeface.ITALIC -> "ITALIC"
                else -> "STYLE"
            }
            is UnderlineSpan -> "UNDERLINE"
            else -> span.javaClass.simpleName
        }
    }
    private fun copySpan(span: Any): Any {
        return when (span) {
            is StyleSpan -> StyleSpan(span.style)
            is ForegroundColorSpan -> ForegroundColorSpan(span.foregroundColor)
            is AbsoluteSizeSpan -> AbsoluteSizeSpan(span.size, span.dip)
            is UnderlineSpan -> UnderlineSpan()
            else -> span
        }
    }
}