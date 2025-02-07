package com.jariahxp.utils

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.jariahxp.R

object DialogUtilsFermentation {
    private var loadingDialog: AlertDialog? = null

    fun showLoading(activity: Activity, title: String, desc: String, lottieAnimationRes : Int) {
        val builder = AlertDialog.Builder(activity)
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.item_fermentation, null)

        val tvTitleLoading = dialogView.findViewById<TextView>(R.id.tvTitleLoading)
        val tvDescriptionLoading = dialogView.findViewById<TextView>(R.id.tvDescriptionLoading)
        val lottieAnimationView = dialogView.findViewById<LottieAnimationView>(R.id.lottieAnimationLoading)
        val closeButton = dialogView.findViewById<Button>(R.id.close)
        lottieAnimationView.setAnimation(lottieAnimationRes)
        lottieAnimationView.playAnimation()

        closeButton.setOnClickListener{
            hideLoading()
        }
        tvTitleLoading.text = title
        tvDescriptionLoading.text = desc

        val dialog = builder.setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation2

        loadingDialog = dialog
        dialog.show()
    }

    fun hideLoading() {
        loadingDialog?.dismiss()
    }
}
