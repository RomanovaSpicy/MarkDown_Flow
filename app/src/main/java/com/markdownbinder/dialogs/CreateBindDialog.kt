package com.markdownbinder.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.markdownbinder.R
import com.markdownbinder.databinding.DialogCreateBindBinding
import com.markdownbinder.models.Bind

class CreateBindDialog : DialogFragment() {

    private var _binding: DialogCreateBindBinding? = null
    private val binding get() = _binding!!

    private var existingBind: Bind? = null
    private var onSave: ((name: String, content: String) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogCreateBindBinding.inflate(layoutInflater)

        existingBind?.let { bind ->
            binding.nameInput.setText(bind.name)
            binding.contentInput.setText(bind.content)
        }

        setupButtons()
        setupValidation()

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Widget_App_Dialog)
            .setView(binding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return dialog
    }

    override fun onStart() {
        super.onStart()
        (activity as? BlurryDialogListener)?.onDialogShown()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as? BlurryDialogListener)?.onDialogDismissed()
    }

    private fun setupButtons() {
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        binding.helpButton.setOnClickListener {
            HelpDialog.show(childFragmentManager)
        }

        binding.clearButton.setOnClickListener {
            binding.nameInput.text?.clear()
            binding.contentInput.text?.clear()
            binding.nameInput.requestFocus()
        }

        binding.saveButton.setOnClickListener {
            saveBind()
        }
    }

    private fun setupValidation() {
        val textWatcher = {
            val name = binding.nameInput.text?.toString() ?: ""
            val content = binding.contentInput.text?.toString() ?: ""

            binding.saveButton.isEnabled = name.isNotBlank() && content.isNotBlank()
        }

        binding.nameInput.addTextChangedListener { textWatcher() }
        binding.contentInput.addTextChangedListener { textWatcher() }

        textWatcher()
    }

    private fun saveBind() {
        val name = binding.nameInput.text?.toString()?.trim() ?: return
        val content = binding.contentInput.text?.toString() ?: return

        if (name.isBlank() || content.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Name and content cannot be empty",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        onSave?.invoke(name, content)
        dismiss()

        Toast.makeText(
            requireContext(),
            if (existingBind == null) R.string.toast_bind_created
            else R.string.toast_bind_updated,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "CreateBindDialog"

        fun show(
            fragmentManager: FragmentManager,
            bind: Bind? = null,
            onSave: (name: String, content: String) -> Unit
        ) {
            val dialog = CreateBindDialog()
            dialog.existingBind = bind
            dialog.onSave = onSave
            dialog.show(fragmentManager, TAG)
        }
    }
}