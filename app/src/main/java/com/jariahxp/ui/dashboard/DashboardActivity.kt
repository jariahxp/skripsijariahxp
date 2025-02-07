package com.jariahxp.ui.dashboard

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.jariahxp.R
import com.jariahxp.databinding.ActivityDashboardBinding
import com.jariahxp.helper.adapter.IdListAdapter
import com.jariahxp.helper.preference.SharedPreferencesHelper
import com.jariahxp.helper.viewmodel.BoxViewModel
import com.jariahxp.ui.auth.signin.SignInActivity
import com.jariahxp.ui.dashboard.fragment.AboutUsFragment
import com.jariahxp.ui.dashboard.fragment.HomeFragment
import com.jariahxp.utils.DialogUtils
import android.content.Context
import android.content.pm.PackageManager
import android.net.NetworkCapabilities
import android.os.Build
import com.jariahxp.helper.foreground.NotifikasiServices

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityDashboardBinding
    private var lastSelectedItemId: Int? = null
    private lateinit var boxViewModel: BoxViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        startSensorMonitoringService()
        val toolbar: Toolbar = findViewById(R.id.toolbar) // Ignore red line errors
        setSupportActionBar(toolbar)
        boxViewModel = ViewModelProvider(this).get(BoxViewModel::class.java)
        boxViewModel.status.observe(this, Observer { status ->
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
        })
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)
        val tvName = headerView.findViewById<TextView>(R.id.nameNav)
        val tvEmail = headerView.findViewById<TextView>(R.id.emailNav)

        val name = SharedPreferencesHelper.getUsername(this)
        val email = SharedPreferencesHelper.getEmail(this)

        tvName.text = name ?: "Nama tidak ditemukan"
        tvEmail.text = email ?: "Email tidak ditemukan"
        navigationView.setNavigationItemSelectedListener(this)

        replaceFragment(HomeFragment())
        binding.bottomNavigationView.background = null

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            rotateBottomNavItem(item, 360f)

            when (item.itemId) {
                R.id.home1 -> {
                    replaceFragment(HomeFragment())
                    navigationView.setCheckedItem(R.id.nav_home)
                    true

                }

                R.id.about1 -> {
                    replaceFragment(AboutUsFragment())
                    navigationView.setCheckedItem(R.id.nav_about)
                    true
                }

                else -> false
            }
        }
        binding.bottomNavigationView.setOnItemReselectedListener { item ->
            val view = binding.bottomNavigationView.findViewById<View>(item.itemId)
            animateItem(view)
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        drawerLayout = findViewById(R.id.drawer_layout)


        val toggle = ActionBarDrawerToggle(
            this@DashboardActivity, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment()).commit()
            navigationView.setCheckedItem(R.id.nav_home)
        }
        binding.fab.setOnClickListener {
            rotateFab(135f)
            showBottomDialog()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.nav_home -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, HomeFragment()).commit()
                binding.bottomNavigationView.selectedItemId = R.id.home1 // Sync bottom navigation
            }

            R.id.nav_about -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AboutUsFragment()).commit()
                binding.bottomNavigationView.selectedItemId = R.id.about1 // Sync bottom navigation
            }

            R.id.nav_share -> {
                val shareText = """
                    🌟 TaPredict: Aplikasi Fermentasi Tape Singkong 🌟
                    
                    Aplikasi ini memudahkan kamu dalam memantau proses fermentasi tape singkong secara real-time. 
                    Pantau kemajuan fermentasi, prediksi waktu matang, dan dapatkan pemberitahuan tepat waktu!
            
                    Dikembangkan oleh: Ahmad Ghozali (JariahXp)
            
                    Unduh sekarang dan nikmati pengalaman fermentasi yang lebih mudah! 🚀
                """.trimIndent()

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }

                startActivity(Intent.createChooser(shareIntent, "Bagikan aplikasi ini"))
            }


            R.id.nav_logout -> {
                showLogoutConfirmationDialog()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        Log.d("Drawer", "Drawer closed")
        return true
    }

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
            this@DashboardActivity,
            "Keluar Akun...",
            "Mohon tunggu, Anda sedang keluar dari akun.",
            3000L
        )
        stopSensorMonitoringService()
        Handler().postDelayed({
            SharedPreferencesHelper.clearSession(this)
            SharedPreferencesHelper.deleteUsername(this)

            googleSignInClient.signOut().addOnCompleteListener {
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this, SignInActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }, 3500L) // Waktu delay (3000ms = 3 detik)
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment !is HomeFragment) {
            super.onBackPressed()

            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment is HomeFragment) {
                binding.bottomNavigationView.selectedItemId = R.id.home1
            } else if (currentFragment is AboutUsFragment) {
                binding.bottomNavigationView.selectedItemId = R.id.about1
            }
        } else {
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

    private fun replaceFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        // Cek apakah fragment baru sudah ada di back stack
        if (currentFragment == null || currentFragment::class.java != fragment::class.java) {
            fragmentTransaction.replace(R.id.fragment_container, fragment)
            fragmentTransaction.addToBackStack(null)
            fragmentTransaction.commit()
        }
    }


    private fun showBottomDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottomsheetlayout)

        // Inisialisasi komponen layout dialog
        val addID = dialog.findViewById<LinearLayout>(R.id.layoutAddID)
        val removeID = dialog.findViewById<LinearLayout>(R.id.layoutRemoveID)
        val listID = dialog.findViewById<LinearLayout>(R.id.layoutListID)
        val cancelButton = dialog.findViewById<ImageView>(R.id.cancelButton)

        addID.setOnClickListener {
            if (!isInternetAvailable()) {
                dialog.dismiss()
                showNoInternetDialog() // Menampilkan dialog kustom jika tidak ada koneksi
                return@setOnClickListener
            }
            dialog.dismiss()

            resetFabRotation()
            showAddIdBoxDialog()
        }

        removeID.setOnClickListener {
            if (!isInternetAvailable()) {
                dialog.dismiss()
                showNoInternetDialog() // Menampilkan dialog kustom jika tidak ada koneksi
                return@setOnClickListener
            }
            dialog.dismiss()

            resetFabRotation()
            showIdListDialog(isRemove = true)

        }

        listID.setOnClickListener {
            if (!isInternetAvailable()) {
                dialog.dismiss()
                showNoInternetDialog() // Menampilkan dialog kustom jika tidak ada koneksi
                return@setOnClickListener
            }
            dialog.dismiss()

            showIdListDialog(isRemove = false)
            resetFabRotation()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
            resetFabRotation()
        }
        dialog.setOnDismissListener {
            resetFabRotation()
        }
        dialog.show()
        dialog.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes.windowAnimations = R.style.DialogAnimation
            setGravity(Gravity.BOTTOM)
        }
    }

    private fun showNoInternetDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_no_internet)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation2
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(),  // Lebar 90% dari layar
            ViewGroup.LayoutParams.WRAP_CONTENT  // Tinggi sesuai konten
        )
        // Inisialisasi komponen di layout dialog
        val cancelButton = dialog.findViewById<ImageView>(R.id.cancelButton)
        val lottieAnimationView = dialog.findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = dialog.findViewById<TextView>(R.id.tvDescription)

        // Set animasi dan teks
        lottieAnimationView.setAnimation(R.raw.no_internet) // Ganti dengan animasi Lottie yang sesuai
        tvTitle.text = "Sayangnya fitur ini hanya tersedia kalau anda terhubung internet"
        tvDescription.text = "Coba hubungkan perangkat ke koneksi internet"

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun showAddIdBoxDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Menambahkan Box-ID")
        builder.setMessage("Masukkan Box ID yang ingin ditambahkan:")

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_id_box, null)
        val idBoxEditText = dialogView.findViewById<EditText>(R.id.id_box_add)
        builder.setView(dialogView)

        builder.setPositiveButton("Tambah") { dialog, _ ->
            val username = SharedPreferencesHelper.getUsername(this@DashboardActivity)
            val idBox = idBoxEditText.text.toString().trim()

            if (idBox.isNotEmpty()) {
                if (idBox.length <= 5) {  // Check if idBox is not more than 5 characters
                    if (username != null) {
                        boxViewModel.addIdToFirebase(username, idBox)
                        dialog.dismiss()  // Dismiss dialog only if the ID is successfully added
                        DialogUtils.showLoading(
                            this@DashboardActivity,
                            "Menambahkan ID BOX",
                            "Mohon tunggu. Kami akan menambahkan ID Box Anda.",
                            2000L
                        )
                    } else {
                        Toast.makeText(this@DashboardActivity, "Username tidak ditemukan", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@DashboardActivity, "Box ID tidak boleh lebih dari 5 huruf", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@DashboardActivity, "Box ID tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Batal") { dialog, _ -> dialog.cancel() }
        builder.setCancelable(false)  // Prevent dialog from closing when tapping outside
        builder.show()
    }


    private fun rotateFab(rotation: Float) {
        val rotate = ObjectAnimator.ofFloat(binding.fab, "rotation", rotation)
        rotate.duration = 300
        rotate.start()
    }

    private fun resetFabRotation() {
        val rotateBack = ObjectAnimator.ofFloat(binding.fab, "rotation", 0f)
        rotateBack.duration = 300
        rotateBack.start()
    }
    private fun rotateBottomNavItem(item: MenuItem, rotation: Float) {
        val view = binding.bottomNavigationView.findViewById<View>(item.itemId)
        if (lastSelectedItemId == item.itemId) return

        ObjectAnimator.ofFloat(view, "rotation", 0f).apply {
            duration = 0
            start()
        }
        val rotate = ObjectAnimator.ofFloat(view, "rotation", rotation)
        rotate.duration = 500
        rotate.start()
        lastSelectedItemId = item.itemId

    }
    private fun animateItem(view: View){
        view.animate()
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }
    private fun removeId(id: String) {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Penghapusan")
            .setMessage("Apakah Anda yakin ingin menghapus ID Box ini: $id?")
            .setPositiveButton("Ya") { _, _ ->
                val username = SharedPreferencesHelper.getUsername(this)
                if (username != null) {
                    DialogUtils.showLoading(
                        this@DashboardActivity,
                        "Menghapus ID BOX",
                        "Mohon tunggu. Kami akan menghapus ID Box Anda.",
                        1500L
                    )
                    boxViewModel.removeIdFromFirebase(username, id)

                    boxViewModel.getIdFromFirebase(username)
                } else {
                    Toast.makeText(this, "Username tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun showIdListDialog(isRemove: Boolean) {
        val username = SharedPreferencesHelper.getUsername(this)
        if (username != null) {
            boxViewModel.getIdFromFirebase(username)
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_list_id, null)
        val tvNoIds = dialogView.findViewById<LottieAnimationView>(R.id.tvNoIds)
        val tvNoIdsTV = dialogView.findViewById<TextView>(R.id.tvNoIdsTEXVIEW)

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewIDList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val title = if (isRemove) "Klik ID untuk Menghapus" else "Berikut List ID Box Anda"

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setNegativeButton("Tutup", null)
            .create()

        boxViewModel.idBox.observe(this, Observer { userData ->
            if (userData != null && userData.ids != null && userData.ids.isNotEmpty()) {
                val adapter = IdListAdapter(userData.ids.toMutableList()) { id ->
                    if (isRemove) {
                        dialog.dismiss()
                        removeId(id)
                    }
                }
                tvNoIds.visibility = View.GONE
                tvNoIdsTV.visibility = View.GONE
                recyclerView.adapter = adapter
            } else {
                tvNoIds.visibility = View.VISIBLE
                tvNoIdsTV.visibility = View.VISIBLE
            }
        })
        dialog.show()
    }
    fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    private fun stopSensorMonitoringService() {
        val serviceIntent = Intent(this, NotifikasiServices::class.java)
        stopService(serviceIntent)
    }
    private fun startSensorMonitoringService() {
        val serviceIntent = Intent(this, NotifikasiServices::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}