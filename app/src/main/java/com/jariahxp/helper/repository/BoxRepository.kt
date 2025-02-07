package com.jariahxp.helper.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jariahxp.model.IdBox

class BoxRepository {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    fun getID(username: String, callback: (IdBox?) -> Unit) {
        val userRef = database.child("id_box_user").child(username)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userData = snapshot.getValue(IdBox::class.java)
                callback(userData)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }

    fun addIdToFirebase(username: String, newId: String): Task<Void> {
        val userRef = database.child("id_box_user").child(username)

        val task = TaskCompletionSource<Void>()
        getID(username) { userData ->
            val idsList = userData?.ids?.toMutableList() ?: mutableListOf()
            idsList.add(newId)  // Add the new ID to the list

            // Create a new IdBox object with the updated IDs list
            val updatedUserData = IdBox(ids = idsList)

            // Save the updated data to Firebase
            userRef.setValue(updatedUserData).addOnCompleteListener { taskResult ->
                if (taskResult.isSuccessful) {
                    Log.d("Firebase", "ID added successfully.")
                    task.setResult(null)  // Indicate successful completion
                } else {
                    Log.e("Firebase", "Failed to add ID", taskResult.exception)
                    taskResult.exception?.let { task.setException(it) }  // Propagate failure
                }
            }
        }

        return task.task  // Return the Task object
    }


}
