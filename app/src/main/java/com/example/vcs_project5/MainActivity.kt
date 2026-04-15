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
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val activeStyles = mutableMapOf<String, Any>()
    private var isEditing = false
    private var lastCursorStart = -1
    private var lastCursorEnd = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        initTextWatchers()
    }
    private fun initToolbar() {
        binding.btnBold.setOnClickListener {
            applyStyle("BOLD", StyleSpan(Typeface.BOLD), isToggle = true)
        }
        binding.btnItalic.setOnClickListener {
            applyStyle("ITALIC", StyleSpan(Typeface.ITALIC), isToggle = true)
        }
        binding.btnUnderline.setOnClickListener {
            applyStyle("UNDERLINE", UnderlineSpan(), isToggle = true)
        }
        binding.btnColor.setOnClickListener { showColorPicker() }
        binding.btnSize.setOnClickListener { showSizePicker() }
    }
    private fun applyStyle(key: String, spanTemplate: Any, isToggle: Boolean) {
        val start = binding.editText.selectionStart
        val end = binding.editText.selectionEnd
        val text = binding.editText.text

        if (start != end) {
            val min = min(start, end)
            val max = max(start, end)

            val existingSpans = text
                .getSpans(min, max, Any::class.java)
                .filter { getStyleKey(it) == key }

            val isFullyCovered = existingSpans.any {
                text.getSpanStart(it) <= min &&
                        text.getSpanEnd(it) >= max
            }
            splitAndRemoveOldSpans(min, max, key)

            if (isToggle && isFullyCovered) {
                activeStyles.remove(key)
            } else {
                text.setSpan(cloneSpan(spanTemplate), min, max, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                activeStyles[key] = cloneSpan(spanTemplate)
            }
        } else {
            if (isToggle && activeStyles.containsKey(key)) {
                activeStyles.remove(key)
            } else {
                activeStyles[key] = cloneSpan(spanTemplate)
            }
        }
        updateToolbarUI()
    }
    private fun initTextWatchers() {
        val editable = binding.editText.text
        editable.setSpan(object : SpanWatcher {
            override fun onSpanAdded(text: Spannable?, what: Any?, start: Int, end: Int) {
                if (what === Selection.SELECTION_START || what === Selection.SELECTION_END) onSelectionChanged()
            }
            override fun onSpanRemoved(text: Spannable?, what: Any?, start: Int, end: Int) {}
            override fun onSpanChanged(text: Spannable?, what: Any?, ostart: Int, oend: Int, nstart: Int, nend: Int) {
                if (what === Selection.SELECTION_START || what === Selection.SELECTION_END) onSelectionChanged()
            }
        }, 0, editable.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        binding.editText.addTextChangedListener(object : TextWatcher {
            private var insertStart = 0
            private var insertCount = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                insertStart = start
                insertCount = count
            }

            override fun afterTextChanged(s: Editable?) {
                if (isEditing || s == null || insertCount <= 0) return
                isEditing = true
                applyActiveStylesToNewText(s, insertStart, insertStart + insertCount)
                isEditing = false
            }
        })
    }
    private fun applyActiveStylesToNewText(s: Editable, start: Int, end: Int) {
        val overlappingSpans = s
            .getSpans(start, end, Any::class.java)
            .filter { isFormatSpan(it) }

        for (span in overlappingSpans) {
            val spanStart = s.getSpanStart(span)
            val spanEnd = s.getSpanEnd(span)
            val key = getStyleKey(span)

            val isDesiredStyle = activeStyles.containsKey(key) && isSameStyle(span, activeStyles[key]!!)
            if (isDesiredStyle) continue

            s.removeSpan(span)
            if (spanStart < start) s.setSpan(cloneSpan(span), spanStart, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (spanEnd > end) s.setSpan(cloneSpan(span), end, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        for ((key, spanTemplate) in activeStyles) {
            val existingSpans = s
                .getSpans(start, end, Any::class.java)
                .filter {
                    getStyleKey(it) == key &&
                            isSameStyle(it, spanTemplate)
                }

            val isFullyCovered = existingSpans.any {
                s.getSpanStart(it) <= start &&
                        s.getSpanEnd(it) >= end
            }

            if (!isFullyCovered) {
                s.setSpan(cloneSpan(spanTemplate), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
    private fun onSelectionChanged() {
        if (isEditing) return
        val start = binding.editText.selectionStart
        val end = binding.editText.selectionEnd

        if (start != lastCursorStart || end != lastCursorEnd) {
            lastCursorStart = start
            lastCursorEnd = end
            syncActiveStylesWithCursor(start, end)
        }
    }
    private fun syncActiveStylesWithCursor(start: Int, end: Int) {
        activeStyles.clear()
        val text = binding.editText.text

        val checkPos = if (start == end) start - 1 else min(start, end)
        val limitPos = if (start == end) start else min(start, end) + 1

        if (checkPos >= 0 && limitPos <= text.length) {
            val spans = text
                .getSpans(checkPos, limitPos, Any::class.java)
                .filter { isFormatSpan(it) }
            for (span in spans) {
                if (text.getSpanStart(span) <= checkPos && text.getSpanEnd(span) >= limitPos) {
                    activeStyles[getStyleKey(span)] = cloneSpan(span)
                }
            }
        }
        updateToolbarUI()
    }
    private fun updateToolbarUI() {
        setActiveUI(binding.btnBold, activeStyles.containsKey("BOLD"))
        setActiveUI(binding.btnItalic, activeStyles.containsKey("ITALIC"))
        setActiveUI(binding.btnUnderline, activeStyles.containsKey("UNDERLINE"))
    }
    private fun splitAndRemoveOldSpans(start: Int, end: Int, key: String) {
        val text = binding.editText.text
        val spans = text
            .getSpans(start, end, Any::class.java)
            .filter { getStyleKey(it) == key }

        for (span in spans) {
            val spanStart = text.getSpanStart(span)
            val spanEnd = text.getSpanEnd(span)
            text.removeSpan(span)
            if (spanStart < start) {
                text.setSpan(cloneSpan(span), spanStart, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            if (spanEnd > end) {
                text.setSpan(cloneSpan(span), end, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
    @SuppressLint("InflateParams")
    private fun showSizePicker() {
        val view = layoutInflater.inflate(R.layout.dialog_size_picker, null)
        val list = view.findViewById<ListView>(R.id.listSize)

        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, SIZES.map { "Size $it" })
        val dialog = createDialog(view)

        list.setOnItemClickListener { _, _, pos, _ ->
            applyStyle("SIZE", AbsoluteSizeSpan(SIZES[pos], true), isToggle = false)
            dialog.dismiss()
        }
        dialog.show()
    }
    @SuppressLint("InflateParams")
    private fun showColorPicker() {
        val view = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val grid = view.findViewById<GridLayout>(R.id.colorGrid)
        val dialog = createDialog(view)

        COLORS.forEach { color ->
            grid.addView(createColorItemView(color) {
                applyStyle("COLOR", ForegroundColorSpan(color), isToggle = false)
                dialog.dismiss()
            })
        }
        dialog.show()
    }
    private fun createDialog(view: View): AlertDialog {
        return AlertDialog.Builder(this).setView(view).create().apply {
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
    }
    private fun createColorItemView(color: Int, onClick: () -> Unit): View {
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
    private fun setActiveUI(view: View, isActive: Boolean) {
        view.setBackgroundColor(if (isActive) "#AAAAAA".toColorInt() else "#DDDDDD".toColorInt())
    }
    private fun isFormatSpan(span: Any): Boolean {
        return span is StyleSpan || span is UnderlineSpan || span is ForegroundColorSpan || span is AbsoluteSizeSpan
    }
    private fun isSameStyle(span1: Any, span2: Any): Boolean {
        if (span1.javaClass != span2.javaClass) return false
        return when (span1) {
            is StyleSpan -> span1.style == (span2 as StyleSpan).style
            is ForegroundColorSpan -> span1.foregroundColor == (span2 as ForegroundColorSpan).foregroundColor
            is AbsoluteSizeSpan -> span1.size == (span2 as AbsoluteSizeSpan).size
            else -> true
        }
    }
    private fun getStyleKey(span: Any): String {
        return when (span) {
            is ForegroundColorSpan -> "COLOR"
            is AbsoluteSizeSpan -> "SIZE"
            is StyleSpan -> if (span.style == Typeface.BOLD) "BOLD" else "ITALIC"
            is UnderlineSpan -> "UNDERLINE"
            else -> span.javaClass.simpleName
        }
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
    companion object {
        private val SIZES = listOf(10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 40)
        private val COLORS = listOf(
            Color.BLACK, Color.DKGRAY, Color.GRAY, Color.LTGRAY,
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA,
            "#FF9800".toColorInt(), "#9C27B0".toColorInt(), "#009688".toColorInt(),
            "#795548".toColorInt(), "#607D8B".toColorInt()
        )
    }
}