package com.jariahxp.helper.adapter

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jariahxp.R
import com.jariahxp.helper.preference.SharedPreferencesHelper
import com.jariahxp.helper.viewmodel.BoxViewModel
import com.jariahxp.utils.DialogUtilsFermentation
import kotlin.random.Random

class DataBoxAdapter(
    public val ids: List<String>
) : RecyclerView.Adapter<DataBoxAdapter.BoxIdViewHolder>() {

    private lateinit var boxViewModel: BoxViewModel

    inner class BoxIdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val idTextView: TextView = itemView.findViewById(R.id.tvIdBox)
        val alkoholTextView: TextView = itemView.findViewById(R.id.tvKadarAlkoholValue)
        val kelembapanTextView: TextView = itemView.findViewById(R.id.tvKelembapanValue)
        val suhuTextView: TextView = itemView.findViewById(R.id.tvSuhuValue)
        val prediksiTextView: TextView = itemView.findViewById(R.id.tvHasilPrediksi)
        val tvsuhu: TextView = itemView.findViewById(R.id.tvSuhu)
        val tvkelembapan: TextView = itemView.findViewById(R.id.tvKelembapan)
        val tvalkohol: TextView = itemView.findViewById(R.id.tvKadarAlkohol)
        val eqlsuhu: TextView = itemView.findViewById(R.id.tvSuhuEqual)
        val eqlkelembapan: TextView = itemView.findViewById(R.id.tvKelembapanEqual)
        val eqlalkohol: TextView = itemView.findViewById(R.id.tvKadarAlkoholEqual)

        fun bind(id: String) {
            idTextView.text = "ID Box: $id"
            getDataFromFirebase(id)
            itemView.setOnClickListener {
                calculatePredictionForItem()
            }
        }

        private fun getDataFromFirebase(id: String, showDialog: Boolean =false) {

            val database = FirebaseDatabase.getInstance()
            val ref = database.reference.child("data").child(id)

            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val alkohol = snapshot.child("alkohol").getValue(String::class.java)
                    val kelembapan = snapshot.child("kelembapan").getValue(String::class.java)
                    val suhu = snapshot.child("suhu").getValue(String::class.java)

                    if (alkohol == null  || kelembapan == null || suhu == null) {
                        alkoholTextView.visibility = View.GONE
                        kelembapanTextView.visibility = View.GONE
                        suhuTextView.visibility = View.GONE
                        tvsuhu.visibility = View.GONE
                        tvkelembapan.visibility = View.GONE
                        tvalkohol.visibility = View.GONE
                        eqlsuhu.visibility = View.GONE
                        eqlkelembapan.visibility = View.GONE
                        eqlalkohol.visibility = View.GONE

                        val kalimatList = listOf(
                            "Ups! Kami gak bisa nemuin ID Box ini di database. 😅 Coba cek lagi, mungkin ada yang salah ketik atau ID-nya kabur di jalan! 😜",
                            "Eits, Box ID-nya hilang! 😱 Sepertinya kami gak bisa bantu prediksi kalau ID Box-nya nggak ada. Cek lagi deh!",
                            "Hmm... ID Box ini gak ada di database kami. Mungkin dia lagi liburan? 😆 Pastikan ID-nya sesuai sama yang ada di alat fermentasi!",
                            "Box ID ini gak terdaftar nih! 😬 Mungkin ID-nya kabur ke tempat lain? Pastikan ID-nya beneran sesuai ya!",
                            "Yikes! Kami gak bisa menemukan ID Box ini. 😅 Jangan-jangan ID-nya kabur ke dunia lain? Coba pastiin lagi ya!"
                        )
                        val randomKalimat = kalimatList.random()
                        prediksiTextView.text = randomKalimat
                        prediksiTextView.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                        prediksiTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    }else{
                        alkoholTextView.visibility = View.VISIBLE
                        kelembapanTextView.visibility = View.VISIBLE
                        suhuTextView.visibility = View.VISIBLE
                        tvsuhu.visibility = View.VISIBLE
                        tvkelembapan.visibility = View.VISIBLE
                        tvalkohol.visibility = View.VISIBLE
                        eqlsuhu.visibility = View.VISIBLE
                        eqlkelembapan.visibility = View.VISIBLE
                        eqlalkohol.visibility = View.VISIBLE

                        alkoholTextView.text = "$alkohol %"
                        kelembapanTextView.text = "$kelembapan %"
                        suhuTextView.text = "$suhu°C"

                        val alkoholValue = alkohol.toDoubleOrNull() ?: 0.0
                        val kelembapanValue = kelembapan.toDoubleOrNull() ?: 0.0
                        val suhuValue = suhu.toDoubleOrNull() ?: 0.0

                        calculatePrediction(alkoholValue, kelembapanValue, suhuValue)
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }

        private fun calculatePrediction(alkohol: Double, kelembapan: Double, suhu: Double) {
            val database = FirebaseDatabase.getInstance()
            val ref = database.reference.child("model_regresi")

            ref.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val intercept = snapshot.child("b1").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                    val alkoholCoef = snapshot.child("b4").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                    val kelembapanCoef = snapshot.child("b3").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                    val suhuCoef = snapshot.child("b2").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0

                    val prediction = intercept + (alkohol * alkoholCoef)  + (kelembapan * kelembapanCoef) + (suhu * suhuCoef)
                    var jam = prediction.toInt()

                    var menit = ((prediction - jam) * 60).toInt()
                    if (jam < 0) {
                        jam = 0
                        menit = 0
                    }
                    val kalimatMatang = listOf(
                        "🎉 Tape akhirnya matang! 🍚 Rasakan lezatnya, tapi jangan sampe kebanyakan! 😋",
                        "🍚 Tape sudah siap! 🎉 Waktunya nikmatin hasil fermentasi ini, tapi jangan sampe kelamaan ya! 😍",
                        "🥳 Tape udah jadi! Gak sabar kan? Cepet habisin sebelum ada yang nyusup! 🍴",
                        "🎊 Tape matang, siap disajikan! Jangan kebanyakan ya, nanti malah ketagihan! 😋",
                        "🍽️ Tape udah jadi, waktunya makan! Jangan lupa, hasil fermentasi ini luar biasa! 🎉"
                    )

                    val kalimatFermentasi = listOf(
                        "Fermentasi hampir selesai! Tinggal $jam jam $menit menit lagi, sabar ya...😏",
                        "Sabar dikit lagi, tinggal $jam jam $menit menit. Sementara itu, nonton aja dulu! 😆",
                        "Proses fermentasi tinggal $jam jam $menit menit lagi, siap-siap bersiap! ⏳",
                        "Fermentasi tinggal $jam jam $menit menit lagi, jangan sampai kelewatan ya! 😅",
                        "Tunggu ya, tinggal $jam jam $menit menit lagi, semoga sabarnya sebanding dengan hasilnya! 😜"
                    )


                    // Memilih kalimat acak sesuai kondisi
                    if (jam == 0 && menit == 0) {
                        prediksiTextView.text = kalimatMatang.random() // Pilih kalimat acak dari kalimatMatang
                    } else {
                        prediksiTextView.text = kalimatFermentasi.random() // Pilih kalimat acak dari kalimatFermentasi
                    }

                } else {

                }
            }.addOnFailureListener {
            }
        }
        private fun calculatePredictionForItem() {
            val database = FirebaseDatabase.getInstance()
            val ref = database.reference.child("model_regresi")

            ref.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val intercept = snapshot.child("b1").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                    val alkoholCoef = snapshot.child("b4").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                    val kelembapanCoef = snapshot.child("b3").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                    val suhuCoef = snapshot.child("b2").getValue(String::class.java)?.toDoubleOrNull() ?: 0.0

                    val alkohol = alkoholTextView.text.toString().replace("%", "").toDoubleOrNull() ?: 0.0
                    val kelembapan = kelembapanTextView.text.toString().replace("%", "").toDoubleOrNull() ?: 0.0
                    val suhu = suhuTextView.text.toString().replace("°C", "").toDoubleOrNull() ?: 0.0

                    val prediction = intercept + (alkohol * alkoholCoef) +  (kelembapan * kelembapanCoef) + (suhu * suhuCoef)
                    var jam = prediction.toInt()

                    var menit = ((prediction - jam) * 60).toInt()

                    if (jam < 0) {
                        jam = 0
                        menit = 0
                    }
                    if(alkohol == 0.0  && kelembapan == 0.0 && suhu == 0.0){
                        showFermentationDialog(jam, menit,true)

                    }else{
                        showFermentationDialog(jam, menit)
                    }

                } else {

                }
            }.addOnFailureListener {
            }
        }
        private fun showFermentationDialog(jam: Int, menit: Int, isBoxEmpty: Boolean = false) {

            if (!isBoxEmpty){
                if (jam == 0 && menit == 0) {

                    val titles = listOf(
                        "Tadaaa! Tape Singkong Sempurna! 😜",
                        "Akhirnya Matang! Tapi Lama Banget! 😆",
                        "Nih, Tape Singkongnya! Udah Matang! 😏",
                        "Selamat! Fermentasi Berhasil... Akhirnya! 😅",
                        "Tape Singkong Matang, Tapi Kamu Gak Sabar? 😏"
                    )

                    val messages = listOf(
                        "Yay! Fermentasi selesai dan tape singkong udah matang! Waktunya santai dan nikmatin hasilnya, gak sia-sia nunggu! 😜",
                        "Selesai juga! Tape singkong udah matang, akhirnya bisa dinikmatin setelah penantian panjang! 😆",
                        "Tadaaa! Fermentasi sukses, tape singkong siap disantap! Gak sabar, kan? 😏",
                        "Fermentasi selesai! Tape singkong udah matang, akhirnya terbayar juga kesabaran kamu! 😅",
                        "Selamat! Tape singkong udah matang. Kamu pasti gak sabar, tapi tenang, semua usaha terbayar! 😋"
                    )

                    val randomMessage = messages[Random.nextInt(messages.size)]
                    val randomTitle = titles[Random.nextInt(titles.size)]
                    DialogUtilsFermentation.showLoading(
                        itemView.context as androidx.fragment.app.FragmentActivity,
                        randomTitle,
                        randomMessage,
                        R.raw.aa
                    )
                }else if (jam == 0 && menit > 0) {
                    val titles = listOf(
                        "Eh, Hampir Kelar Lo! 😜",
                        "Dikit Lagi, Sabar! 😏",
                        "Fermentasi Makin Dekat, Kalian Bisa! 💪",
                        "Udah Hampir Sih, Tapi Lama Lagi! 😬",
                        "Sabar Dikit Lagi, Pasti Enak! 😋"
                    )

                    val messages = listOf(
                        "Tinggal $menit menit lagi, ya! Gak lama kok, tapi semangat tetep ya! 😜",
                        "Hanya $menit menit lagi! Sabar ya, bentar lagi tape singkongnya siap buat disantap! 😅",
                        "Fermentasi cuma tinggal $menit menit! Jangan panik, kamu pasti bisa nunggu! 😆",
                        "Udah hampir selesai! Hanya $menit menit lagi, tahan sebentar lagi! 😎",
                        "Cuma $menit menit lagi! Sabar ya, gak lama kok, tinggal dikit aja! 😏"
                    )

                    val randomMessage = messages[Random.nextInt(messages.size)]
                    val randomTitle = titles[Random.nextInt(titles.size)]
                    DialogUtilsFermentation.showLoading(
                        itemView.context as androidx.fragment.app.FragmentActivity,
                        randomTitle,
                        randomMessage,
                        R.raw.aa
                    )
                }else if (jam in 1..10) {
                    val titles = listOf(
                        "Dikit Lagi! Gimana Nih? 😏",
                        "Masih Lama Sih? 😬",
                        "Hampir Kelar, Bentar Lagi! 😎",
                        "Fermentasi? Sudah Cepet Aja! 😜",
                        "Ngapain Ditunggu? 😆"
                    )

                    val messages = listOf(
                        "Wah, tinggal $jam jam $menit menit lagi! Ayo, sabar sedikit lagi, gak lama kok! 😜",
                        "Fermentasi tinggal $jam jam $menit menit lagi! Ya, gitu deh, tinggal nunggu sedikit aja. 😏",
                        "Cepet banget! Cuma tinggal $jam jam $menit menit lagi. Udah siap-siap makan? 😅",
                        "Tinggal sedikit lagi! Proses selesai dalam $jam jam $menit menit. Gimana, sabar gak? 😂",
                        "Wah, nungguin lama-lama bikin kamu tambah lapar ya? Tinggal $jam jam $menit menit lagi kok. 😆"
                    )

                    val randomMessage = messages[Random.nextInt(messages.size)]  // Pilih pesan secara acak
                    val randomTitle = titles[Random.nextInt(titles.size)]  // Pilih judul secara acak
                    DialogUtilsFermentation.showLoading(
                        itemView.context as androidx.fragment.app.FragmentActivity,
                        randomTitle,
                        randomMessage,
                        R.raw.aa
                    )
                }
                else if (jam in 11..20) {
                    val titles = listOf(
                        "Ayo Gimana Sih! 😤",
                        "Lama Banget! 😩",
                        "Kok Belum Selesai? 🧐",
                        "Masih Lama, Tenang Aja! 😅",
                        "Nungguin Terus Nih? 😂"
                    )

                    val messages = listOf(
                        "Sabar sedikit lagi, tinggal $jam jam $menit menit. Jangan jadi gila nungguin ya! 😆",
                        "Fermentasi masih berjalan, tinggal $jam jam $menit menit lagi. Kalau udah lapar, coba sabar dulu! 😜",
                        "Udah $jam jam $menit menit! Masih gak sabar? Yaudah deh, tinggal bentar lagi kok! 🤪",
                        "Cuma tinggal $jam jam $menit menit lagi. Sabar, nanti juga kelar! Kamu udah siap makan? 🤭",
                        "Aduh, prosesnya lama banget ya? Tinggal $jam jam $menit menit lagi. Coba tahan lapar ya! 😂"
                    )

                    val randomMessage = messages[Random.nextInt(messages.size)]  // Pilih pesan secara acak
                    val randomTitle = titles[Random.nextInt(titles.size)]  // Pilih judul secara acak
                    DialogUtilsFermentation.showLoading(
                        itemView.context as androidx.fragment.app.FragmentActivity,
                        randomTitle,
                        randomMessage,
                        R.raw.bb
                    )
                } else if (jam in 21..30) {
                    val titles = listOf(
                        "Sabar Dikit Lagi... ⏳",
                        "Masih Nunggu, Kan? ⏰",
                        "Sedikit Lagi, Jangan Kabur! 😬",
                        "Proses Panjang, Hasil Nikmat! 🍚",
                        "Eh, Masih Lama! 😅"
                    )

                    val messages = listOf(
                        "Fermentasi sudah berjalan cukup lama, tinggal $jam jam $menit menit lagi. Udah sabar kan? Sabar dikit lagi! 😜",
                        "Proses fermentasi hampir selesai, tinggal $jam jam $menit menit lagi. Terus bertahan ya, jangan kabur! 😏",
                        "Sedikit lagi selesai, tinggal $jam jam $menit menit. Jangan cemas, kamu pasti bisa nunggu! 😬",
                        "Prosesnya panjang, tapi hasilnya enak! Tinggal $jam jam $menit menit lagi. Tetap semangat ya! 😅",
                        "Eh, lama banget kan? Tinggal $jam jam $menit menit lagi, siap-siap nikmatin hasilnya! 😎"
                    )

                    val randomMessage = messages[Random.nextInt(messages.size)]  // Pilih pesan secara acak
                    val randomTitle = titles[Random.nextInt(titles.size)]  // Pilih judul secara acak
                    DialogUtilsFermentation.showLoading(
                        itemView.context as androidx.fragment.app.FragmentActivity,
                        randomTitle,
                        randomMessage,
                        R.raw.cc
                    )
                } else if (jam in 31..40) {
                    val titles = listOf(
                        "Sabar Dikit Lagi! ⏳, Tapi Boong 😋",
                        "Proses Fermentasi Lama, Gak Usah Kabur! 🕰",
                        "Tetap Semangat, Kamu Gak Sendirian! 😌",
                        "Proses Lama, Hasil Makanan Enak! 🍚",
                        "Sedikit Lagi, Hasil Memuaskan! ⏳"
                    )

                    val messages = listOf(
                        "Fermentasi masih butuh waktu sedikit lebih lama. Sabar ya, tinggal $jam jam $menit menit lagi! 😉",
                        "Masih ada waktu, tinggal $jam jam $menit menit lagi. Jangan kabur dulu ya! 😏",
                        "Perjalanan fermentasi memang lama, tinggal $jam jam $menit menit lagi. Tetap semangat, gak jauh kok! 😬",
                        "Sabar sedikit lagi, fermentasi masih berjalan. Tinggal $jam jam $menit menit lagi! Udah gak lama kok! 😅",
                        "Proses fermentasi masih berlangsung, tinggal $jam jam $menit menit lagi. Hasilnya pasti memuaskan! Gak sia-sia deh! 😎"
                    )


                    val randomMessage = messages[Random.nextInt(messages.size)]  // Pilih pesan secara acak
                    val randomTitle = titles[Random.nextInt(titles.size)]  // Pilih judul secara acak
                    DialogUtilsFermentation.showLoading(
                        itemView.context as androidx.fragment.app.FragmentActivity,
                        randomTitle,
                        randomMessage,
                        R.raw.dd
                    )
                } else if (jam in 41..50) {
                    val titles = listOf(
                        "Proses Panjang, Hasil Sempurna! 🕑",
                        "Masih Lama, Tapi Tak Mengapa! 😅",
                        "Proses Berlanjut... 😌",
                        "Sabar, Hasil Akan Memuaskan! ⏳",
                        "Huhu..masih lama, Kesabaran Membayar! 🕰️"
                    )

                    val messages = listOf(
                        "Fermentasi memerlukan waktu lebih banyak lagi, tapi tetap semangat ya! Masih $jam jam $menit menit... Cuma sedikit lagi kok, tahanin ya! 😜",
                        "Waktu fermentasi masih cukup lama, tinggal $jam jam $menit menit lagi. Jangan menyerah ya! Kamu bisa nonton 2 episode series lagi, santai! 📺",
                        "Fermentasi masih butuh waktu, tinggal $jam jam $menit menit lagi. Semangat, kamu pasti bisa! Pikirin aja enaknya makan nanti! 🤤",
                        "Proses fermentasi sedang berlangsung, tinggal $jam jam $menit menit lagi. Sabar ya, hasilnya pasti sepadan! Gak akan kecewa kok, udah pernah coba! 😏",
                        "Sedikit lagi, fermentasi selesai! Hanya tinggal $jam jam $menit menit lagi. Kamu pasti puas dengan hasilnya! Percayalah, itu bakal worth it! 💪"
                    )


                    val randomMessage = messages[Random.nextInt(messages.size)]  // Pilih pesan secara acak
                    val randomTitle = titles[Random.nextInt(titles.size)]  // Pilih judul secara acak
                    DialogUtilsFermentation.showLoading(
                        itemView.context as androidx.fragment.app.FragmentActivity,
                        randomTitle,
                        randomMessage,
                        R.raw.ee
                    )
                }
            }else{
                val titles = listOf(
                    "Box Mana? Gak Ada! ⚠️",
                    "Peringatan! Box ID Kosong! 🚫 Gak Bisa Gitu Aja!",
                    "Oops! Box ID Hilang, Kayak Punya Kamu! 😜",
                    "Gagal! Box ID Yang Kamu Masukkan Gak Valid! ❌",
                    "Perhatian! Box Kamu Kemana? Gak Ketemu! ⚠️"
                )

                val messages = listOf(
                    "Box yang kamu cari gak ditemukan di database, mungkin ID-nya salah. Coba lagi deh, jangan malas! 😆",
                    "Hadeh, Box ID-nya kosong! Coba cek lagi, pasti ada yang salah! 😅",
                    "Gak ada data untuk Box ini! Pastikan ID-nya bener, atau coba yang lain. Jangan harap-harap cemas! 😜",
                    "Box ID gak terdaftar! Cek dulu deh, jangan asal input ID, coba lagi ya! 😏",
                    "Kami gak bisa nemuin Box ID yang kamu masukkan. Coba pake ID lain yang lebih bisa dipercaya! 😆"
                )
                val randomMessage = messages[Random.nextInt(messages.size)]
                val randomTitle = titles[Random.nextInt(titles.size)]

                DialogUtilsFermentation.showLoading(
                    itemView.context as androidx.fragment.app.FragmentActivity,
                    randomTitle,
                    randomMessage,
                    R.raw.ff
                )
            }
        }
    }
    fun removeItem(context: Context, position: Int, id: String) {
        val username = SharedPreferencesHelper.getUsername(context)!!

        val boxViewModel = BoxViewModel()
        boxViewModel.removeIdFromFirebase(username, id)

        val updatedIds = ids.toMutableList()
        updatedIds.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxIdViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_prediksi, parent, false)
        return BoxIdViewHolder(view)
    }
    override fun onBindViewHolder(holder: BoxIdViewHolder, position: Int) {
        val id = ids[position]
        holder.bind(id)

        val color = Color.parseColor("#5A5A5A")
        val colorBlack = Color.parseColor("#FF000000")
        holder.alkoholTextView.setTextColor(color)
        holder.kelembapanTextView.setTextColor(color)
        holder.suhuTextView.setTextColor(color)
        holder.tvsuhu.setTextColor(color)
        holder.tvkelembapan.setTextColor(color)
        holder.tvalkohol.setTextColor(color)
        holder.eqlsuhu.setTextColor(color)
        holder.eqlkelembapan.setTextColor(color)
        holder.eqlalkohol.setTextColor(color)

        holder.idTextView.setTextColor(colorBlack)
        holder.prediksiTextView.setTextColor(colorBlack)


    }
    override fun getItemCount(): Int = ids.size

}
