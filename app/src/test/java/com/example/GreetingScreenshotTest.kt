package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.BusinessCard
import com.example.ui.components.DigitalBusinessCardView
import com.example.ui.theme.CardMateTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun digital_card_screenshot() {
    val sampleCard = BusinessCard(
      fullName = "Mrinal Kanti Roy",
      jobTitle = "Lead AI Architect",
      company = "CardMate AI Corp",
      phone = "+880 1712-345678",
      email = "mrinal.eee@gmail.com",
      category = "Tech & IT",
      cardLayoutTemplate = "modern_slate"
    )

    composeTestRule.setContent {
      CardMateTheme(darkTheme = true) {
        DigitalBusinessCardView(card = sampleCard)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/card_preview.png")
  }
}
