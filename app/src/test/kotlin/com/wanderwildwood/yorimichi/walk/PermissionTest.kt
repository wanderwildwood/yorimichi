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

    @Test
    fun `a first refusal is not kept on record, so Back after Ask every time still asks again`() {
        // A first refusal: Android shows its reason after it, so it is not for good, and the
        // view model records only a refusal for good.
        val first = refusedForGood(granted = false, reasonBefore = false, reasonAfter = true, atOnce = false, refusedEarlier = false)
        assertFalse(first)
        // Set back to "Ask every time" in the system settings, the reason is gone; the dialog
        // is then closed with Back, slowly.
        assertFalse(refusedForGood(granted = false, reasonBefore = false, reasonAfter = false, atOnce = false, refusedEarlier = first))
    }

    @Test
    fun `a refusal with the reason showing again is not for good, even with one on record`() {
        // After a reset, a refusal Android will ask about again; this answer takes the record off.
        assertFalse(refusedForGood(granted = false, reasonBefore = false, reasonAfter = true, atOnce = false, refusedEarlier = true))
    }
}
