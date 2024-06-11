package com.github.marzr.sum.bot

import com.github.marzr.sum.bot.db.Link
import com.github.marzr.sum.bot.db.Links
import com.github.marzr.sum.bot.db.Product
import com.github.marzr.sum.bot.db.Products
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

private suspend fun handleMessage(message: Message, bot: TelegramBot) {
    if (message.from == null) return
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

/**
 * Example records:
 * ". сырок" => calculateDbProduct("сырок") => 144 ккал
 * ". Сырок" => calculateDbProduct("Сырок") => 144 ккал
 * "булка 400*0.6" => calculate("булка 400*0.6") => 240 ккал
 * "Чай 70*3" => calculate("Чай 70*3") => 120 ккал
 */
private fun calculateSum(text: String, userId: Long): Int {
    val warnings = mutableListOf<String>()
    return text.split('\n').asSequence().map {
        when {
            it.isDbProduct() -> calculateDbProduct(it.drop(2), userId).onSecond {
                warnings.add(it)
            }.first

            else -> calculate(it)
        }
    }.sum().roundToInt()
}

private fun <A, B> Pair<A, B>.onSecond(function: (B) -> Unit): Pair<A, B> {
    function(this.second)
    return this
}

/**
 * Example records:
 * "сырок" => 144 ккал
 * "помидор 120 г" => 25*1.2 ккал
 */
private fun calculateDbProduct(record: String, userId: Long): Pair<Double, String> {
    if (record.indexOf(' ') == -1) return 0.0 to "wrong line format: '. $record'"
    val productName = record.substringBefore(' ')
    val product = transaction {
        Product.find {
            (Products.userId eq userId) and (Products.name eq productName)
        }.firstOrNull()
    } ?: return 0.0 to "product [$productName] is not found"

    TODO()
}

private fun calculate(record: String): Double {
    val expression = record.lastIndexOf(' ').let { spaceIndex ->
        if (spaceIndex < 0) return 0.0
        else record.drop(spaceIndex + 1).replace(',', '.')
    }

    return expression.lastIndexOf('*').let { starIndex ->
        if (starIndex < 0)
            expression.toDoubleOrZero()
        else
            expression.take(starIndex).toDoubleOrZero() * expression.drop(starIndex + 1).toDoubleOrZero()
    }
}

fun String.toDoubleOrZero(): Double = this.toDoubleOrNull() ?: 0.0

fun String.isDbProduct() = startsWith('.')
