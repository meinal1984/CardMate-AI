package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.CardAiHistoryManager
import com.example.ai.ChatMessage
import com.example.ai.ChatRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CardAiHistoryManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        CardAiHistoryManager.clearHistoryForCard(context, 101L)
        CardAiHistoryManager.clearHistoryForCard(context, 202L)
        CardAiHistoryManager.clearHistoryForCard(context, 0L)
    }

    @Test
    fun testSaveAndLoadHistoryPerCard() {
        val messagesCard1 = listOf(
            ChatMessage(
                id = "msg1",
                role = ChatRole.USER,
                text = "Hello for Card 1"
            ),
            ChatMessage(
                id = "msg2",
                role = ChatRole.MODEL,
                text = "Welcome to Card 1 Assistant"
            )
        )

        CardAiHistoryManager.saveHistoryForCard(context, 101L, messagesCard1)

        val loadedCard1 = CardAiHistoryManager.getHistoryForCard(context, 101L)
        assertEquals(2, loadedCard1.size)
        assertEquals("Hello for Card 1", loadedCard1[0].text)
        assertEquals("Welcome to Card 1 Assistant", loadedCard1[1].text)

        // Card 2 should be empty
        val loadedCard2 = CardAiHistoryManager.getHistoryForCard(context, 202L)
        assertTrue(loadedCard2.isEmpty())
    }

    @Test
    fun testClearHistoryPerCard() {
        val messages = listOf(
            ChatMessage(
                id = "msg1",
                role = ChatRole.USER,
                text = "Test prompt"
            )
        )
        CardAiHistoryManager.saveHistoryForCard(context, 101L, messages)
        CardAiHistoryManager.clearHistoryForCard(context, 101L)

        val reloaded = CardAiHistoryManager.getHistoryForCard(context, 101L)
        assertTrue(reloaded.isEmpty())
    }
}
