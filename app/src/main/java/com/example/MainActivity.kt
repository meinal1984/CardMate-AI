package com.example

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.nfc.NfcCardService
import com.example.nfc.NfcOperationMode
import com.example.ui.CardMateApp
import com.example.ui.viewmodel.CardViewModel
import com.example.ui.viewmodel.NavigationTab
import com.example.widget.CardMateWidgetProvider

class MainActivity : ComponentActivity() {

    private val viewModel: CardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.example.sync.AuthManager.init(this)

        handleAppIntent(intent)

        setContent {
            CardMateApp(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshNfcStatus(this)

        // Enable real-time NFC Reader Mode with Tag callback
        NfcCardService.enableReaderMode(this) { tag: Tag ->
            viewModel.processDiscoveredTag(
                tag = tag,
                packageName = packageName
            ) { card ->
                Toast.makeText(
                    this@MainActivity,
                    "NFC Card Read: ${card.fullName}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        NfcCardService.disableReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAppIntent(intent)
    }

    private fun handleAppIntent(intent: Intent?) {
        if (intent == null) return
        handleWidgetIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent) {
        val action = intent.action
        if (action == CardMateWidgetProvider.ACTION_QUICK_SCAN) {
            viewModel.setTab(NavigationTab.SCANNER)
        } else if (action == CardMateWidgetProvider.ACTION_OPEN_CARD || intent.hasExtra(CardMateWidgetProvider.EXTRA_CARD_ID)) {
            val cardId = intent.getLongExtra(CardMateWidgetProvider.EXTRA_CARD_ID, -1L)
            if (cardId > 0) {
                viewModel.openCardById(cardId)
            }
        }
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        viewModel.handleNfcIntent(intent) { card ->
            viewModel.setTab(NavigationTab.CARDS)
            Toast.makeText(
                this@MainActivity,
                "NFC Business Card Auto-Imported: ${card.fullName}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

