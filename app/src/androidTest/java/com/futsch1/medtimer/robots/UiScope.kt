package com.futsch1.medtimer.robots

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.espresso.Espresso.onIdle

/**
 * One subtree of the semantics tree, addressed by the matcher that finds its root.
 *
 * Selectors name what they are after the way a screen reader does — content description, label,
 * visible text — and a scope says where to look for it, so a name that repeats elsewhere in the app
 * cannot be matched by accident. Every Compose query in the suite goes through one of these; this is
 * the only file allowed to reach for the global `onNode`/`onAllNodes` finders.
 */
class UiScope internal constructor(
    private val rule: ComposeTestRule,
    private val anchor: SemanticsMatcher,
) {

    /** A scope nested inside this one, e.g. the event list within the Overview screen. */
    fun scope(matcher: SemanticsMatcher): UiScope = UiScope(rule, inScope(matcher))

    fun scope(tag: String): UiScope = scope(hasTestTag(tag))

    /** The scope's own root node. */
    fun self(): SemanticsNodeInteraction = rule.onNode(anchor)

    fun node(matcher: SemanticsMatcher): SemanticsNodeInteraction = rule.onNode(inScope(matcher))

    fun nodes(matcher: SemanticsMatcher, useUnmergedTree: Boolean = false): SemanticsNodeInteractionCollection =
        rule.onAllNodes(inScope(matcher), useUnmergedTree)

    fun count(matcher: SemanticsMatcher): Int = nodes(matcher).fetchSemanticsNodes().size

    fun exists(matcher: SemanticsMatcher): Boolean = count(matcher) > 0

    fun await(timeoutMillis: Long = DEFAULT_TIMEOUT, condition: () -> Boolean) =
        rule.waitUntil(timeoutMillis, condition)

    fun awaitAtLeast(matcher: SemanticsMatcher, minCount: Int, timeoutMillis: Long = DEFAULT_TIMEOUT) =
        await(timeoutMillis) { count(matcher) >= minCount }

    fun awaitExists(matcher: SemanticsMatcher, timeoutMillis: Long = DEFAULT_TIMEOUT) =
        awaitAtLeast(matcher, 1, timeoutMillis)

    fun awaitGone(timeoutMillis: Long = DEFAULT_TIMEOUT) =
        await(timeoutMillis) { rule.onAllNodes(anchor).fetchSemanticsNodes().isEmpty() }

    /**
     * Waits for the target before tapping it: menu entries and list items animate in, and the arc
     * action menu staggers its reveal on a real-time delay that no idling resource tracks.
     */
    fun click(matcher: SemanticsMatcher) {
        awaitExists(matcher)
        node(matcher).performClick()
        settle()
    }

    fun clickSelf() {
        self().performClick()
        settle()
    }

    fun assertDisplayed(matcher: SemanticsMatcher) {
        awaitExists(matcher)
        node(matcher).assertIsDisplayed()
    }

    /**
     * Asserts the scope itself rendered before checking for the absence below it — otherwise a scope
     * that never appeared would make every absence check pass for the wrong reason.
     */
    fun assertAbsent(matcher: SemanticsMatcher) {
        self().assertExists()
        node(matcher).assertDoesNotExist()
    }

    /** Reordering a list leaves the semantics tree out of visual order, so sort by position. */
    fun indicesTopToBottom(matcher: SemanticsMatcher): List<Int> =
        nodes(matcher).fetchSemanticsNodes()
            .withIndex().sortedBy { it.value.positionInRoot.y }.map { it.index }

    fun nodeAt(matcher: SemanticsMatcher, index: Int): SemanticsNodeInteraction =
        nodes(matcher)[indicesTopToBottom(matcher)[index]]

    fun boundsTopToBottom(matcher: SemanticsMatcher, useUnmergedTree: Boolean = false): List<Pair<Int, Float>> =
        nodes(matcher, useUnmergedTree).fetchSemanticsNodes()
            .withIndex().map { it.index to it.value.boundsInRoot.center.y }.sortedBy { it.second }

    /**
     * Sibling Text composables share no semantics node, so no single node's text holds a target
     * spanning them; concatenates each matched node's own subtree instead.
     */
    fun textsUnder(matcher: SemanticsMatcher): List<String> =
        nodes(matcher, useUnmergedTree = true).fetchSemanticsNodes()
            .sortedBy { it.positionInRoot.y }
            .map { matched -> buildString { appendTexts(matched) } }

    /** Scrolls this scope's own node, which has to be the lazy list. */
    fun scrollToIndex(index: Int): Boolean = runCatching {
        self().performScrollToIndex(index)
        rule.waitForIdle()
    }.isSuccess

    /**
     * Every text under [textMatcher] in this list, scrolling through it. A lazy list virtualizes, so
     * an item below the fold has no semantics node until it has been scrolled to.
     */
    fun allTexts(itemMatcher: SemanticsMatcher, textMatcher: SemanticsMatcher): List<String> {
        val texts = linkedSetOf<String>()
        var index = 0
        scrollToIndex(0)
        repeat(MAX_SCROLLS) {
            texts += textsUnder(textMatcher)
            val next = index + count(itemMatcher) - 1
            if (next <= index || !scrollToIndex(next)) return texts.toList()
            index = next
        }
        return texts.toList()
    }

    /** Scrolls until a text under [textMatcher] contains [substring], leaving that item composed. */
    fun scrollUntilText(itemMatcher: SemanticsMatcher, textMatcher: SemanticsMatcher, substring: String) {
        await { count(itemMatcher) > 0 }
        scrollToIndex(0)
        var index = 0
        repeat(MAX_SCROLLS) {
            if (textsUnder(textMatcher).any { it.contains(substring) }) return
            val next = index + count(itemMatcher) - 1
            if (next <= index || !scrollToIndex(next)) return
            index = next
        }
    }

    /**
     * Registered Espresso IdlingResources (e.g. async generateTestData) need an explicit wait since
     * later steps often drive UiAutomator directly, bypassing Espresso's own idle check.
     */
    fun settle() {
        rule.waitForIdle()
        onIdle()
    }

    fun waitForIdle() = rule.waitForIdle()

    private fun inScope(matcher: SemanticsMatcher) = matcher and hasAnyAncestor(anchor)

    companion object {
        private const val DEFAULT_TIMEOUT = 15_000L
        private const val MAX_SCROLLS = 20
    }
}

private fun StringBuilder.appendTexts(node: SemanticsNode) {
    node.config.getOrNull(SemanticsProperties.Text)?.forEach { append(it.text) }
    node.children.forEach { appendTexts(it) }
}
