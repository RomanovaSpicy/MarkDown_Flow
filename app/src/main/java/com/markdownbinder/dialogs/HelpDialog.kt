package com.markdownbinder.dialogs

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.markdownbinder.R
import com.markdownbinder.databinding.DialogHelpBinding

class HelpDialog : DialogFragment() {

    private var _binding: DialogHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogHelpBinding.inflate(layoutInflater)
        
        setupButtons()
        
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Widget_App_Dialog)
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        return dialog
    }

    private fun setupButtons() {
        // Copy Symbol button
        binding.copySymbolButton.setOnClickListener {
            copyPipeSymbol()
        }

        // Close button
        binding.closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun copyPipeSymbol() {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Cursor Symbol", "|")
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(
            requireContext(),
            R.string.toast_copied,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HelpDialog"
        
        fun show(fragmentManager: FragmentManager) {
            HelpDialog().show(fragmentManager, TAG)
        }
    }
}
