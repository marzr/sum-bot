package com.github.marzr.sum.bot

import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.editMessageText
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.Message
import eu.vendeli.tgbot.types.internal.getOrNull
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime.now
import kotlin.math.roundToInt

suspend fun main() {
    Database.connect(
        "jdbc:postgresql://localhost:5432/sumbot",
        driver = "org.postgresql.Driver",
        user = "sumbot",
        password = ""
    )
    transaction {
        SchemaUtils.create(Links)
    }
    val bot = TelegramBot("")
    bot.handleUpdates {
        onMessage {
            handleMessage(this.update.message, bot)
        }
        onEditedMessage {
            handleEditedMessage(this.update.editedMessage, bot)
        }
    }
}

private suspend fun handleMessage(message: Message, bot: TelegramBot) {
    val sent = message("Sum = ${calculateSum(message.text ?: "")}").sendAsync(message.chat, bot)
    val sentId = sent.await().getOrNull()?.messageId!!
    transaction {
        Link.new {
            messageId = message.messageId
            chatId = message.chat.id
            replyId = sentId
            createdAt = now().toKotlinLocalDateTime()
        }
    }
}

private suspend fun handleEditedMessage(editedMessage: Message, bot: TelegramBot) {
    val replyId = transaction {
        Link.find {
            (Links.messageId eq editedMessage.messageId) and (Links.chatId eq editedMessage.chat.id)
        }.firstOrNull()?.replyId
    }?: return
    editMessageText(replyId) {
        "Sum = ${calculateSum(editedMessage.text ?: "")}"
    }.send(editedMessage.chat.id, bot)
}

private fun calculateSum(text: String): Int {
    return text.split('\n').asSequence().mapNotNull {
        it.lastIndexOf(' ').let { spaceIndex ->
            if (spaceIndex < 0) null
            else it.drop(spaceIndex + 1)
        }
    }.map {
        it.replace(',', '.')
    }.map {
        it.lastIndexOf('*').let { starIndex ->
            if (starIndex < 0)
                it.toDoubleOrZero()
            else
                it.take(starIndex).toDoubleOrZero() * it.drop(starIndex + 1).toDoubleOrZero()
        }
    }.sum().roundToInt()
}

fun String.toDoubleOrZero(): Double = this.toDoubleOrNull() ?: 0.0
