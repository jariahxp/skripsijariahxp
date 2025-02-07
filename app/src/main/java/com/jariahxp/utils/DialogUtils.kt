package com.jariahxp.utils


import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.jariahxp.R

object DialogUtils {
    private var loadingDialog: ProgressDialog? = null

    fun showLoading(activity: Activity, title: String, desc: String, duration: Long) {
        val builder = AlertDialog.Builder(activity)
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.item_loading, null)

        val tvTitleLoading = dialogView.findViewById<TextView>(R.id.tvTitleLoading)
        val tvDescriptionLoading = dialogView.findViewById<TextView>(R.id.tvDescriptionLoading)
        val lottieAnimationView = dialogView.findViewById<LottieAnimationView>(R.id.lottieAnimationLoading)

        lottieAnimationView.setAnimation(R.raw.loading)
        lottieAnimationView.playAnimation()

        tvTitleLoading.text = title
        tvDescriptionLoading.text = desc

        val dialog = builder.setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation2

        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
        }, duration)
    }
    fun hideLoading() {
        loadingDialog?.dismiss()
    }
}
