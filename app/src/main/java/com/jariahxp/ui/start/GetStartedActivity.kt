package com.jariahxp.ui.start

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.jariahxp.MainActivity
import com.jariahxp.R
import com.jariahxp.databinding.ActivityGetStartedBinding
import com.jariahxp.helper.adapter.GetStartedAdapter
import com.jariahxp.model.PageData
import com.jariahxp.ui.auth.signin.SignInActivity
import com.jariahxp.ui.auth.signup.SignUpActivity

class GetStartedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGetStartedBinding
    private val auth by lazy { FirebaseAuth.getInstance() } // FirebaseAuth instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetStartedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pages = listOf(
            PageData(
                R.raw.start1,
                "Selamat Datang di TaPredict",
                "Aplikasi ini membantu memprediksi sisa waktu fermentasi tape singkong berbasis IoT dan Machine Learning."
            ),
            PageData(
                R.raw.start2,
                "Bagaimana TaPredict Bekerja?",
                "TaPredict menggunakan sensor IoT untuk memantau fermentasi dan menerapkan model Machine Learning untuk memprediksi waktu optimal."
            ),
            PageData(
                R.raw.start3,
                "Maksimalkan Fermentasi Tape Singkong",
                "Lanjutkan untuk mendapatkan prediksi akurat dan meningkatkan kualitas tape singkong Anda dengan TaPredict!"
            )
        )

        val adapter = GetStartedAdapter(pages)
        binding.viewPager.adapter = adapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                updateIndicators(position)

                // Tampilkan tombol Get Started di halaman terakhir
                if (position == pages.size - 1) {
                    binding.btnGetStarted.visibility = View.VISIBLE
                } else {
                    binding.btnGetStarted.visibility = View.GONE
                }
            }
        })
        binding.btnGetStarted.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
    private fun updateIndicators(position: Int) {
        // Set indikator yang aktif sesuai halaman
        when (position) {
            0 -> binding.indicator.setImageResource(R.drawable.first_start)
            1 -> binding.indicator.setImageResource(R.drawable.second_start)
            2 -> binding.indicator.setImageResource(R.drawable.third_start)
        }
    }
}