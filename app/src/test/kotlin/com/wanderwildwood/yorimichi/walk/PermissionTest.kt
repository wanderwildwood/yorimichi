package com.wanderwildwood.yorimichi.walk

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionTest {

    @Test
    fun `a grant is never a refusal`() {
        assertFalse(refusedForGood(granted = true, reasonBefore = true, reasonAfter = false, atOnce = true, refusedEarlier = true))
    }

    @Test
    fun `a first refusal leaves the button asking`() {
        // Android explains itself after one refusal, so it will ask again.
        assertFalse(refusedForGood(granted = false, reasonBefore = false, reasonAfter = true, atOnce = false, refusedEarlier = false))
    }

    @Test
    fun `a dialog closed with back the first time is not a refusal for good`() {
        assertFalse(refusedForGood(granted = false, reasonBefore = false, reasonAfter = false, atOnce = false, refusedEarlier = false))
    }

    @Test
    fun `a second refusal in the same run is for good`() {
        assertTrue(refusedForGood(granted = false, reasonBefore = true, reasonAfter = false, atOnce = false, refusedEarlier = true))
    }

    @Test
    fun `an answer with no dialog at all is for good`() {
        assertTrue(refusedForGood(granted = false, reasonBefore = false, reasonAfter = false, atOnce = true, refusedEarlier = false))
    }

    @Test
    fun `in a later run an earlier refusal on record is enough, however slow the answer`() {
        assertTrue(refusedForGood(granted = false, reasonBefore = false, reasonAfter = false, atOnce = false, refusedEarlier = true))
    }
}
