package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
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
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(CyberMidnight)
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberSlate)
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Text(
                text = "LEDGER TEST ENVIRONMENT",
                style = MaterialTheme.typography.titleSmall.copy(
                  color = CyberCyan,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.5.sp
                )
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "SYSTEM ACTIVE",
                style = MaterialTheme.typography.headlineMedium.copy(
                  color = CyberEmerald,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                )
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Cyberpunk theme rendering check successful. Testing local SQLite Flow connection on modern Compose canvas.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
              )
            }
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
