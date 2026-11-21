package com.example.helpdeskai.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.helpdeskai.R
import com.example.helpdeskai.data.model.Complaint
import com.example.helpdeskai.utils.CameraHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView Adapter to render grievance tickets with status badges and details.
 */
class ComplaintAdapter(
    private var complaintList: List<Complaint>,
    private val onItemClick: ((Complaint) -> Unit)? = null
) : RecyclerView.Adapter<ComplaintAdapter.ComplaintViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    inner class ComplaintViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tv_card_category)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_card_status)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_card_title)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_card_description)
        val ivPhoto: ImageView = itemView.findViewById(R.id.iv_card_photo)
        val layoutLocation: LinearLayout = itemView.findViewById(R.id.layout_card_location)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_card_location)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tv_card_timestamp)
        val layoutOfficerReply: LinearLayout = itemView.findViewById(R.id.layout_card_officer_reply)
        val tvOfficerReply: TextView = itemView.findViewById(R.id.tv_card_officer_reply)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComplaintViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_complaint_card, parent, false)
        return ComplaintViewHolder(view)
    }

    override fun onBindViewHolder(holder: ComplaintViewHolder, position: Int) {
        val complaint = complaintList[position]
        val context = holder.itemView.context

        holder.tvTitle.text = complaint.title
        holder.tvDescription.text = complaint.description
        holder.tvCategory.text = complaint.category
        holder.tvCategory.setTextColor(ContextCompat.getColor(context, R.color.badge_cat_text))
        holder.tvTimestamp.text = dateFormat.format(Date(complaint.timestamp))

        // Status Badge Styling with dynamic theme colors
        when (complaint.status.lowercase(Locale.ROOT)) {
            "resolved" -> {
                holder.tvStatus.text = "Resolved"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_resolved)
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_resolved))
            }
            "in progress" -> {
                holder.tvStatus.text = "In Progress"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_progress)
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_in_progress))
            }
            else -> {
                holder.tvStatus.text = "Pending"
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_pending)
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending))
            }
        }

        // Photo Preview
        if (!complaint.photoPath.isNullOrEmpty()) {
            val bitmap = CameraHelper.loadBitmapFromPath(complaint.photoPath)
            if (bitmap != null) {
                holder.ivPhoto.setImageBitmap(bitmap)
                holder.ivPhoto.visibility = View.VISIBLE
            } else {
                holder.ivPhoto.visibility = View.GONE
            }
        } else {
            holder.ivPhoto.visibility = View.GONE
        }

        // Location Info
        if (!complaint.locationAddress.isNullOrEmpty()) {
            holder.tvLocation.text = complaint.locationAddress
            holder.layoutLocation.visibility = View.VISIBLE
        } else {
            holder.layoutLocation.visibility = View.GONE
        }

        // Officer Reply - completely hide if empty or null
        if (!complaint.officerReply.isNullOrBlank()) {
            holder.tvOfficerReply.text = complaint.officerReply
            holder.layoutOfficerReply.visibility = View.VISIBLE
        } else {
            holder.layoutOfficerReply.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(complaint)
        }
    }

    override fun getItemCount(): Int = complaintList.size

    fun updateList(newList: List<Complaint>) {
        this.complaintList = newList
        notifyDataSetChanged()
    }
}
