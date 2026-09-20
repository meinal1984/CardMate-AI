package com.example

import com.example.ui.components.ContactQrData
import com.example.ui.components.QrCenterBadge
import com.example.ui.components.QrCodeHelper
import com.example.ui.components.QrContactFormat
import com.example.ui.components.QrThemeColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DynamicQrCodeHelperTest {

    private val sampleContact = ContactQrData(
        fullName = "Rahim Ahmed",
        jobTitle = "Lead Architect",
        company = "Apex Technologies",
        phone = "+8801712345678",
        secondaryPhone = "+8801812345678",
        email = "rahim@apex.com",
        website = "https://apex.com",
        address = "Gulshan-2, Dhaka",
        notes = "VIP Business Partner"
    )

    @Test
    fun testBuildContactPayload_vCard3() {
        val payload = QrCodeHelper.buildContactPayload(sampleContact, QrContactFormat.VCARD_3_0)
        assertTrue(payload.startsWith("BEGIN:VCARD"))
        assertTrue(payload.contains("VERSION:3.0"))
        assertTrue(payload.contains("FN:Rahim Ahmed"))
        assertTrue(payload.contains("TITLE:Lead Architect"))
        assertTrue(payload.contains("ORG:Apex Technologies"))
        assertTrue(payload.contains("TEL;TYPE=WORK,VOICE:+8801712345678"))
        assertTrue(payload.contains("EMAIL;TYPE=WORK,INTERNET:rahim@apex.com"))
        assertTrue(payload.contains("URL:https://apex.com"))
        assertTrue(payload.trimEnd().endsWith("END:VCARD"))
    }

    @Test
    fun testBuildContactPayload_meCard() {
        val payload = QrCodeHelper.buildContactPayload(sampleContact, QrContactFormat.MECARD)
        assertTrue(payload.startsWith("MECARD:"))
        assertTrue(payload.contains("N:Rahim Ahmed;"))
        assertTrue(payload.contains("TEL:+8801712345678;"))
        assertTrue(payload.contains("EMAIL:rahim@apex.com;"))
        assertTrue(payload.contains("ORG:Apex Technologies;"))
        assertTrue(payload.contains("TIL:Lead Architect;"))
        assertTrue(payload.contains("URL:https\\://apex.com;"))
        assertTrue(payload.endsWith(";;"))
    }

    @Test
    fun testBuildContactPayload_plainText() {
        val payload = QrCodeHelper.buildContactPayload(sampleContact, QrContactFormat.PLAIN_TEXT)
        assertTrue(payload.contains("Contact: Rahim Ahmed"))
        assertTrue(payload.contains("Title: Lead Architect"))
        assertTrue(payload.contains("Company: Apex Technologies"))
        assertTrue(payload.contains("Phone: +8801712345678"))
    }

    @Test
    fun testGenerateDynamicQr_rendersBitmapWithZxing() {
        val bitmap = QrCodeHelper.generateDynamicContactQr(
            data = sampleContact,
            format = QrContactFormat.VCARD_3_0,
            sizePx = 400,
            themeColor = QrThemeColor.CARDMATE_TEAL,
            centerBadge = QrCenterBadge.USER_INITIALS
        )
        assertNotNull(bitmap)
        assertEquals(400, bitmap!!.width)
        assertEquals(400, bitmap.height)
    }

    @Test
    fun testRenderSharableQrCard_generatesPresentationCard() {
        val baseQr = QrCodeHelper.generateDynamicContactQr(
            data = sampleContact,
            format = QrContactFormat.VCARD_3_0,
            sizePx = 300,
            themeColor = QrThemeColor.ELECTRIC_CYAN,
            centerBadge = QrCenterBadge.NONE
        )
        assertNotNull(baseQr)

        val cardBitmap = QrCodeHelper.renderSharableQrCard(
            data = sampleContact,
            qrBitmap = baseQr!!,
            themeColor = QrThemeColor.ELECTRIC_CYAN
        )
        assertNotNull(cardBitmap)
        assertEquals(1000, cardBitmap.width)
        assertEquals(1400, cardBitmap.height)
    }
}
