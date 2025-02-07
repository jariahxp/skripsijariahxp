package com.jariahxp.helper.foreground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*
import com.jariahxp.R
import com.jariahxp.helper.preference.SharedPreferencesHelper
import com.jariahxp.ui.dashboard.fragment.HomeFragment

class NotifikasiServices : Service() {

    private lateinit var database: DatabaseReference

    override fun onCreate() {
        super.onCreate()
        database = FirebaseDatabase.getInstance().reference
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createForegroundNotification(this)
        startForeground(1, notification)

        val username = SharedPreferencesHelper.getUsername(context = this)
        if (username != null) {
            monitorUserBoxData(username)
        }

        return START_STICKY
    }

    private fun monitorUserBoxData(username: String) {
        // Menambahkan ValueEventListener untuk memantau perubahan secara real-time pada data "ids"
        database.child("id_box_user").child(username).child("ids")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Clear atau hapus semua boxId yang lama agar tidak terjadi duplikasi
                        val boxIds = mutableListOf<String>()
                        for (child in snapshot.children) {
                            val boxId = child.getValue(String::class.java)
                            boxId?.let {
                                // Menambahkan boxId ke list
                                boxIds.add(it)
                            }
                        }

                        boxIds.forEach { boxId ->
                            monitorBoxData(boxId)
                        }

                        Log.d("BoxMonitoring", "Data id_box yang terdaftar: $boxIds")
                    } else {
                        Log.d("BoxMonitoring", "User tidak memiliki box yang terdaftar.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Menangani error jika ada masalah dalam mengambil data
                    Log.e("Firebase", "Error mengambil daftar id box: ${error.message}")
                }
            })
    }

    private fun monitorBoxData(boxId: String) {
        database.child("data").child(boxId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val suhu = snapshot.child("suhu").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                        val kelembapan = snapshot.child("kelembapan").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                        val alkohol = snapshot.child("alkohol").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0

                        val ref = FirebaseDatabase.getInstance().reference.child("model_regresi")
                        ref.get().addOnSuccessListener { modelSnapshot ->
                            if (modelSnapshot.exists()) {
                                val intercept = modelSnapshot.child("b1").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                                val alkoholCoef = modelSnapshot.child("b4").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                                val kelembapanCoef = modelSnapshot.child("b3").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                                val suhuCoef = modelSnapshot.child("b2").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0

                                val prediction = intercept + (alkohol * alkoholCoef)  + (kelembapan * kelembapanCoef) + (suhu * suhuCoef)
                                val jam = prediction.toInt()
                                val menit = ((prediction - jam) * 60).toInt()

                                if (jam <= 0 && menit <= 0) {
                                    showNotification("Horeee", "Fermentasi Box $boxId telah matang", boxId)
                                } else {
                                    Log.d("BoxMonitoring", "Box $boxId aman. Prediksi waktu: ${jam} jam ${menit} menit.")
                                }
                            }
                        }.addOnFailureListener {
                            Log.e("Firebase", "Gagal mengambil model regresi: ${it.message}")
                        }
                    } else {
                        Log.d("BoxMonitoring", "Data untuk box $boxId tidak ditemukan.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error mengambil data box $boxId: ${error.message}")
                }
            })
    }
    private fun showNotification(title: String, message: String, boxId: String) {
        val notificationId = boxId.hashCode()

        // Membuat intent untuk membuka aplikasi jika pengguna mengetuk notifikasi
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: Intent(this, HomeFragment::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK  // Menambahkan flag agar aplikasi dibuka di task baru
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT or  PendingIntent.FLAG_IMMUTABLE)

        // Menambahkan notifikasi pendahuluan
        val notification = NotificationCompat.Builder(this, "foreground_service_channel")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Small Icon tetap monokrom atau sederhana
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.logo123)) // Logo besar dengan warna
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // Prioritas tinggi agar muncul dengan segera
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true) // Menambahkan fullScreenIntent untuk notifikasi pendahuluan
            .setDefaults(Notification.DEFAULT_VIBRATE) // Menambahkan getaran agar lebih menonjol
            .setSound(Uri.parse("android.resource://" + packageName + "/" + R.raw.notif)) // Menambahkan suara
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)  // Gunakan ID yang unik berdasarkan boxId
    }



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createForegroundNotification(context: Context): Notification {
        val channelId = "foreground_service_channel"
        val channelName = "Foreground Service Channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("Memantau Fermentasi Tape Anda")
            .setContentText("Layanan foreground aktif untuk memantau kondisi fermentasi secara real-time.")
            .setSmallIcon(R.drawable.logo123) // Small Icon tetap monokrom atau sederhana
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.logo123))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Layanan foreground aktif untuk memantau kondisi fermentasi secara real-time. " +
                            "Pastikan kondisi tape selalu terjaga agar proses fermentasi berjalan dengan baik dan optimal. " +
                            "Proses ini memantau suhu dan kelembapan secara real-time.")
            )
            .build()

    }
}
