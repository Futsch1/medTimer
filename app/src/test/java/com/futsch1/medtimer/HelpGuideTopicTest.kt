package com.futsch1.medtimer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HelpGuideTopicTest {
    @Test
    fun `guide page URL resolves to its topic`() {
        assertEquals("getting-started", helpTopicFromUrl("https://appassets.androidplatform.net/assets/getting-started.html"))
    }

    @Test
    fun `non-guide page completion has no topic`() {
        assertNull(helpTopicFromUrl("about:blank"))
    }
}
