package com.jariahxp.helper.adapter

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.jariahxp.utils.DialogUtils

class SwipeToDeleteCallback(
    private val adapter: DataBoxAdapter,
) : ItemTouchHelper.Callback() {

    private val textColor = Color.WHITE // Warna teks di latar belakang
    private val backgroundColor = Color.RED // Warna latar belakang saat swipe
    private val stopThreshold = 0.3f // Threshold untuk menghentikan swipe
    private val rightSwipeThreshold = 0.1f // Threshold untuk swipe ke kanan


    override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val swipeFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // Swipe ke kiri dan kanan

        return makeMovementFlags(0, swipeFlags)
    }

    // Mengatur threshold untuk swipe agar lebih sensitif
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        val translationX = viewHolder.itemView.translationX
        return if (translationX < 0) { // Swipe ke kiri
            stopThreshold
        } else { // Swipe ke kanan
            rightSwipeThreshold
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemWidth = itemView.width
        val absDX = Math.abs(dX)

        // Tetapkan batasan untuk gerakan horizontal
        val cappedDX = when {
            dX < 0 && absDX > itemWidth * stopThreshold -> itemWidth * stopThreshold * -1
            dX > 0 && absDX > itemWidth * rightSwipeThreshold -> itemWidth * rightSwipeThreshold
            else -> dX
        }

        // Terapkan translasi X ke itemView
        itemView.translationX = cappedDX

        // Gambar latar belakang dengan efek melengkung
        val paint = Paint()
        val path = Path()

        // Tentukan radius untuk sudut melengkung
        val cornerRadius = 40f // Radius untuk sudut melengkung
        val cornerRadiusl = 15f // Radius untuk sudut melengkung

        if (dX < 0) { // Swipe ke kiri
            // Tentukan path dengan sudut melengkung
            val rectF = RectF(
                itemView.right + cappedDX, itemView.top.toFloat(),
                itemView.right.toFloat(), itemView.bottom.toFloat()
            )
            path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW)

            // Membuat gradien atau warna pada latar belakang
            val gradient = LinearGradient(
                itemView.right + cappedDX, itemView.top.toFloat(),
                itemView.right.toFloat(), itemView.bottom.toFloat(),
                Color.TRANSPARENT, backgroundColor, Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            c.drawPath(path, paint)

            // Gambar teks "Delete"
            val text = "Delete"
            val textPaint = Paint()
            textPaint.color = textColor
            textPaint.textSize = 50f
            textPaint.textAlign = Paint.Align.CENTER
            val xPos = itemView.right - (itemWidth * stopThreshold / 2)
            val yPos = itemView.top + (itemView.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2)
            c.drawText(text, xPos, yPos, textPaint)

        } else { // Swipe ke kanan
            // Tentukan path dengan sudut melengkung
            val rectF = RectF(
                itemView.left.toFloat(), itemView.top.toFloat(),
                itemView.left + cappedDX, itemView.bottom.toFloat()
            )
            path.addRoundRect(rectF, cornerRadiusl, cornerRadiusl, Path.Direction.CW)

            // Membuat gradien atau warna pada latar belakang
            val gradient = LinearGradient(
                itemView.left.toFloat(), itemView.top.toFloat(),
                itemView.left + cappedDX, itemView.bottom.toFloat(),
                Color.TRANSPARENT, Color.GREEN, Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            c.drawPath(path, paint)
        }

        // Panggil super hanya jika swipe belum berhenti
        if (isCurrentlyActive || dX > 0 || absDX <= itemWidth * stopThreshold) {
            super.onChildDraw(c, recyclerView, viewHolder, cappedDX, dY, actionState, isCurrentlyActive)
        }
    }



    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Ambil posisi item yang di-swipe
        val position = viewHolder.adapterPosition
        val id = adapter.ids[position]
        val context = viewHolder.itemView.context

        if (direction == ItemTouchHelper.RIGHT) {
            // Kembalikan item ke posisi normal
            adapter.notifyItemChanged(position)
        } else if (direction == ItemTouchHelper.LEFT) {
            // Lakukan aksi delete
            val id = adapter.ids[position]
            showConfirmationDialog(viewHolder.itemView.context, position, id)
        }
            // Menampilkan dialog konfirmasi

    }
    private fun showConfirmationDialog(context: Context, position: Int, id: String) {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Konfirmasi Penghapusan")
            .setMessage("Apakah Anda yakin ingin menghapus item ini?")
            .setPositiveButton("Hapus") { dialog, _ ->

                adapter.removeItem(context, position, id)
                dialog.dismiss()
                if (context is Activity) {
                    DialogUtils.showLoading(
                        context,
                        "Menghapus ID BOX",
                        "Mohon tunggu. Kami akan menghapus ID Box Anda.",
                        1500L
                    )
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                // Jika batal, kembalikan item ke posisi semula
                adapter.notifyItemChanged(position)
                dialog.dismiss()
            }
            .setCancelable(false) // Menonaktifkan penutupan dialog dengan klik di luar dialog
            .create()

        dialog.show()
    }
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false // Tidak perlu mendukung drag and drop
    }
}
