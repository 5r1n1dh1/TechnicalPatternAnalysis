package com.tpa.app.ui.wishlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tpa.app.R
import com.tpa.app.data.wishlist.WishlistEntry

class WishlistAdapter(private var items: List<WishlistEntry> = emptyList()) :
    RecyclerView.Adapter<WishlistAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val sym: TextView = v.findViewById(R.id.sym)
        val date: TextView = v.findViewById(R.id.date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wishlist, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val e = items[pos]
        h.sym.text = e.symbol
        h.date.text = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(e.addedAt))
    }

    fun submit(list: List<WishlistEntry>) {
        items = list
        notifyDataSetChanged()
    }
}
