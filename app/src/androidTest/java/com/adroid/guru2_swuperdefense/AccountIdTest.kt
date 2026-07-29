package com.adroid.guru2_swuperdefense

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountIdTest {
    @Test
    fun plainIdIsConvertedToVirtualFirebaseEmail() {
        assertEquals(
            "student01@swuperdepense.kr",
            AccountId.toFirebaseEmail(" Student01 ")
        )
        assertEquals(
            "student01",
            AccountId.toDisplayId("student01@swuperdepense.kr")
        )
    }

    @Test
    fun fullEmailIsNotAcceptedAsId() {
        assertFalse(AccountId.isValidInput("student@example.com"))
    }

    @Test
    fun validatesIdCharactersAndLength() {
        assertTrue(AccountId.isValidInput("student_01"))
        assertFalse(AccountId.isValidInput("ab"))
        assertFalse(AccountId.isValidInput("student 01"))
        assertFalse(AccountId.isValidInput("student!"))
    }
}
