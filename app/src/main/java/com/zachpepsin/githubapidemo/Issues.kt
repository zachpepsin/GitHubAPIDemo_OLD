package com.zachpepsin.githubapidemo

import java.util.*

class Issues {
    /**
     * An array of items
     */
    val items: MutableList<IssueItem> = ArrayList()

    /**
     * A map of items, by ID
     */
    private val itemMap: MutableMap<String, IssueItem> = HashMap()

    fun addItem(id: String, number: String, title: String, body: String, state: String) {
        val item = IssueItem(id, number, title, body, state)
        items.add(item)
        itemMap[item.id] = item
    }

    /**
     * Data class
     */
    data class IssueItem(
        val id: String,
        val number: String,
        val title: String,
        val body: String,
        val state: String
    ) {
        override fun toString(): String = title
    }
}