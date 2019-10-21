package com.zachpepsin.githubapidemo

import java.util.*

class Repositories {
    /**
     * An array of items
     */
    val items: MutableList<RepositoryItem> = ArrayList()

    /**
     * A map of items, by ID
     */
    private val itemMap: MutableMap<String, RepositoryItem> = HashMap()

    fun addItem(id: String, content: String, details: String) {
        val item = RepositoryItem(id, content, details)
        items.add(item)
        itemMap[item.id] = item
    }

    /**
     * Data class
     */
    data class RepositoryItem(
        val id: String,
        val content: String,
        val details: String
    ) {
        override fun toString(): String = content
    }
}