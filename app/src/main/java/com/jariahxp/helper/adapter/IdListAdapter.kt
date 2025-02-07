package com.jariahxp.helper.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jariahxp.R

class IdListAdapter(private val idList: MutableList<String>, private val onItemClicked: (String) -> Unit) : RecyclerView.Adapter<IdListAdapter.IdViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IdViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_id, parent, false)
        return IdViewHolder(view)
    }

    override fun onBindViewHolder(holder: IdViewHolder, position: Int) {
        holder.bind(idList[position])
    }

    override fun getItemCount(): Int {
        return idList.size
    }

    inner class IdViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textView: TextView = view.findViewById(R.id.textViewId)

        fun bind(id: String) {
            textView.text = id
            itemView.setOnClickListener {
                onItemClicked(id)  // Ketika item diklik, panggil fungsi untuk menghapus ID
            }
        }
    }
}

