package com.jariahxp.helper.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jariahxp.helper.repository.BoxRepository
import com.jariahxp.model.IdBox

class BoxViewModel : ViewModel() {

    private val boxRepository = BoxRepository()

    private val _status = MutableLiveData<String>()
    val status: LiveData<String> get() = _status
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    private val _idBox = MutableLiveData<IdBox?>()
    val idBox: MutableLiveData<IdBox?> get() = _idBox

    fun addIdToFirebase(username: String, idBox: String) {
        boxRepository.addIdToFirebase(username, idBox)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _status.value = "ID Box berhasil ditambahkan"
                } else {
                    _status.value = "Gagal menambahkan ID Box"
                }
            }
    }
    // Fungsi untuk mendapatkan ID dari Firebase
    fun getIdFromFirebase(username: String) {
        boxRepository.getID(username) { userData ->
            if (userData != null) {
                _idBox.value = userData
            } else {
                _idBox.value = null
            }
        }
    }
    fun removeIdFromFirebase(username: String, idToRemove: String) {
        val userRef = database.child("id_box_user").child(username)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userData = snapshot.getValue(IdBox::class.java)
                val idsList = userData?.ids?.toMutableList() ?: mutableListOf()

                if (idsList.contains(idToRemove)) {
                    idsList.remove(idToRemove)  // Hapus ID dari daftar

                    val updatedUserData = IdBox(ids = idsList)

                    userRef.setValue(updatedUserData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            getIdFromFirebase(username)
                            Log.d("TaPredict", "ID Box berhasil dihapus.")
                        } else {
                            Log.e("TaPredict", "ID Box gagal dihapus.", task.exception)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TaPredict", "Failed to fetch data", error.toException())
            }
        })
    }

}