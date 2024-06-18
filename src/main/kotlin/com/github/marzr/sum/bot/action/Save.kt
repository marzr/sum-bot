package com.github.marzr.sum.bot.action

import com.github.marzr.sum.bot.db.Product
import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.Message
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime.now

suspend fun saveProducts(message: Message, bot: TelegramBot) {
    runCatching {
        val reply = saveProducts(message.text!!, message.from!!.id)
        // сообщение подождите
        message(reply).send(message.chat, bot)
    }.onFailure {
        message("error").send(message.chat, bot)
    }
}

fun saveProducts(text: String, user: Long): String {
    val now = now().toKotlinLocalDateTime()
    val warnings = mutableListOf<String>()

    text.split('\n').drop(1).forEach {
        val productName = it.substringBeforeLast(' ')
        val calories = it.substringAfterLast(' ').toInt()
        try {
            transaction {
                addLogger(StdOutSqlLogger)
                Product.new {
                    userId = user
                    name = productName.lowercase()
                    this.calories = calories
                    createdAt = now
                }
            }
        } catch (e: ExposedSQLException) {
            if (e.message?.contains("duplicate key value violates unique constraint \"products_name_unique\"") == true)
                warnings.add("product [$productName] already exists")
            else return "system error"
        } catch (e: Exception) {
            return "unknown error"
        }
    }
    return if (warnings.isNotEmpty()) "accepted\nwarn: ${warnings.joinToString("\n")}"
    else "accepted"
}
