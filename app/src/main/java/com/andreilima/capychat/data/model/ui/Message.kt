package com.andreilima.capychat.data.model.ui

import com.andreilima.capychat.data.model.MessageStatus

data class Message(
    val id: String = "",
    val sender: String = "",
    val text: String = "",
    val isMine: Boolean = false,
    val time: String = "",
    val messageType: String = "text",
    val status: MessageStatus = MessageStatus.SENT,
    val reactions: Map<String, String> = emptyMap(),
    val replyToText: String = "",
    val replyToSender: String = "",
    val selfDestructAt: Long = 0L
)