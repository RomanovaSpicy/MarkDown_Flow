package com.markdownbinder.services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.min

@Suppress("DEPRECATION")
class MarkDownAccessibilityService : AccessibilityService() {

    // Window-based cache for the last used input field
    private val windowCache = mutableMapOf<Int, AccessibilityNodeInfo>()

    companion object {
        const val ACTION_INJECT_BIND = "com.markdownbinder.ACTION_INJECT_BIND"
        const val EXTRA_BIND_TEXT = "com.markdownbinder.EXTRA_BIND_TEXT"
        const val ACTION_BIND_INSERTED = "com.markdownbinder.ACTION_BIND_INSERTED"
    }

    private val bindInjectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_INJECT_BIND) {
                intent.getStringExtra(EXTRA_BIND_TEXT)?.let { text ->
                    performTextInsertion(text)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(ACTION_INJECT_BIND)
        ContextCompat.registerReceiver(this, bindInjectionReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun performTextInsertion(bindContent: String) {
        val targetNode = findBestCandidateForInsertion() ?: return

        try {
            val existingText = if (targetNode.isShowingHintText) "" else targetNode.text?.toString() ?: ""
            val selectionStart = targetNode.textSelectionStart.coerceAtLeast(0)
            val selectionEnd = targetNode.textSelectionEnd.coerceAtLeast(0)

            val currentSelectionStart = min(selectionStart, selectionEnd)
            val currentSelectionEnd = max(selectionStart, selectionEnd)

            val cursorMarkerPosition = bindContent.indexOf('|')
            val textToInsert = if (cursorMarkerPosition != -1) bindContent.replace("|", "") else bindContent

            val newText = existingText.take(currentSelectionStart) + textToInsert + existingText.drop(currentSelectionEnd)

            val finalCursorPosition = if (cursorMarkerPosition != -1) {
                currentSelectionStart + cursorMarkerPosition
            } else {
                currentSelectionStart + textToInsert.length
            }

            val setTextArguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
            }
            targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setTextArguments)

            val setSelectionArguments = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, finalCursorPosition)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, finalCursorPosition)
            }
            targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, setSelectionArguments)

            val insertedIntent = Intent(ACTION_BIND_INSERTED).apply {
                putExtra(EXTRA_BIND_TEXT, bindContent)
            }
            sendBroadcast(insertedIntent)

            val windowId = targetNode.windowId
            windowCache[windowId]?.recycle() // Recycle the old node if it exists
            windowCache[windowId] = AccessibilityNodeInfo.obtain(targetNode)

        } finally {
            targetNode.recycle()
        }
    }

    /**
     * New, improved field insertion search.
     * It combines cache checking, active field search, and deep analysis.,
     * to work reliably in both native applications and WebView.
     */
    private fun findBestCandidateForInsertion(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null

        val windowId = rootNode.windowId
        val cachedNode = windowCache[windowId]
        if (cachedNode != null && cachedNode.refresh()) {
            if (isViableInput(cachedNode)) {
                rootNode.recycle()
                return AccessibilityNodeInfo.obtain(cachedNode)
            }
        } else {
            windowCache.remove(windowId)?.recycle()
        }

        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null) {
            if (isViableInput(focusedNode)) {
                rootNode.recycle()
                return focusedNode
            }
            focusedNode.recycle()
        }

        val potentialInputs = mutableListOf<AccessibilityNodeInfo>()
        findPotentialInputs(rootNode, potentialInputs)

        val bestCandidate: AccessibilityNodeInfo? = if (potentialInputs.size == 1) {
            val singleCandidate = potentialInputs.first()
            potentialInputs.remove(singleCandidate)
            singleCandidate
        } else {
            null
        }

        potentialInputs.forEach { it.recycle() }

        rootNode.recycle()
        return bestCandidate
    }

    /**
     * Checks whether the node is suitable for text input.
     * Works for native fields and fields in WebView.
     */
    private fun isViableInput(node: AccessibilityNodeInfo?): Boolean {
        if (node == null || !node.isVisibleToUser || node.isPassword) {
            return false
        }
        val canSetText = node.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT)
        val isInteractive = node.isEditable || node.isFocusable

        return canSetText && isInteractive
    }



    /**
     * Recursively finds all potentially suitable input nodes.
     */
    private fun findPotentialInputs(node: AccessibilityNodeInfo, outList: MutableList<AccessibilityNodeInfo>) {
        if (isViableInput(node)) {
            outList.add(AccessibilityNodeInfo.obtain(node))
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                findPotentialInputs(child, outList)
                child.recycle()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* No longer clearing cache here */ }

    override fun onInterrupt() { /* ... */ }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bindInjectionReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver not registered
        }
        // Clean up the cache
        windowCache.values.forEach { it.recycle() }
        windowCache.clear()
    }
}
