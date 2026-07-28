package com.futsch1.medtimer.robots

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex

/**
 * Reads the Compose semantics tree: node counts, visual order, text and lazy-list scrolling.
 * Knows nothing about medTimer - every entry point takes the tag to work on.
 */
class SemanticsQueries(private val rule: ComposeTestRule) {

    fun count(tag: String): Int = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    fun exists(tag: String): Boolean = count(tag) > 0

    fun awaitAtLeast(tag: String, minCount: Int, timeoutMillis: Long = DEFAULT_TIMEOUT) {
        rule.waitUntil(timeoutMillis) { count(tag) >= minCount }
    }

    fun awaitExists(tag: String, timeoutMillis: Long = DEFAULT_TIMEOUT) = awaitAtLeast(tag, 1, timeoutMillis)

    /** Reordering a list leaves the semantics tree out of visual order, so sort by position. */
    fun indicesTopToBottom(tag: String): List<Int> =
        rule.onAllNodesWithTag(tag).fetchSemanticsNodes()
            .withIndex().sortedBy { it.value.positionInRoot.y }.map { it.index }

    /**
     * Sibling Text composables share no semantics node, so no single node's text holds a target
     * spanning them; concatenates each tagged node's own subtree instead of searching the window.
     */
    fun textsUnder(tag: String): List<String> =
        rule.onAllNodesWithTag(tag, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .sortedBy { it.positionInRoot.y }
            .map { tagged ->
                buildString {
                    fun visit(node: SemanticsNode) {
                        node.config.getOrNull(SemanticsProperties.Text)?.forEach { append(it.text) }
                        node.children.forEach { visit(it) }
                    }
                    visit(tagged)
                }
            }

    fun scrollTo(listTag: String, index: Int): Boolean = runCatching {
        rule.onNodeWithTag(listTag).performScrollToIndex(index)
        rule.waitForIdle()
    }.isSuccess

    /** A lazy list virtualizes, so an item below the fold has no semantics node until scrolled to. */
    fun allTextsIn(listTag: String, itemTag: String, textTag: String): List<String> {
        val texts = linkedSetOf<String>()
        var index = 0
        repeat(MAX_SCROLLS) {
            texts += textsUnder(textTag)
            val next = index + count(itemTag) - 1
            if (next <= index || !scrollTo(listTag, next)) return texts.toList()
            index = next
        }
        return texts.toList()
    }

    /** Scrolls until a text under [textTag] contains [substring], leaving that item composed. */
    fun scrollUntilTextIn(listTag: String, itemTag: String, textTag: String, substring: String) {
        rule.waitUntil(DEFAULT_TIMEOUT) { count(itemTag) > 0 }
        scrollTo(listTag, 0)
        var index = 0
        repeat(MAX_SCROLLS) {
            if (textsUnder(textTag).any { it.contains(substring) }) return
            val next = index + count(itemTag) - 1
            if (next <= index || !scrollTo(listTag, next)) return
            index = next
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT = 5_000L
        private const val MAX_SCROLLS = 20
    }
}
