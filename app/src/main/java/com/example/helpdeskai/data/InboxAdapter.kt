package com.example.helpdeskai.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.helpdeskai.R
import com.example.helpdeskai.data.model.InboxMessage
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InboxAdapter(
    private var messageList: List<InboxMessage>,
    private val onItemClick: ((InboxMessage) -> Unit)? = null
) : RecyclerView.Adapter<InboxAdapter.InboxViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    inner class InboxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView as MaterialCardView
        val tvTag: TextView = itemView.findViewById(R.id.tv_inbox_type_badge)
        val tvDate: TextView = itemView.findViewById(R.id.tv_inbox_date)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_inbox_title)
        val tvBody: TextView = itemView.findViewById(R.id.tv_inbox_body)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inbox_card, parent, false)
        return InboxViewHolder(view)
    }

    override fun onBindViewHolder(holder: InboxViewHolder, position: Int) {
        val item = messageList[position]
        val context = holder.itemView.context

        holder.tvTitle.text = item.title
        holder.tvBody.text = item.body
        holder.tvTag.text = item.tag
        holder.tvDate.text = dateFormat.format(Date(item.timestamp))

        if (item.isUnread) {
            holder.card.strokeColor = ContextCompat.getColor(context, R.color.primary)
            holder.card.strokeWidth = 3
        } else {
            holder.card.strokeColor = ContextCompat.getColor(context, R.color.divider)
            holder.card.strokeWidth = 1
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = messageList.size

    fun updateList(newList: List<InboxMessage>) {
        this.messageList = newList
        notifyDataSetChanged()
    }
}
