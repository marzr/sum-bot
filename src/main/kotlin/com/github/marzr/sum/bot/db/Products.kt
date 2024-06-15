package com.github.marzr.sum.bot.db

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Products : LongIdTable() {
    val userId = long("user_id").index()
    val name = varchar("name", 50)
    val calories = integer("calories")
    val createdAt = datetime("created_at")
}

class Product(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Product>(Products)

    var userId by Products.userId
    var name by Products.name
    var calories by Products.calories
    var createdAt by Products.createdAt
}
