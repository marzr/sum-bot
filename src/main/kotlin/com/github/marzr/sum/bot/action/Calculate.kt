package com.github.marzr.sum.bot.action

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.marzr.sum.bot.db.Product
import com.github.marzr.sum.bot.db.Products
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.roundToInt

fun calculateSum(text: String, userId: Long): Pair<Int, String> {
    val warnings = mutableListOf<String>()
    return text.split('\n').sumOf {
        if (it.isDbProduct())
            calculateDbProduct(it, userId).onLeft { warnings.add(it) }.getOrNull() ?: 0.0
        else calculate(it)
    }.roundToInt() to warnings.joinToString("\n").let { if (it.isEmpty()) "" else "warn: $it" }
}

/**
 * Example records:
 * "сырок" => 144 ккал
 * "помидор 200 г" => 25*2 = 50 ккал
 */
private fun calculateDbProduct(record: String, userId: Long): Either<String, Double> {
    val (productName, productType) = when {
        record.endsWith(" г") -> record.dropLast(2).substringBeforeLast(' ') to ProductType.WEIGHT
        else -> record to ProductType.PIECE
    }
    val product = transaction {
        addLogger(StdOutSqlLogger)
        Product.find {
            (Products.userId eq userId) and (Products.name eq productName.lowercase())
        }.firstOrNull()
    } ?: return "product [$productName] is not found".left()

    return when (productType) {
        ProductType.WEIGHT -> product.calories * record.dropLast(2).substringAfterLast(' ').toDoubleOrZero() / 100
        ProductType.PIECE -> product.calories.toDouble()
    }.right()
}

private enum class ProductType {
    WEIGHT, PIECE
}

/**
 * Example records:
 * "булка 400*0.6" => 240 ккал
 * "Чай 70*3" => 210 ккал
 */
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

private fun String.isDbProduct() = endsWith(" г") || !substringAfterLast(' ').isExpression()

private fun String.isExpression() = toDoubleOrNull() != null ||
        (substringBefore('*').toDoubleOrNull() != null && substringAfter('*').toDoubleOrNull() != null)
