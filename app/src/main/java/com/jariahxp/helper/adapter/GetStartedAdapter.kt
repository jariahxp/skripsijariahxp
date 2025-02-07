package com.jariahxp.helper.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.jariahxp.R
import com.jariahxp.model.PageData

class GetStartedAdapter(private val pages: List<PageData>) :
    RecyclerView.Adapter<GetStartedAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lottieView: LottieAnimationView = itemView.findViewById(R.id.lottieAnimation)
        val titleTextView: TextView = itemView.findViewById(R.id.tvTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_page, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val page = pages[position]
        holder.lottieView.setAnimation(page.animationRes)
        holder.titleTextView.text = page.title
        holder.descriptionTextView.text = page.description
    }

    override fun getItemCount(): Int = pages.size
}