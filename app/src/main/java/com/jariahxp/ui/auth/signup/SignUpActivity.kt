package com.jariahxp.ui.auth.signup

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.jariahxp.MainActivity
import com.jariahxp.R
import com.jariahxp.databinding.ActivitySignUpBinding
import com.jariahxp.helper.preference.SharedPreferencesHelper
import com.jariahxp.helper.repository.AuthRepository
import com.jariahxp.helper.viewmodel.AuthViewModel
import com.jariahxp.helper.viewmodel.AuthViewModelFactory
import com.jariahxp.helper.viewmodel.BoxViewModel
import com.jariahxp.ui.auth.signin.SignInActivity
import com.jariahxp.utils.DialogUtils

class SignUpActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val authRepository by lazy { AuthRepository(auth, firestore) }
    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(authRepository)
    }
    private lateinit var binding: ActivitySignUpBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var boxViewModel: BoxViewModel

    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        boxViewModel = ViewModelProvider(this).get(BoxViewModel::class.java)
        boxViewModel.status.observe(this, Observer { status ->
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
        })
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        viewModel.registrationStatus.observe(this) { isSuccess ->
            if (isSuccess) {
                addFieldIdBox()
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SignInActivity::class.java))
                overridePendingTransition(R.anim.slide_fade_in, R.anim.slide_fade_out)
                finish()
            } else {
                performLogout()
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle back button click
        binding.backButton.setOnClickListener {
            onBackPressed()  // Go back to the previous screen
        }

        // Handle registration button click (Email/Password)
        binding.registerButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()
            val username = binding.usernameInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && username.isNotEmpty()) {
                DialogUtils.showLoading(
                    this@SignUpActivity,
                    "Mendaftarkan Akun...",
                    "Mohon tunggu, kami sedang membuat akun Anda.",
                    3000L
                )

                viewModel.registerWithEmailPassword(email, password, username)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvToLogin.setOnClickListener {
            startActivity(Intent(this@SignUpActivity, SignInActivity::class.java))
            overridePendingTransition(R.anim.slide_fade_in, R.anim.slide_fade_out)
            finish()
        }

        binding.googleSignUpButton.setOnClickListener {
            val googleSignInClient = GoogleSignIn.getClient(this, gso)  // gso is GoogleSignInOptions

            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    private fun addFieldIdBox() {
        val username = binding.usernameInput.text.toString()
        val idBox = "idbox"
        boxViewModel.addIdToFirebase(username, idBox)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                val errorMessage = "Google sign-in failed: ${e.statusCode}"
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()

                Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
            }
        }
    }


    private fun firebaseAuthWithGoogle(idToken: String) {
        DialogUtils.showLoading(
            this@SignUpActivity,
            "Mendaftarkan Akun Google...",
            "Mohon tunggu, kami sedang menautkan akun Google Anda.",
            3000L
        )
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        Handler().postDelayed({
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {

                        val user = auth.currentUser
                        val username = binding.usernameInput.text.toString()

                        if (user != null && username.isNotEmpty()) {
                            viewModel.registerWithGoogle(user.email!!, username)
                            performLogout()

                        } else {
                            performLogout()
                            showUsernameWarningDialog()
                        }
                    } else {
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                }
        }, 2000L)

    }
    private fun performLogout() {
        // Hapus sesi pengguna dari SharedPreferences
        SharedPreferencesHelper.clearSession(this)
        SharedPreferencesHelper.deleteUsername(this)
        googleSignInClient.signOut().addOnCompleteListener {
            FirebaseAuth.getInstance().signOut()
        }
    }
    private fun showUsernameWarningDialog() {
        // Membuat dialog peringatan
        AlertDialog.Builder(this)
            .setTitle("Peringatan")
            .setMessage("Silakan isi kolom username untuk mendaftar dengan akun Google.")
            .setPositiveButton("OK") { _, _ ->
                // Fokus pada kolom username dan buka keyboard
                binding.usernameInput.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.usernameInput, InputMethodManager.SHOW_IMPLICIT)
            }
            .setCancelable(false) // Agar dialog tidak bisa ditutup selain dengan OK
            .show()
    }


}