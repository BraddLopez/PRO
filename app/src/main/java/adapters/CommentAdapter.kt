package adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.pro.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import models.Comment
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView



class CommentAdapter(
    private val context: Context,
    private val commentList: MutableList<Comment>,
    private val onEditClick: (Comment) -> Unit,
    private val onDeleteClick: (Comment) -> Unit,
    private val onReplyClick: (Comment) -> Unit,
    private val beachId: String // <-- AGREGADO
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private var comentarioDestacadoId: String? = null
    private val commentIdToUserNameMap: MutableMap<String, String> = mutableMapOf()
    private val repliesMap: MutableMap<String, MutableList<Comment>> = mutableMapOf()
    private val visibilityStateMap: MutableMap<String, Boolean> = mutableMapOf()

    fun setComentarioDestacado(commentId: String) {
        comentarioDestacadoId = commentId
        notifyDataSetChanged()
    }

    fun setCommentIdToUserNameMap(map: Map<String, String>) {
        commentIdToUserNameMap.clear()
        commentIdToUserNameMap.putAll(map)
        notifyDataSetChanged()
    }

    fun setRepliesMap(map: Map<String, List<Comment>>) {
        repliesMap.clear()
        for ((parentId, replies) in map) {
            repliesMap[parentId] = replies.toMutableList()
        }
        notifyDataSetChanged()
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameText: TextView = itemView.findViewById(R.id.userName)
        val dateText: TextView = itemView.findViewById(R.id.commentDate)
        val commentText: TextView = itemView.findViewById(R.id.commentText)
        val userImage: ImageView = itemView.findViewById(R.id.userImage)
        val moreOptions: ImageView = itemView.findViewById(R.id.moreOptions)
        val replyTextView: TextView = itemView.findViewById(R.id.btnReply)
        val replyLabel: TextView = itemView.findViewById(R.id.replyLabel)
        val cardView: MaterialCardView = itemView.findViewById(R.id.commentCard)
        val repliesContainer: LinearLayout = itemView.findViewById(R.id.repliesContainer)
        val tvToggleReplies: TextView = itemView.findViewById(R.id.tvToggleReplies)
        val heartIcon: ImageView = itemView.findViewById(R.id.heartIcon)
        val likesCountText: TextView = itemView.findViewById(R.id.likesCountText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_coment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentList[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        holder.userNameText.text = comment.userName
        holder.dateText.text = comment.date
        holder.commentText.text = comment.text
        holder.userImage.setImageResource(R.drawable.uc_user)

        val heartRef = FirebaseFirestore.getInstance()
            .collection("comments")
            .document(comment.id)
            .collection("likes")
            .document(currentUserId)

        val likesCollection = FirebaseFirestore.getInstance()
            .collection("comments")
            .document(comment.id)
            .collection("likes")

        likesCollection.get().addOnSuccessListener { snapshot ->
            holder.likesCountText.text = snapshot.size().toString()
        }

        heartRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                holder.heartIcon.setImageResource(R.drawable.ic_heart_filled)
                holder.heartIcon.setColorFilter(ContextCompat.getColor(context, R.color.heart_red))
            } else {
                holder.heartIcon.setImageResource(R.drawable.ic_heart_outline)
                holder.heartIcon.setColorFilter(ContextCompat.getColor(context, R.color.heart_gray))
            }

        }

        holder.heartIcon.setOnClickListener {
            heartRef.get().addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    // Nuevo like
                    heartRef.set(mapOf("timestamp" to FieldValue.serverTimestamp())).addOnSuccessListener {
                        holder.heartIcon.setImageResource(R.drawable.ic_heart_filled)
                        holder.heartIcon.setColorFilter(
                            ContextCompat.getColor(context, R.color.heart_green)
                        )

                        sendHeartNotification(comment.userId, comment.text, comment.userName?: "Usuario", beachId, comment.id)

                        // Transición a rojo después de 800ms
                        Handler(Looper.getMainLooper()).postDelayed({
                            holder.heartIcon.setColorFilter(
                                ContextCompat.getColor(context, R.color.heart_red)
                            )
                        }, 800)

                        // Actualizar contador
                        likesCollection.get().addOnSuccessListener {
                            holder.likesCountText.text = it.size().toString()
                        }
                    }
                } else {
                    // Quitar like
                    heartRef.delete().addOnSuccessListener {
                        holder.heartIcon.setImageResource(R.drawable.ic_heart_outline)
                        holder.heartIcon.setColorFilter(
                            ContextCompat.getColor(context, R.color.heart_gray)
                        )

                        // Actualizar contador
                        likesCollection.get().addOnSuccessListener {
                            holder.likesCountText.text = it.size().toString()
                        }
                    }
                }
            }
        }


        if (comment.userId == currentUserId) {
            holder.moreOptions.visibility = View.VISIBLE
            holder.moreOptions.setOnClickListener {
                val popup = PopupMenu(holder.itemView.context, it)
                popup.inflate(R.menu.comment_options_menu)
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_edit -> {
                            onEditClick(comment)
                            true
                        }
                        R.id.menu_delete -> {
                            onDeleteClick(comment)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        } else {
            holder.moreOptions.visibility = View.GONE
        }

        if (comment.parentId.isNullOrEmpty()) {
            holder.replyTextView.visibility = View.VISIBLE
            holder.replyTextView.setOnClickListener {
                onReplyClick(comment)
            }
            holder.replyLabel.visibility = View.GONE

            val replies = repliesMap[comment.id] ?: emptyList()
            holder.repliesContainer.removeAllViews()

            if (replies.isNotEmpty()) {
                holder.repliesContainer.visibility = View.GONE
                holder.tvToggleReplies.visibility = View.VISIBLE

                holder.tvToggleReplies.setOnClickListener {
                    val visible = visibilityStateMap[comment.id] ?: false
                    visibilityStateMap[comment.id] = !visible
                    holder.repliesContainer.visibility = if (visible) View.GONE else View.VISIBLE
                    holder.tvToggleReplies.text = if (visible) "Mostrar respuestas" else "Ocultar respuestas"
                }

                for (reply in replies) {
                    val replyView = LayoutInflater.from(context).inflate(R.layout.item_reply, holder.repliesContainer, false)
                    replyView.findViewById<TextView>(R.id.replyUserName).text = reply.userName
                    replyView.findViewById<TextView>(R.id.replyDate).text = reply.date
                    replyView.findViewById<TextView>(R.id.replyText).text = reply.text
                    val replyOptions = replyView.findViewById<ImageView>(R.id.moreOptionsReply)

                    if (reply.userId == currentUserId) {
                        replyOptions.visibility = View.VISIBLE
                        replyOptions.setOnClickListener {
                            val popup = PopupMenu(context, it)
                            popup.inflate(R.menu.comment_options_menu)
                            popup.setOnMenuItemClickListener { item ->
                                when (item.itemId) {
                                    R.id.menu_edit -> {
                                        onEditClick(reply)
                                        true
                                    }
                                    R.id.menu_delete -> {
                                        onDeleteClick(reply)
                                        true
                                    }
                                    else -> false
                                }
                            }
                            popup.show()
                        }
                    } else {
                        replyOptions.visibility = View.GONE
                    }

                    holder.repliesContainer.addView(replyView)
                }
            } else {
                holder.repliesContainer.visibility = View.GONE
                holder.tvToggleReplies.visibility = View.GONE
            }
        } else {
            holder.replyTextView.visibility = View.GONE
            holder.replyLabel.text = "↪ Respuesta a ${commentIdToUserNameMap[comment.parentId] ?: ""}"
            holder.replyLabel.visibility = View.VISIBLE
            holder.repliesContainer.visibility = View.GONE
        }

// Comprobar si es comentario destacado
        val esDestacado = comment.id == comentarioDestacadoId

// Si es comentario raíz destacado
        if (esDestacado && comment.parentId.isNullOrEmpty()) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF9C4"))
            holder.cardView.setStrokeColor(Color.parseColor("#FFC107")) // solo si usas MaterialCardView
            holder.cardView.cardElevation = 12f
        } else if (esDestacado && !comment.parentId.isNullOrEmpty()) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFFDE7"))
            holder.cardView.cardElevation = 8f
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE)
            holder.cardView.cardElevation = 0f
        }

    }

    override fun getItemCount(): Int = commentList.size

    private fun sendHeartNotification(
        receiverUserId: String,
        commentText: String,
        userName: String,
        beachId: String,
        commentId: String
    ) {
        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (receiverUserId == currentUserId) return

        db.collection("users").document(receiverUserId)
            .get()
            .addOnSuccessListener { document ->
                val token = document.getString("token")
                if (!token.isNullOrEmpty()) {
                    val functions = FirebaseFunctions.getInstance()

                    val data = hashMapOf(
                        "to" to token,
                        "notification" to mapOf(
                            "title" to "$userName reaccionó a tu comentario",
                            "body" to "\"$commentText\" ❤️",
                            "click_action" to "COMMENT_NOTIFICATION"
                        ),
                        "beachId" to beachId,
                        "commentId" to commentId
                    )

                    functions.getHttpsCallable("sendHeartNotification").call(data)
                }
            }
    }

}
