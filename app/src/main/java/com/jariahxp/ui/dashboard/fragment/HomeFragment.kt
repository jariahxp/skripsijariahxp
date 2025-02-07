package com.jariahxp.ui.dashboard.fragment

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jariahxp.R
import com.jariahxp.helper.adapter.DataBoxAdapter
import com.jariahxp.helper.adapter.SwipeToDeleteCallback
import com.jariahxp.helper.preference.SharedPreferencesHelper
import com.jariahxp.helper.viewmodel.BoxViewModel
import com.jariahxp.utils.DialogUtils
import java.util.Timer
import kotlin.concurrent.schedule

class HomeFragment : Fragment() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tvInternetStatus: TextView
    private lateinit var lottieNoInternet: LottieAnimationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var boxIdAdapter: DataBoxAdapter
    private val idsList = mutableListOf<String>()
    private lateinit var boxViewModel: BoxViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        swipeRefreshLayout = view.findViewById(R.id.swipelayout)
        tvInternetStatus = view.findViewById(R.id.tvInternetStatus)
        lottieNoInternet = view.findViewById(R.id.lottieNoInternet)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 300
            removeDuration = 300
        }
        boxIdAdapter = DataBoxAdapter(idsList)
        recyclerView.adapter = boxIdAdapter

        boxViewModel = ViewModelProvider(this).get(BoxViewModel::class.java)
        boxViewModel.status.observe(requireActivity(), Observer { status ->
        })

        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(boxIdAdapter))
        itemTouchHelper.attachToRecyclerView(recyclerView)

        swipeRefreshLayout.setOnRefreshListener {
            SwipeRefresh()
        }

        checkInternetConnection()

        val username = SharedPreferencesHelper.getUsername(requireContext())
        if (username != null) {
            // Ambil data ids dari Firebase Realtime Database berdasarkan username
            getIdsFromFirebase(username)
        } else {
            Toast.makeText(activity, "Username not found!", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun checkInternetConnection() {
        val isConnected = isInternetAvailable()

        if (isConnected) {
            lottieNoInternet.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.GONE
            lottieNoInternet.visibility = View.VISIBLE
        }
    }


    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    private fun SwipeRefresh() {
        swipeRefreshLayout.isRefreshing = true
        Timer().schedule(2000) {
            // Use the Handler to update the UI on the main thread
            requireActivity().runOnUiThread {
                checkInternetConnection()
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun getIdsFromFirebase(username: String) {

        DialogUtils.showLoading(requireActivity(), "Sabar yaaa", "Kami sedang mengambil data fermentasi tape singkong Anda", 1000)  // 5000ms (5 detik)

        val database = FirebaseDatabase.getInstance()
        val ref = database.reference.child("id_box_user").child(username).child("ids")

        // Gunakan ChildEventListener untuk mendengarkan perubahan secara real-time
        ref.addChildEventListener(object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.getValue(String::class.java)
                if (id != null) {
                    idsList.add(id) // Menambahkan ID baru ke dalam list
                    boxIdAdapter.notifyItemInserted(idsList.size - 1) // Memberi tahu adapter bahwa item baru ditambahkan

                    idsList.sort()
                    boxIdAdapter.notifyDataSetChanged() // Memberi tahu adapter bahwa data telah diubah
                    DialogUtils.hideLoading()
                    DialogUtils.hideLoading()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.getValue(String::class.java)
                if (id != null) {
                    val index = idsList.indexOf(id)
                    if (index != -1) {
                        idsList[index] = id
                        boxIdAdapter.notifyItemChanged(index)
                        idsList.sort()
                        boxIdAdapter.notifyDataSetChanged() // Memberi tahu adapter bahwa data telah diubah
                        DialogUtils.hideLoading()
                        DialogUtils.hideLoading()
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val id = snapshot.getValue(String::class.java)
                if (id != null) {
                    val index = idsList.indexOf(id)
                    if (index != -1) {
                        idsList.removeAt(index)
                        idsList.sort()
                        boxIdAdapter.notifyDataSetChanged() // Memberi tahu adapter bahwa data telah diubah
                        DialogUtils.hideLoading()
                        boxIdAdapter.notifyItemRemoved(index)
                        DialogUtils.hideLoading()
                    }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Jika data dipindahkan, bisa ditambahkan logika di sini (jika perlu)
            }

            override fun onCancelled(error: DatabaseError) {
                DialogUtils.hideLoading()
                Toast.makeText(activity, "Failed to load data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
