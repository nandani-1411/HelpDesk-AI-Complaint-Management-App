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

        // Visual Tracking Timeline Nodes & Connectors
        val dotSubmitted: ImageView = itemView.findViewById(R.id.dot_step_submitted)
        val line1: View = itemView.findViewById(R.id.line_step_1)
        val dotReview: ImageView = itemView.findViewById(R.id.dot_step_review)
        val line2: View = itemView.findViewById(R.id.line_step_2)
        val dotProgress: ImageView = itemView.findViewById(R.id.dot_step_progress)
        val line3: View = itemView.findViewById(R.id.line_step_3)
        val dotResolved: ImageView = itemView.findViewById(R.id.dot_step_resolved)
        val tvLabelSubmitted: TextView = itemView.findViewById(R.id.tv_label_submitted)
        val tvLabelReview: TextView = itemView.findViewById(R.id.tv_label_review)
        val tvLabelProgress: TextView = itemView.findViewById(R.id.tv_label_progress)
        val tvLabelResolved: TextView = itemView.findViewById(R.id.tv_label_resolved)
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

        // Visual Tracking Timeline Dynamic Highlighting
        val colorPrimary = ContextCompat.getColor(context, R.color.primary)
        val colorPending = ContextCompat.getColor(context, R.color.status_pending)
        val colorProgress = ContextCompat.getColor(context, R.color.status_in_progress)
        val colorResolved = ContextCompat.getColor(context, R.color.status_resolved)
        val colorMuted = ContextCompat.getColor(context, R.color.text_muted)

        when (complaint.status.lowercase(Locale.ROOT)) {
            "resolved" -> {
                holder.dotSubmitted.setImageResource(R.drawable.dot_timeline_resolved)
                holder.line1.setBackgroundResource(R.drawable.line_timeline_active)
                holder.dotReview.setImageResource(R.drawable.dot_timeline_resolved)
                holder.line2.setBackgroundResource(R.drawable.line_timeline_active)
                holder.dotProgress.setImageResource(R.drawable.dot_timeline_resolved)
                holder.line3.setBackgroundResource(R.drawable.line_timeline_active)
                holder.dotResolved.setImageResource(R.drawable.dot_timeline_resolved)

                holder.tvLabelSubmitted.setTextColor(colorResolved)
                holder.tvLabelReview.setTextColor(colorResolved)
                holder.tvLabelProgress.setTextColor(colorResolved)
                holder.tvLabelResolved.setTextColor(colorResolved)
            }
            "in progress" -> {
                holder.dotSubmitted.setImageResource(R.drawable.dot_timeline_active)
                holder.line1.setBackgroundResource(R.drawable.line_timeline_active)
                holder.dotReview.setImageResource(R.drawable.dot_timeline_active)
                holder.line2.setBackgroundResource(R.drawable.line_timeline_active)
                holder.dotProgress.setImageResource(R.drawable.dot_timeline_active)
                holder.line3.setBackgroundResource(R.drawable.line_timeline_inactive)
                holder.dotResolved.setImageResource(R.drawable.dot_timeline_inactive)

                holder.tvLabelSubmitted.setTextColor(colorPrimary)
                holder.tvLabelReview.setTextColor(colorPrimary)
                holder.tvLabelProgress.setTextColor(colorProgress)
                holder.tvLabelResolved.setTextColor(colorMuted)
            }
            else -> {
                holder.dotSubmitted.setImageResource(R.drawable.dot_timeline_active)
                holder.line1.setBackgroundResource(R.drawable.line_timeline_active)
                holder.dotReview.setImageResource(R.drawable.dot_timeline_active)
                holder.line2.setBackgroundResource(R.drawable.line_timeline_inactive)
                holder.dotProgress.setImageResource(R.drawable.dot_timeline_inactive)
                holder.line3.setBackgroundResource(R.drawable.line_timeline_inactive)
                holder.dotResolved.setImageResource(R.drawable.dot_timeline_inactive)

                holder.tvLabelSubmitted.setTextColor(colorPrimary)
                holder.tvLabelReview.setTextColor(colorPending)
                holder.tvLabelProgress.setTextColor(colorMuted)
                holder.tvLabelResolved.setTextColor(colorMuted)
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
