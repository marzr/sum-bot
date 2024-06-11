package com.github.marzr.sum.bot.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Links : LongIdTable() {
    val messageId = long("message_id").index()
    val chatId = long("chat_id")
    val replyId = long("reply_id")
    val createdAt = datetime("created_at")
}

class Link(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Link>(Links)

    var messageId by Links.messageId
    var chatId by Links.chatId
    var replyId by Links.replyId
    var createdAt by Links.createdAt
}
