package com.jariahxp.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jariahxp.MainActivity
import com.jariahxp.R
import com.jariahxp.databinding.ActivitySplashBinding
import com.jariahxp.helper.preference.SharedPreferencesHelper
import com.jariahxp.ui.dashboard.DashboardActivity
import com.jariahxp.ui.start.GetStartedActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Handler().postDelayed({
            val username = SharedPreferencesHelper.getUsername(this)

            if (username.isNullOrEmpty()) {
                startActivity(Intent(this, GetStartedActivity::class.java))
                finish()
            } else {
                startActivity(Intent(this, DashboardActivity::class.java))
                overridePendingTransition(R.anim.slide_fade_in, R.anim.slide_fade_out)
                finish()
            }

        }, 4000L)

    }
}