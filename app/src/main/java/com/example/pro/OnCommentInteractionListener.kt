package com.example.pro

interface OnCommentInteractionListener {
    fun onReplyClick(comment: Comment)
    fun onEditClick(comment: Comment)
    fun onDeleteClick(comment: Comment)
}