package com.jariahxp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.jariahxp.databinding.ActivityMainBinding
import com.jariahxp.helper.preference.SharedPreferencesHelper
import com.jariahxp.ui.auth.signin.SignInActivity
import com.jariahxp.ui.dashboard.DashboardActivity
import com.jariahxp.utils.DialogUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)  // Inisialisasi disini

        // Mendapatkan username dari SharedPreferences
        val usernameq = SharedPreferencesHelper.getUsername(this)
        binding.apply {
            username.text = "Welcome, $usernameq" // Menampilkan username
        }

        // Handle tombol logout
        binding.logout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
        binding.btndashboard.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Fungsi untuk menampilkan dialog konfirmasi logout
    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("Konfirmasi Logout")
            setMessage("Apakah Anda yakin ingin logout?")
            setPositiveButton("Ya") { _, _ ->

                performLogout()
            }
            setNegativeButton("Batal", null)
        }
        builder.create().show()
    }

    // Fungsi untuk logout
    private fun performLogout() {
        DialogUtils.showLoading(
            this@MainActivity,
            "Keluar Akun...",
            "Mohon tunggu, Anda sedang keluar dari akun.",
            3000L
        )
        Handler().postDelayed({
            // Hapus sesi pengguna dari SharedPreferences
            SharedPreferencesHelper.clearSession(this)
            SharedPreferencesHelper.deleteUsername(this)

            // Logout dari Google
            googleSignInClient.signOut().addOnCompleteListener {
                // Logout dari Firebase
                FirebaseAuth.getInstance().signOut()

                // Arahkan ke SignInActivity setelah logout
                val intent = Intent(this, SignInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }, 3500L) // Waktu delay (3000ms = 3 detik)
    }
    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("Konfirmasi Keluar")
            setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
            setPositiveButton("Ya") { _, _ ->
                finish()
            }
            setNegativeButton("Batal", null)
        }
        builder.create().show()
    }

}
