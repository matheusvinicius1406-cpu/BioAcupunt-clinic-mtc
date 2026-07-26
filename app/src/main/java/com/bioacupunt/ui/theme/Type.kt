package com.bioacupunt.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Premium-minimalista pairing: a serif display face for headings (editorial,
// "private clinic" feel) over a humanist sans for body/labels (readability at
// small sizes). HeadingFont deliberately stays FontFamily.Serif — a generic
// system family Android always resolves locally (Noto Serif or the OEM
// equivalent) — never a bundled font file or a downloadable Google Font: this
// project already crashed once on a missing R.font.* from a bundled font, and
// there is no device/emulator in this environment to verify a font swap
// renders correctly before shipping it.
val HeadingFont = FontFamily.Serif
val BodyFont = FontFamily.Default
val LabelFont = FontFamily.Default

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = HeadingFont, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 48.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = HeadingFont, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = HeadingFont, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontFamily = HeadingFont, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = LabelFont, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = LabelFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = LabelFont, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp)
)
