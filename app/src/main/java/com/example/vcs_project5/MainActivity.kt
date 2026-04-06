package com.example.vcs_project5

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
import com.example.vcs_project5.databinding.ActivityMainBinding
import androidx.core.graphics.toColorInt

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val activeSpans = mutableMapOf<String, Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()
        setupTypingListener()
    }
    private fun setupListeners() {
        binding.btnBold.setOnClickListener {
            toggleStyle("BOLD", StyleSpan(Typeface.BOLD))
        }
        binding.btnItalic.setOnClickListener {
            toggleStyle("ITALIC", StyleSpan(Typeface.ITALIC))
        }
        binding.btnUnderline.setOnClickListener {
            toggleStyle("UNDERLINE", UnderlineSpan())
        }
        binding.btnColor.setOnClickListener {
            showColorPicker()
        }
        binding.btnSize.setOnClickListener {
            showSizePicker()
        }
    }
    private fun toggleStyle(key: String, span: Any) {
        val (start, end) = getSelection()
        val text = binding.editText.text

        if (start == end) {
            if (activeSpans.containsKey(key)) {
                activeSpans.remove(key)
            } else {
                activeSpans[key] = span
            }
            return
        }

        // Xử lý khi có bôi đen text
        val clazz = span.javaClass
        val existingSpans = text.getSpans(start, end, clazz)

        // Tìm xem đã có span cùng loại (ví dụ cùng là BOLD) chưa
        val specificSpan = if (span is StyleSpan) {
            existingSpans.filterIsInstance<StyleSpan>().find { it.style == span.style }
        } else {
            existingSpans.firstOrNull()
        }

        if (specificSpan != null) {
            text.removeSpan(specificSpan)
        } else {
            text.setSpan(cloneSpan(span), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
    private fun applySpan(span: Any, start: Int, end: Int) {
        binding.editText.text.setSpan(
            span,
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    private fun handleStyle(span: Any) {
        val (start, end) = getSelection()
        val key = when(span) {
            is ForegroundColorSpan -> "COLOR"
            is AbsoluteSizeSpan -> "SIZE"
            else -> span.javaClass.simpleName
        }
        if (start == end) {
            activeSpans[key] = span
        } else {
            binding.editText.text.setSpan(cloneSpan(span), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
    private fun getSelection(): Pair<Int, Int> =
        Pair(binding.editText.selectionStart, binding.editText.selectionEnd)
    private fun setupTypingListener() {
        binding.editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val cursor = binding.editText.selectionStart
                if (cursor <= 0 || s == null) return

                activeSpans.values.forEach { span ->
                    val clazz = span.javaClass
                    val existing = s.getSpans(cursor - 1, cursor, clazz)

                    val isAlreadyApplied = if (span is StyleSpan) {
                        existing.filterIsInstance<StyleSpan>().any { it.style == span.style }
                    } else {
                        existing.isNotEmpty()
                    }

                    if (!isAlreadyApplied) {
                        s.setSpan(
                            cloneSpan(span),
                            cursor - 1,
                            cursor,
                            Spanned.SPAN_EXCLUSIVE_INCLUSIVE
                        )
                    }
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
    private fun cloneSpan(span: Any): Any {
        return when (span) {
            is StyleSpan -> StyleSpan(span.style)
            is ForegroundColorSpan -> ForegroundColorSpan(span.foregroundColor)
            is AbsoluteSizeSpan -> AbsoluteSizeSpan(span.size, span.dip)
            is UnderlineSpan -> UnderlineSpan()
            else -> span
        }
    }
    private fun showSizePicker() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_size_picker, null)
        val listView = dialogView.findViewById<ListView>(R.id.listSize)

        val sizes = listOf(10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 40)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            sizes.map { "Size $it" }
        )

        listView.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            val size = sizes[position]
            val span = AbsoluteSizeSpan(size, true)
            activeSpans["SIZE"] = span
            handleStyle(span)

            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    }
    private fun showColorPicker() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val grid = dialogView.findViewById<GridLayout>(R.id.colorGrid)

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
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        colors.forEach { color ->
            val view = View(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = resources.displayMetrics.widthPixels / 6
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
                val drawable = GradientDrawable().apply {
                    setColor(color)
                    cornerRadius = 20f
                }
                background = drawable

                setOnClickListener {
                    val span = ForegroundColorSpan(color)
                    activeSpans["COLOR"] = span
                    handleStyle(span)
                    dialog.dismiss()
                }
            }
            grid.addView(view)
        }
        dialog.show()
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
    }
}