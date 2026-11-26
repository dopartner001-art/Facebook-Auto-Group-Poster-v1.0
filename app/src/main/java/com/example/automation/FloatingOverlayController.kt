package com.example.automation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class FloatingOverlayController(
    private val context: Context,
    private val onStopClicked: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var titleTextView: TextView? = null
    private var groupTextView: TextView? = null
    private var statusTextView: TextView? = null
    private var progressBar: ProgressBar? = null

    fun show(state: PostQueueState) {
        if (!Settings.canDrawOverlays(context)) return
        if (overlayView != null) {
            update(state)
            return
        }

        val view = buildOverlayCard()
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = dpToPx(64)
        }

        attachDragListener(view, params)

        try {
            windowManager.addView(view, params)
            overlayView = view
            layoutParams = params
            update(state)
        } catch (_: Exception) {
            overlayView = null
            layoutParams = null
        }
    }

    fun update(state: PostQueueState) {
        if (overlayView == null) return
        val current = state.displayGroupNumber
        val total = state.totalGroups
        titleTextView?.text = if (total > 0) {
            "Posting to $current of $total groups..."
        } else {
            "Auto Group Poster"
        }
        groupTextView?.text = if (state.currentGroupName.isNotBlank()) {
            "Group: ${state.currentGroupName}"
        } else {
            "Finishing up..."
        }
        statusTextView?.text = if (state.countdownSeconds > 0) {
            "${state.statusMessage} (${state.countdownSeconds}s)"
        } else {
            state.statusMessage
        }
        progressBar?.apply {
            max = total.coerceAtLeast(1)
            progress = if (state.step == AutomationStep.COMPLETED) total else current.coerceAtLeast(1)
        }
    }

    fun hide() {
        val view = overlayView ?: return
        try {
            windowManager.removeView(view)
        } catch (_: Exception) {
        } finally {
            overlayView = null
            layoutParams = null
            titleTextView = null
            groupTextView = null
            statusTextView = null
            progressBar = null
        }
    }

    private fun buildOverlayCard(): View {
        val cardBg = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dpToPx(18).toFloat()
            setStroke(dpToPx(1), Color.parseColor("#D0D7DE"))
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = cardBg
            elevation = dpToPx(10).toFloat()
            setPadding(dpToPx(14), dpToPx(12), dpToPx(12), dpToPx(12))
            minimumWidth = dpToPx(300)
        }

        val accentPill = View(context).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1877F2"))
                cornerRadius = dpToPx(4).toFloat()
            }
        }
        root.addView(
            accentPill,
            LinearLayout.LayoutParams(dpToPx(4), dpToPx(48)).apply {
                marginEnd = dpToPx(12)
            }
        )

        val infoColumn = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(context).apply {
            text = "Posting to 1 of 1 groups..."
            textSize = 14f
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextColor(Color.parseColor("#1C1E21"))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        titleTextView = title

        val group = TextView(context).apply {
            text = "Preparing..."
            textSize = 12f
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextColor(Color.parseColor("#1877F2"))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        groupTextView = group

        val status = TextView(context).apply {
            text = "Initializing automation..."
            textSize = 11.5f
            setTextColor(Color.parseColor("#65676B"))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        statusTextView = status

        val progress = ProgressBar(
            context,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            isIndeterminate = false
            progressDrawable.setTint(Color.parseColor("#1877F2"))
        }
        progressBar = progress

        infoColumn.addView(title)
        infoColumn.addView(group)
        infoColumn.addView(status)
        infoColumn.addView(
            progress,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(6)
            ).apply {
                topMargin = dpToPx(6)
            }
        )

        root.addView(
            infoColumn,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dpToPx(10)
            }
        )

        val stopButtonBg = GradientDrawable().apply {
            setColor(Color.parseColor("#D93025"))
            cornerRadius = dpToPx(12).toFloat()
        }

        val stopButton = Button(context).apply {
            text = "Stop"
            isAllCaps = false
            textSize = 13f
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextColor(Color.WHITE)
            background = stopButtonBg
            minHeight = dpToPx(48)
            minWidth = dpToPx(72)
            setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8))
            contentDescription = "Stop auto posting"
            setOnClickListener { onStopClicked() }
        }

        root.addView(stopButton)
        return root
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachDragListener(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(view, params)
                    } catch (_: Exception) {
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
