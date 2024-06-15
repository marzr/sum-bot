package com.github.marzr.sum.bot

import com.github.marzr.sum.bot.calc.Calculations.calculateSum
import com.github.marzr.sum.bot.db.Link
import com.github.marzr.sum.bot.db.Links
import com.github.marzr.sum.bot.db.Product
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

suspend fun main() {
    Database.connect(
        "jdbc:postgresql://localhost:5432/sumbot",
        driver = "org.postgresql.Driver",
        user = "sumbot",
        password = "sumbot"
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

private suspend fun handleMessage(message: Message, bot: TelegramBot) = when {
    message.from == null -> println("ignore this message")
    message.text?.startsWith("save") == true -> handleSaveMessage(message, bot)
    else -> handleDefaultMessage(message, bot)
}

private suspend fun handleSaveMessage(message: Message, bot: TelegramBot) {
    runCatching {
        saveProducts(message.text!!, message.from!!.id)
        message("accepted").send(message.chat, bot)
    }.onFailure {
        message("error").send(message.chat, bot)
    }
}

fun saveProducts(text: String, user: Long) = transaction {
    val now = now().toKotlinLocalDateTime()
    text.split('\n').drop(1).forEach {
        Product.new {
            userId = user
            name = it.substringBeforeLast(' ')
            calories = it.substringAfterLast(' ').toInt()
            createdAt = now
        }
    }
}

private suspend fun handleDefaultMessage(message: Message, bot: TelegramBot) {
    val sent = message(composeReplay(message)).sendAsync(message.chat, bot)
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
    } ?: return
    editMessageText(replyId) { composeReplay(editedMessage) }.send(editedMessage.chat.id, bot)
}

private fun composeReplay(message: Message): String {
    val sum = calculateSum(message.text ?: "", message.from!!.id)
    return "Sum = $sum"
}
