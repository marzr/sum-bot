package com.github.marzr.sum.bot.action

import com.github.marzr.sum.bot.db.Products
import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.Message
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun deleteProducts(message: Message, bot: TelegramBot) {
    runCatching {
        val reply = deleteProducts(message.text!!, message.from!!.id)
        message(reply).send(message.chat, bot)
    }.onFailure {
        message("error").send(message.chat, bot)
    }
}

private fun deleteProducts(text: String, user: Long): String {
    val sum = text.split('\n').drop(1).sumOf { productName ->
        try {
            transaction {
                addLogger(StdOutSqlLogger)
                Products.deleteWhere {
                    (userId eq user) and (name eq productName.lowercase())
                }
            }
        } catch (e: Exception) {
            println(e)
            return "error"
        }
    }
    return "deleted: $sum"
}
