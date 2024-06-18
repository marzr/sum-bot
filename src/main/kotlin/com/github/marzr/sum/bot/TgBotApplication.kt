package com.github.marzr.sum.bot

import com.github.marzr.sum.bot.action.calculateSum
import com.github.marzr.sum.bot.action.deleteProducts
import com.github.marzr.sum.bot.action.listProducts
import com.github.marzr.sum.bot.action.saveProducts
import com.github.marzr.sum.bot.db.Link
import com.github.marzr.sum.bot.db.Links
import com.github.marzr.sum.bot.db.Products
import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.editMessageText
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.Message
import eu.vendeli.tgbot.types.internal.getOrNull
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime.now

suspend fun main() {
    Database.connect(
        "jdbc:postgresql://localhost:5432/sumbot",
        driver = "org.postgresql.Driver",
        user = "",
        password = ""
    )
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(Links, Products)
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
    message.from == null || message.text == null -> println("ignore this message")
    message.isCommand("list", "список") -> listProducts(message, bot)
    message.isCommand("save", "сохранить") -> saveProducts(message, bot)
    message.isCommand("delete", "удалить") -> deleteProducts(message, bot)
    else -> handleCalculateMessage(message, bot)
}

private suspend fun handleCalculateMessage(message: Message, bot: TelegramBot) {
    val sent = message(composeSumReply(message)).sendAsync(message.chat, bot)
    val sentId = sent.await().getOrNull()?.messageId!!
    transaction {
        addLogger(StdOutSqlLogger)
        Link.new {
            messageId = message.messageId
            chatId = message.chat.id
            replyId = sentId
            createdAt = now().toKotlinLocalDateTime()
        }
    }
}

suspend fun handleEditedMessage(message: Message, bot: TelegramBot) {
    val replyId = transaction {
        addLogger(StdOutSqlLogger)
        Link.find {
            (Links.messageId eq message.messageId) and (Links.chatId eq message.chat.id)
        }.firstOrNull()?.replyId
    } ?: return
    editMessageText(replyId) { composeSumReply(message) }.send(message.chat.id, bot)
}

private fun composeSumReply(message: Message): String {
    val (sum, warnings) = calculateSum(message.text!!, message.from!!.id)
    return "Sum = $sum\n$warnings"
}

private fun Message.isCommand(vararg command: String) =
    command.any { text?.startsWith(it, ignoreCase = true) == true }
