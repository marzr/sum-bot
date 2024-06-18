package com.github.marzr.sum.bot.calc

import com.github.marzr.sum.bot.action.calculateSum
import com.github.marzr.sum.bot.action.saveProducts
import com.github.marzr.sum.bot.db.Links
import com.github.marzr.sum.bot.db.Products
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class CalculateTest {

    companion object {
        private val db by lazy {
            Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
            transaction {
                addLogger(StdOutSqlLogger)
                SchemaUtils.create(Links, Products)
            }
        }

        @BeforeAll
        @JvmStatic
        fun before() {
            db
        }
    }

    @Test
    fun `save and use products`() {
        val userId = 1234567L
        val saveMessage = """
            save
            каша 123
            сырок 144
            тортик протеин рекс 160
            Чипсеки лейз 520
        """.trimIndent()
        saveProducts(saveMessage, userId)
        val message = """
            Каша 100 г
            Сырок
            Кола 42*2
            Тортик протеин рекс
            Чипсеки лейз 30 г
        """.trimIndent()
        assertEquals(123 + 144 + 42 * 2 + 160 + 156, calculateSum(message, userId).first)
    }

    @Test
    fun `save with error`() {
        val userId = 1234567L
        val saveMessage = """
            save
            что-то не то...
        """.trimIndent()
        assertThrows(Exception::class.java) { saveProducts(saveMessage, userId) }
    }
}
