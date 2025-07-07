package adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pro.R
import models.UserActivity

class ActivityAdapter(private val activities: List<UserActivity>) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_item, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = activities[position]

        // Seteamos el nombre de la playa
        holder.beachNameText.text = activity.beachName

        // Seteamos el comentario
        holder.activityText.text = activity.comment

        // Seteamos la puntuación (RatingBar)
        holder.activityRating.rating = activity.rating // Aquí asignamos la puntuación al RatingBar

        // Seteamos la fecha
        holder.activityDate.text = activity.date
    }

    override fun getItemCount(): Int = activities.size

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val beachNameText: TextView = itemView.findViewById(R.id.beachName)
        val activityText: TextView = itemView.findViewById(R.id.activityText)
        val activityRating: RatingBar = itemView.findViewById(R.id.activityRating)
        val activityDate: TextView = itemView.findViewById(R.id.activityDate)
    }
}