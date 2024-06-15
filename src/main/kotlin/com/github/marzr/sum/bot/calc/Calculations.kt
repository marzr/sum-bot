package com.github.marzr.sum.bot.calc

import com.github.marzr.sum.bot.db.Product
import com.github.marzr.sum.bot.db.Products
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.roundToInt

object Calculations {
    /**
     * Example records:
     * ". сырок" => calculateDbProduct("сырок") => 144 ккал
     * ". Сырок" => calculateDbProduct("Сырок") => 144 ккал
     * "булка 400*0.6" => calculate("булка 400*0.6") => 240 ккал
     * "Чай 70*3" => calculate("Чай 70*3") => 120 ккал
     */
    fun calculateSum(text: String, userId: Long): Int {
        val warnings = mutableListOf<String>()
        return text.split('\n').sumOf {
            when {
                it.isDbProduct() -> calculateDbProduct(it, userId).onSecond {
                    it?.let { warnings.add(it) }
                }.first

                else -> calculate(it)
            }
        }.roundToInt()
    }

    private fun <A, B> Pair<A, B>.onSecond(function: (B) -> Unit): Pair<A, B> {
        function(this.second)
        return this
    }

    /**
     * Example records:
     * ". сырок" => 144 ккал
     * ". помидор 120 г" => 25*1.2 ккал
     */
    private fun calculateDbProduct(record: String, userId: Long): Pair<Double, String?> {
        val (recordClean, productType) = record.drop(2).let {
            when {
                record.endsWith(" г") -> it.dropLast(2) to ProductType.WEIGHT
                else -> it to ProductType.PIECE
            }
        }
        // if (нет пробела) вернуть ошибку
        val productName = recordClean.substringBeforeLast(' ')
        val product = transaction {
            addLogger(StdOutSqlLogger)
            Product.find {
                (Products.userId eq userId) and (Products.name eq productName)
            }.firstOrNull()
        } ?: return 0.0 to "product [$productName] is not found"

        return when (productType) {
            ProductType.WEIGHT -> product.calories * recordClean.substringAfter(' ').toDoubleOrZero() / 100
            ProductType.PIECE -> product.calories.toDouble()
        } to null
    }

    enum class ProductType {
        WEIGHT, PIECE
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

    private fun String.toDoubleOrZero(): Double = this.toDoubleOrNull() ?: 0.0

    private fun String.isDbProduct() = startsWith('.')
}
