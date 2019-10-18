package com.zachpepsin.githubapidemo

import java.util.*

class Issues {
    /**
     * An array of sample (dummy) items.
     */
    val ITEMS: MutableList<IssueItem> = ArrayList()


    /**
     * A map of sample (dummy) items, by ID.
     */
    val ITEM_MAP: MutableMap<String, IssueItem> = HashMap()


    private val COUNT = 25

    /*
    init {
        // Add some sample items.
        for (i in 1..COUNT) {
            addItem(createDummyItem(i))
        }
    }
     */

    private fun addItem(item: IssueItem) {
        ITEMS.add(item)
        ITEM_MAP.put(item.id, item)
    }

    fun addItem(id: String, number: String, content: String, details: String, state: String) {
        val item = IssueItem(id, number, content, details, state)
        ITEMS.add(item)
        ITEM_MAP.put(item.id, item)
    }

    private fun createDummyItem(position: Int): IssueItem {
        return IssueItem(position.toString(), position.toString(), "Item " + position, makeDetails(position), "open")
    }

    private fun makeDetails(position: Int): String {
        val builder = StringBuilder()
        builder.append("Details about Item: ").append(position)
        for (i in 0..position - 1) {
            builder.append("\nMore details information here.")
        }
        return builder.toString()
    }

    /**
     * A dummy item representing a piece of content.
     */
    data class IssueItem(
        val id: String,
        val number: String,
        val content: String,
        val details: String,
        val state: String
    ) {
        override fun toString(): String = content
    }
}