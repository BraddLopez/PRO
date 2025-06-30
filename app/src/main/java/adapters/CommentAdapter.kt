package adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.pro.R
import com.google.firebase.auth.FirebaseAuth
import models.Comment

class CommentAdapter(
    private val context: Context,
    private val commentList: MutableList<Comment>,
    private val onEditClick: (Comment) -> Unit,
    private val onDeleteClick: (Comment) -> Unit,
    private val onReplyClick: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private var comentarioDestacadoId: String? = null
    private val commentIdToUserNameMap: MutableMap<String, String> = mutableMapOf()
    private val repliesMap: MutableMap<String, MutableList<Comment>> = mutableMapOf()

    // Mapa para controlar la visibilidad de las respuestas
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
        val cardView: CardView = itemView.findViewById(R.id.commentCard)
        val repliesContainer: LinearLayout = itemView.findViewById(R.id.repliesContainer)
        val tvToggleReplies: TextView = itemView.findViewById(R.id.tvToggleReplies)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_coment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentList[position]

        holder.userNameText.text = comment.userName
        holder.dateText.text = comment.date
        holder.commentText.text = comment.text
        holder.userImage.setImageResource(R.drawable.uc_user)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        // Opciones de edición y eliminación solo para el comentario del usuario actual
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

        // Si el comentario no tiene parentId, es un comentario principal
        if (comment.parentId.isNullOrEmpty()) {
            holder.replyTextView.visibility = View.VISIBLE
            holder.replyTextView.setOnClickListener {
                onReplyClick(comment) // Acción de responder
            }
            holder.replyLabel.visibility = View.GONE

            // Cargar respuestas para este comentario
            val replies = repliesMap[comment.id] ?: emptyList()
            holder.repliesContainer.removeAllViews()

            if (replies.isNotEmpty()) {
                holder.repliesContainer.visibility = View.GONE  // Inicialmente oculta las respuestas
                holder.tvToggleReplies.visibility = View.VISIBLE

                // Alternar la visibilidad de las respuestas
                holder.tvToggleReplies.setOnClickListener {
                    val isRepliesVisible = visibilityStateMap[comment.id] ?: false
                    visibilityStateMap[comment.id] = !isRepliesVisible
                    holder.repliesContainer.visibility = if (isRepliesVisible) View.GONE else View.VISIBLE
                    holder.tvToggleReplies.text = if (isRepliesVisible) "Mostrar respuestas" else "Ocultar respuestas"
                }

                // Mostrar las respuestas
                for (reply in replies) {
                    val replyView = LayoutInflater.from(context).inflate(R.layout.item_reply, holder.repliesContainer, false)

                    val replyUserName = replyView.findViewById<TextView>(R.id.replyUserName)
                    val replyDate = replyView.findViewById<TextView>(R.id.replyDate)
                    val replyText = replyView.findViewById<TextView>(R.id.replyText)
                    val replyOptions = replyView.findViewById<ImageView>(R.id.moreOptionsReply)

                    replyUserName.text = reply.userName
                    replyDate.text = reply.date
                    replyText.text = reply.text

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

        // Estilo de comentario destacado
        if (comment.id == comentarioDestacadoId) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFF9C4"))
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE)
        }
    }

    override fun getItemCount(): Int = commentList.size
}


