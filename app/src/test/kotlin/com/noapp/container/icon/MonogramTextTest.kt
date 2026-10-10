package com.noapp.container.icon

import org.junit.Assert.assertEquals
import org.junit.Test

class MonogramTextTest {
    @Test
    fun `the monogram is the first character as seen, not half of an emoji`() {
        assertEquals("📚", monogramText("📚 Книги"))
        assertEquals("К", monogramText("книги"))
        assertEquals("W", monogramText("wiki"))
        assertEquals("?", monogramText(""))
    }
}
