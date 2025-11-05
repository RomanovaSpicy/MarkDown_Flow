package com.markdownbinder.services

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.markdownbinder.R
import com.markdownbinder.SettingsActivity
import com.markdownbinder.adapters.OverlayBindsAdapter
import com.markdownbinder.database.BindRepository
import com.markdownbinder.databinding.OverlayWindowBinding
import com.markdownbinder.models.Bind
import com.markdownbinder.utils.PreferenceManager
import kotlinx.coroutines.launch
import kotlin.math.abs

class OverlayService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var binding: OverlayWindowBinding
    private lateinit var repository: BindRepository
    private lateinit var adapter: OverlayBindsAdapter

    private var overlayWidth = 250
    private var overlayHeightRows = 5
    private var isExpanded = true

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        NotificationHelper.createNotificationChannel(this)
        val notification = NotificationHelper.createNotification(this)
        startForeground(NotificationHelper.getNotificationId(), notification)

        repository = BindRepository(this)
        overlayWidth = PreferenceManager.getOverlayWidth(this)
        overlayHeightRows = PreferenceManager.getOverlayHeight(this)

        setupOverlayWindow()
        loadBinds()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == "ACTION_STOP") {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val themedContext = ContextThemeWrapper(this, R.style.AppTheme)
        val themedInflater = LayoutInflater.from(themedContext)
        binding = OverlayWindowBinding.inflate(themedInflater)
        overlayView = binding.root

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = getScreenWidth() - dpToPx(overlayWidth)
        params.y = getScreenHeight() / 2 - 200

        windowManager.addView(overlayView, params)
        setupOverlayUI(params)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayUI(params: WindowManager.LayoutParams) {
        binding.expandedView.layoutParams.width = dpToPx(overlayWidth)

        adapter = OverlayBindsAdapter { bind ->
            onBindSelected(bind)
        }

        binding.overlayBindsList.layoutManager = LinearLayoutManager(this)
        binding.overlayBindsList.adapter = adapter

        val itemHeight = resources.getDimensionPixelSize(R.dimen.overlay_item_height)
        binding.overlayBindsList.layoutParams.height = itemHeight * overlayHeightRows

        binding.overlayHeader.setOnTouchListener { _, event ->
            handleDragTouch(event, params)
        }

        binding.overlayCloseButton.setOnClickListener { stopSelf() }
        binding.overlaySettingsButton.setOnClickListener { openSettings() }

        // Modified: Replaced setOnClickListener with setOnTouchListener for dragging
        binding.collapsedView.setOnTouchListener { _, event ->
            handleDragTouch(event, params, isCollapsedView = true)
        }

        setExpanded(true, params)
    }

    private fun handleDragTouch(event: MotionEvent, params: WindowManager.LayoutParams, isCollapsedView: Boolean = false): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY
                if (!isDragging && (abs(deltaX) > 10 || abs(deltaY) > 10)) {
                    isDragging = true
                }
                if (isDragging) {
                    params.x = initialX + deltaX.toInt()
                    params.y = initialY + deltaY.toInt()
                    windowManager.updateViewLayout(overlayView, params)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    snapToEdge(params)
                } else {
                    // Only expand if it was a click on the collapsed view, not a drag.
                    if (isCollapsedView) {
                        setExpanded(true, params)
                    } else {
                        setExpanded(false, params)
                    }
                }
                isDragging = false
                return true
            }
        }
        return false
    }

    private fun snapToEdge(params: WindowManager.LayoutParams) {
        val screenWidth = getScreenWidth()

        val effectiveWidth = if (isExpanded) {
            dpToPx(overlayWidth)
        } else {
            resources.getDimensionPixelSize(R.dimen.overlay_collapsed_width)
        }

        val snapToRight = (params.x + effectiveWidth / 2) > (screenWidth / 2)

        if (snapToRight) {
            params.x = screenWidth - effectiveWidth
        } else {
            params.x = 0
        }

        windowManager.updateViewLayout(overlayView, params)
        updateUiForPosition(snapToRight)
    }

    private fun setExpanded(expanded: Boolean, params: WindowManager.LayoutParams) {
        isExpanded = expanded

        if (expanded) {
            binding.expandedView.visibility = View.VISIBLE
            binding.collapsedView.visibility = View.GONE
        } else {
            binding.expandedView.visibility = View.GONE
            binding.collapsedView.visibility = View.VISIBLE
        }

        snapToEdge(params)
    }

    private fun updateUiForPosition(isOnRightEdge: Boolean) {
        if (isOnRightEdge) {
            binding.overlayHeader.layoutDirection = View.LAYOUT_DIRECTION_RTL
            (binding.collapsedView.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
            binding.collapsedView.scaleX = 1f
        } else {
            binding.overlayHeader.layoutDirection = View.LAYOUT_DIRECTION_LTR
            (binding.collapsedView.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
            binding.collapsedView.scaleX = -1f
        }
        binding.collapsedView.requestLayout()
    }

    private fun loadBinds() {
        lifecycleScope.launch {
            repository.getAllBinds().collect { binds ->
                adapter.submitList(binds)
            }
        }
    }

    private fun onBindSelected(bind: Bind) {
        val intent = Intent(MarkDownAccessibilityService.ACTION_INJECT_BIND).apply {
            putExtra(MarkDownAccessibilityService.EXTRA_BIND_TEXT, bind.content)
        }
        sendBroadcast(intent)

        lifecycleScope.launch { repository.updateBind(bind.incrementUsage()) }
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private fun getScreenWidth(): Int {
        return resources.displayMetrics.widthPixels
    }

    private fun getScreenHeight(): Int {
        return resources.displayMetrics.heightPixels
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}