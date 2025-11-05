package com.markdownbinder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.markdownbinder.adapters.BindsAdapter
import com.markdownbinder.database.BindRepository
import com.markdownbinder.databinding.ActivityMainBinding
import com.markdownbinder.dialogs.BlurryDialogListener
import com.markdownbinder.dialogs.CreateBindDialog
import com.markdownbinder.models.Bind
import com.markdownbinder.services.MarkDownAccessibilityService
import com.markdownbinder.services.OverlayService
import com.markdownbinder.utils.StatisticsManager
import kotlinx.coroutines.launch
import java.util.Collections

class MainActivity : BaseActivity(), BlurryDialogListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: BindRepository
    private lateinit var adapter: BindsAdapter
    private lateinit var statsManager: StatisticsManager
    private lateinit var itemTouchHelper: ItemTouchHelper

    private var allBinds = listOf<Bind>()
    private var filteredBinds = listOf<Bind>()

    private val bindInsertedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MarkDownAccessibilityService.ACTION_BIND_INSERTED) {
                val insertedText = intent.getStringExtra(MarkDownAccessibilityService.EXTRA_BIND_TEXT)
                if (insertedText != null) {
                    statsManager.incrementBindsInserted()
                    statsManager.addKeystrokesSaved(insertedText.length)
                    updateStatistics()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = BindRepository(this)
        statsManager = StatisticsManager(this)

        setupRecyclerView()
        setupSearch()
        setupButtons()
        loadBinds()
        updateStatistics()

        val filter = IntentFilter(MarkDownAccessibilityService.ACTION_BIND_INSERTED)
        ContextCompat.registerReceiver(
            this,
            bindInsertedReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun setupRecyclerView() {
        adapter = BindsAdapter(
            onBindClick = { bind -> onBindInserted(bind) },
            onBindEdit = { bind -> showEditBindDialog(bind) },
            onBindDelete = { bind -> confirmDeleteBind(bind) }
        )

        binding.bindsRecycler.layoutManager = LinearLayoutManager(this)
        binding.bindsRecycler.adapter = adapter

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                val mutableList = adapter.currentList.toMutableList()
                Collections.swap(mutableList, fromPosition, toPosition)

                adapter.submitList(mutableList)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return

                val bind = adapter.currentList[position]

                adapter.notifyItemChanged(position)

                when (direction) {
                    ItemTouchHelper.LEFT -> confirmDeleteBind(bind)
                    ItemTouchHelper.RIGHT -> showEditBindDialog(bind)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                if (actionState != ItemTouchHelper.ACTION_STATE_DRAG) {
                    return
                }

                val scrollZoneHeight = recyclerView.height * 0.15f
                val topScrollBound = scrollZoneHeight
                val bottomScrollBound = recyclerView.height - scrollZoneHeight
                val viewBottom = viewHolder.itemView.bottom
                val viewTop = viewHolder.itemView.top
                val scrollSpeed = 15

                if (viewBottom > bottomScrollBound) {
                    recyclerView.scrollBy(0, scrollSpeed)
                } else if (viewTop < topScrollBound) {
                    recyclerView.scrollBy(0, -scrollSpeed)
                }
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.7f
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f

                lifecycleScope.launch {
                    val finalList = adapter.currentList.mapIndexed { index, bind ->
                        bind.copy(order = index)
                    }
                    repository.updateBindsOrder(finalList)
                }
            }

            override fun isLongPressDragEnabled() = false
            override fun isItemViewSwipeEnabled() = true
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.bindsRecycler)
    }

    fun getItemTouchHelper(): ItemTouchHelper {
        return itemTouchHelper
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.lowercase() ?: ""

                filteredBinds = if (query.isEmpty()) {
                    allBinds
                } else {
                    allBinds.filter { bind ->
                        bind.name.lowercase().contains(query) ||
                                bind.content.lowercase().contains(query)
                    }
                }

                adapter.submitList(filteredBinds)
                updateBindsCount()
            }
        })
    }

    private fun setupButtons() {
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.fabCreateBind.setOnClickListener {
            showEditBindDialog(null)
        }

        binding.fabOverlay.setOnClickListener {
            if (checkOverlayPermission()) {
                startOverlayService()
            } else {
                requestOverlayPermission()
            }
        }

        binding.fabOverlay.setOnLongClickListener {
            stopOverlayService()
            true
        }
    }

    private fun loadBinds() {
        lifecycleScope.launch {
            repository.getAllBinds().collect { binds ->
                allBinds = binds
                val query = binding.searchInput.text?.toString()?.lowercase() ?: ""
                filteredBinds = if (query.isEmpty()) {
                    allBinds
                } else {
                    allBinds.filter { bind ->
                        bind.name.lowercase().contains(query) ||
                                bind.content.lowercase().contains(query)
                    }
                }
                adapter.submitList(filteredBinds)
                updateBindsCount()
            }
        }
    }

    private fun updateStatistics() {
        binding.statsBindsInserted.text = statsManager.getTotalBindsInserted().toString()
        binding.statsKeystrokesSaved.text = statsManager.getTotalKeystrokesSaved().toString()
        binding.statsDaysActive.text = statsManager.getDaysActive().toString()
    }

    private fun updateBindsCount() {
        val count = filteredBinds.size
        binding.bindsCount.text = getString(R.string.main_binds_count, count)
    }

    private fun onBindInserted(bind: Bind) {
        lifecycleScope.launch {
            repository.updateBind(bind.incrementUsage())
        }
    }

    private fun showEditBindDialog(bind: Bind? = null) {
        CreateBindDialog.show(
            fragmentManager = supportFragmentManager,
            bind = bind,
            onSave = { name, content ->
                lifecycleScope.launch {
                    if (bind == null) {
                        val maxOrder = allBinds.maxOfOrNull { it.order } ?: -1
                        repository.insertBind(
                            Bind(name = name, content = content, order = maxOrder + 1)
                        )
                    } else {
                        repository.updateBind(
                            bind.copy(name = name, content = content, updatedAt = System.currentTimeMillis())
                        )
                    }
                }
            }
        )
    }

    private fun confirmDeleteBind(bind: Bind) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete)
            .setMessage("Delete \"${bind.name}\"?")
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    repository.deleteBind(bind)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        startService(intent)
        updateOverlayFabState()
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
        updateOverlayFabState()
    }

    private fun updateOverlayFabState() {
        if (OverlayService.isRunning) {
            binding.fabOverlay.backgroundTintList = getColorStateList(R.color.primary)
            binding.fabOverlay.imageTintList = getColorStateList(R.color.bg_deepest)
        } else {
            binding.fabOverlay.backgroundTintList = getColorStateList(R.color.glass_medium)
            binding.fabOverlay.imageTintList = getColorStateList(R.color.primary)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatistics()
        updateOverlayFabState()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bindInsertedReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onDialogShown() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurEffect = RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.MIRROR)
            binding.root.setRenderEffect(blurEffect)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onDialogDismissed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.root.setRenderEffect(null)
        }
    }
}