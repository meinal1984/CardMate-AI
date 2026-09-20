package com.example

import com.example.data.model.BusinessCard
import com.example.util.PhoneNumberUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberHandlingTest {

    @Test
    fun testSplitPhoneNumbers_commaSeparated() {
        val raw = "+880 1711-223344, +880 1819-556677"
        val result = PhoneNumberUtils.splitPhoneNumbers(raw)
        assertEquals(2, result.size)
        assertEquals("+880 1711-223344", result[0])
        assertEquals("+880 1819-556677", result[1])
    }

    @Test
    fun testSplitPhoneNumbers_slashAndNewlineSeparated() {
        val raw = "01711111111 / 01822222222\n01933333333"
        val result = PhoneNumberUtils.splitPhoneNumbers(raw)
        assertEquals(3, result.size)
        assertEquals("01711111111", result[0])
        assertEquals("01822222222", result[1])
        assertEquals("01933333333", result[2])
    }

    @Test
    fun testSplitPhoneNumbers_banglaSeparatorsAndDigits() {
        val raw = "০১৭১১২২৩৩৪৪ অথবা ০১৮৫৫৬৬৭৭৮৮"
        val result = PhoneNumberUtils.splitPhoneNumbers(raw)
        assertEquals(2, result.size)
        assertEquals("০১৭১১২২৩৩৪৪", result[0])
        assertEquals("০১৮৫৫৬৬৭৭৮৮", result[1])
    }

    @Test
    fun testHandlePhoneListEdit_pastingMultipleNumbersSplitsIntoFields() {
        val current = listOf("01711111111", "01822222222")
        // User pastes two numbers into index 1
        val updated = PhoneNumberUtils.handlePhoneListEdit(
            currentList = current,
            index = 1,
            newVal = "01855555555, 01966666666"
        )
        assertEquals(3, updated.size)
        assertEquals("01711111111", updated[0])
        assertEquals("01855555555", updated[1])
        assertEquals("01966666666", updated[2])
    }

    @Test
    fun testBusinessCard_resolvesMultipleSecondaryPhonesSeparately() {
        val card = BusinessCard(
            fullName = "Rahim Ahmed",
            phone = "+880 1711-111111",
            secondaryPhone = "+880 1822-222222, +880 1933-333333",
            socialLinks = "বিকল্প ফোন ৩: +880 1644-444444"
        )

        val secondaryList = card.getSecondaryPhoneNumbers()
        assertTrue(secondaryList.size >= 2)
        assertEquals("+880 1822-222222", secondaryList[0])
        assertEquals("+880 1933-333333", secondaryList[1])
        
        val allList = card.getAllPhoneNumbers()
        assertEquals("+880 1711-111111", allList[0])
    }
}
