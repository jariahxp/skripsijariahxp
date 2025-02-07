package com.jariahxp.ui.auth.signin

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.jariahxp.MainActivity
import com.jariahxp.R
import com.jariahxp.databinding.ActivitySignInBinding
import com.jariahxp.helper.foreground.NotifikasiServices
import com.jariahxp.helper.preference.SharedPreferencesHelper
import com.jariahxp.helper.repository.AuthRepository
import com.jariahxp.helper.viewmodel.AuthViewModel
import com.jariahxp.helper.viewmodel.AuthViewModelFactory
import com.jariahxp.ui.auth.signup.SignUpActivity
import com.jariahxp.ui.dashboard.DashboardActivity
import com.jariahxp.utils.DialogUtils
import kotlin.time.Duration

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(AuthRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()))
    }
    private lateinit var googleSignInClient: GoogleSignInClient

    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        authViewModel.loginStatus.observe(this) { isLoggedIn ->
            if (isLoggedIn) {
                // Memulai Foreground Service
                startSensorMonitoringService()
                Toast.makeText(this, "Login berhasil!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                overridePendingTransition(R.anim.slide_fade_in, R.anim.slide_fade_out)
                finish()
            } else {
                performLogout()
                Toast.makeText(this, "Login gagal! Periksa email atau password Anda.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.apply {
            loginButton.setOnClickListener {
                val email = binding.emailInput.text.toString().trim()
                val password = binding.passwordInput.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this@SignInActivity, "Email dan password harus diisi", Toast.LENGTH_SHORT).show()
                } else {
                    DialogUtils.showLoading(
                        this@SignInActivity,
                        "Memproses Login...",
                        "Mohon tunggu, kami sedang memverifikasi akun Anda.",
                        3000L
                    )
                    Handler().postDelayed({
                        authViewModel.loginWithEmailPassword(email, password, this@SignInActivity)
                    }, 3000L) // Waktu delay (3000ms = 3 detik)
                }
            }
            tvToRegister.setOnClickListener {
                startActivity(Intent(this@SignInActivity, SignUpActivity::class.java))
                overridePendingTransition(R.anim.slide_fade_in, R.anim.slide_fade_out)
            }
            googleSignInButton.setOnClickListener {
                val googleSignInClient = GoogleSignIn.getClient(this@SignInActivity, gso)  // gso is GoogleSignInOptions

                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }

            forgotPassword.setOnClickListener {
                showForgotPasswordDialog()
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                val errorMessage = "Google sign-in failed"
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

                Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
            }
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        DialogUtils.showLoading(
            this@SignInActivity,
            "Login dengan Google...",
            "Mohon tunggu, kami sedang menghubungkan akun Google Anda.",
            3000L
        )

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        Handler().postDelayed({
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = FirebaseAuth.getInstance().currentUser
                        user?.let {
                            authViewModel.loginWithGoogle(idToken, this)
                        }
                    } else {
                        Log.w("GoogleSignIn", "signInWithCredential:failure", task.exception)
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }, 2500L) // Waktu delay (3000ms = 3 detik)

    }
    private fun performLogout() {
        // Hapus sesi pengguna dari SharedPreferences
        SharedPreferencesHelper.clearSession(this)
        SharedPreferencesHelper.deleteUsername(this)
        googleSignInClient.signOut().addOnCompleteListener {
            FirebaseAuth.getInstance().signOut()
        }
    }
    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Lupa Password")
        builder.setMessage("Masukkan email Anda untuk reset password:")

        // Menggunakan layout XML yang telah dibuat
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.email_input_forgot)

        builder.setView(dialogView)

        builder.setPositiveButton("Kirim") { dialog, _ ->
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                DialogUtils.showLoading(
                    this@SignInActivity,
                    "Mengirim verivikasi ke Email...",
                    "Mohon tunggu, kami sedang mengirimkan tautan reset password ke email Anda.",
                    3000L
                )
                authViewModel.sendResetPasswordEmail(email)
            } else {
                Toast.makeText(this, "Email tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
        builder.show()
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

    private fun startSensorMonitoringService() {
        val serviceIntent = Intent(this, NotifikasiServices::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Izin notifikasi diperlukan untuk menjalankan layanan.", Toast.LENGTH_SHORT).show()
        }
    }
}