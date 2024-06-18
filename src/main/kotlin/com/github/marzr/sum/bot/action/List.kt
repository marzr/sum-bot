package com.github.marzr.sum.bot.action

import com.github.marzr.sum.bot.db.Product
import com.github.marzr.sum.bot.db.Products
import eu.vendeli.tgbot.TelegramBot
import eu.vendeli.tgbot.api.message.message
import eu.vendeli.tgbot.types.Message
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

suspend fun listProducts(message: Message, bot: TelegramBot) {
    val text = transaction {
        addLogger(StdOutSqlLogger)
        Product.find { Products.userId eq message.from!!.id }.joinToString("\n") { "${it.name} ${it.calories}" }
    }
    message(text).send(message.chat, bot)
}
