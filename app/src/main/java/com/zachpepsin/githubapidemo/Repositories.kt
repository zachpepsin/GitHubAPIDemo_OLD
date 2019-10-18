package com.zachpepsin.githubapidemo

import java.util.*

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 */
class Repositories {

    /**
     * An array of sample (dummy) items.
     */
    val ITEMS: MutableList<RepositoryItem> = ArrayList()


    /**
     * A map of sample (dummy) items, by ID.
     */
    val ITEM_MAP: MutableMap<String, RepositoryItem> = HashMap()


    private val COUNT = 25

    /*
    init {
        // Add some sample items.
        for (i in 1..COUNT) {
            addItem(createDummyItem(i))
        }
    }
     */

    private fun addItem(item: RepositoryItem) {
        ITEMS.add(item)
        ITEM_MAP.put(item.id, item)
    }

    fun addItem(id: String, content: String, details: String) {
        val item = RepositoryItem(id, content, details)
        ITEMS.add(item)
        ITEM_MAP.put(item.id, item)
    }

    private fun createDummyItem(position: Int): RepositoryItem {
        return RepositoryItem(position.toString(), "Item " + position, makeDetails(position))
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
    data class RepositoryItem(val id: String, val content: String, val details: String) {
        override fun toString(): String = content
    }

}