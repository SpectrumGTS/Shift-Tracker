package dev.spectrumgts.shifttracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography =
  Typography(
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      )
  )

// Warning Dialog Typography Styles
object WarningDialogStyles {
  val titleStyle: TextStyle
    @Composable
    get() = MaterialTheme.typography.titleLarge

  val bodyStyle: TextStyle
    @Composable
    get() = MaterialTheme.typography.bodyMedium
}

val WarningDialogTitleStyle: TextStyle
  @Composable
  get() = WarningDialogStyles.titleStyle

val WarningDialogBodyStyle: TextStyle
  @Composable
  get() = WarningDialogStyles.bodyStyle

