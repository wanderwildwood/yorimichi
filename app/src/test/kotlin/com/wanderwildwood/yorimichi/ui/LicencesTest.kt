package com.wanderwildwood.yorimichi.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LicencesTest {

    @Test
    fun `the built with list is not empty and every entry has a web link`() {
        assertTrue(LIBRARIES.isNotEmpty())
        LIBRARIES.forEach {
            assertTrue(it.name, it.link.startsWith("https://"))
            assertTrue(it.name, it.licence.isNotBlank())
        }
    }

    @Test
    fun `the source link is the public repository`() {
        assertEquals("https://github.com/wanderwildwood/yorimichi", SOURCE_URL)
    }

    @Test
    fun `paragraphs are joined into rows without losing a word`() {
        val rows = licenceParagraphs("one two\n  three\n\n\nfour\r\nfive\n")
        assertEquals(listOf("one two three", "four five"), rows)
    }

    @Test
    fun `the bundled licence texts are there and read as paragraphs`() {
        val raw = File("src/main/res/raw")
        listOf("gpl_3_0.txt", "apache_2_0.txt", "ofl_1_1.txt").forEach {
            assertTrue(it, licenceParagraphs(File(raw, it).readText()).size > 3)
        }
        assertTrue(File(raw, "gpl_3_0.txt").readText().contains("GNU GENERAL PUBLIC LICENSE"))
    }
}
