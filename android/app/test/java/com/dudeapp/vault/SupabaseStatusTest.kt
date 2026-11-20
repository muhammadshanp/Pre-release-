package com.dudeapp.vault

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The server answers in codes; these tests pin the mapping a person actually reads. */
class SupabaseStatusTest {

    @Test
    fun readsAFreeName() {
        assertEquals(
            Supabase.NameCheck.Free,
            Supabase.parseCheck(JSONObject("""{"available":true,"reason":"ok"}""")),
        )
    }

    @Test
    fun readsATakenName() {
        assertEquals(
            Supabase.NameCheck.Taken,
            Supabase.parseCheck(JSONObject("""{"available":false,"reason":"taken"}""")),
        )
    }

    @Test
    fun readsARejectedName() {
        assertEquals(
            Supabase.NameCheck.Rejected("reserved"),
            Supabase.parseCheck(JSONObject("""{"available":false,"reason":"reserved"}""")),
        )
    }

    @Test
    fun successStatusesHaveNothingToSay() {
        assertEquals("", Supabase.claimProblem("ok"))
        assertEquals("", Supabase.claimProblem("already_yours"))
    }

    @Test
    fun everyFailureExplainsItself() {
        val statuses = listOf(
            "taken", "reserved", "short", "long", "chars", "empty", "bad_device", "whatever",
        )
        for (status in statuses) {
            val text = Supabase.claimProblem(status)
            assertTrue("$status should explain itself", text.length > 20)
        }
        // Offline is the one case with no status at all.
        assertTrue(Supabase.claimProblem(null).length > 20)
    }

    @Test
    fun takenIsNotReportedAsSuccess() {
        assertFalse(Supabase.claimProblem("taken").isEmpty())
    }
}
