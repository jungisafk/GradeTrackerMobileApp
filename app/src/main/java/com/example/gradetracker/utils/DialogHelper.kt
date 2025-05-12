package com.example.gradetracker.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.TextView
import com.example.gradetracker.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object DialogHelper {
    private var loadingDialog: Dialog? = null

    fun showLoading(context: Context, message: String = "Loading...") {
        if (loadingDialog?.isShowing == true) {
            return
        }

        loadingDialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_loading)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(false)
            findViewById<TextView>(R.id.tvLoadingMessage).text = message
            show()
        }
    }

    fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    fun showConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "Yes",
        negativeText: String = "No",
        onPositiveClick: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ -> onPositiveClick() }
            .setNegativeButton(negativeText, null)
            .show()
    }

    fun showErrorDialog(
        context: Context,
        message: String,
        onDismiss: () -> Unit = {}
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> onDismiss() }
            .show()
    }

    fun showSuccessDialog(
        context: Context,
        message: String,
        onDismiss: () -> Unit = {}
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Success")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> onDismiss() }
            .show()
    }
} 