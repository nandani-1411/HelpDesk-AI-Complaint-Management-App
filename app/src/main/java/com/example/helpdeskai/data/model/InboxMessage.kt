package com.example.helpdeskai.data.model

data class InboxMessage(
    val id: Long,
    val title: String,
    val body: String,
    val tag: String,
    val timestamp: Long,
    val isUnread: Boolean = false
)
