package com.gamer.calibration.modules.ads

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.gamer.calibration.BuildConfig
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography
import com.gamer.calibration.ui.theme.TelemetryFonts
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

object AdmobConfig {
    // Official Google Mobile Ads Native Advanced test unit ID for safe development testing
    const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

    /**
     * Resolves the appropriate Ad Unit ID.
     * Always uses Google's test ad unit in debug builds to protect your AdMob account from self-impressions.
     */
    fun getNativeAdUnitId(): String {
        return if (BuildConfig.DEBUG) {
            TEST_NATIVE_AD_UNIT_ID
        } else {
            BuildConfig.ADMOB_NATIVE_AD_UNIT_ID
        }
    }
}

/**
 * Sleek Bento Native Ad Card.
 * Adheres strictly to Google AdMob Native Ad policies (headline, body, call-to-action,
 * icon, and attribution registered with NativeAdView) while seamlessly blending into
 * Caliber's OLED cyber aesthetic.
 */
@Composable
fun BentoNativeAdCard(
    onOpenPreferences: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var adLoadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val adUnitId = AdmobConfig.getNativeAdUnitId()
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { loadedAd ->
                nativeAd = loadedAd
                isLoading = false
                adLoadFailed = false
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    adLoadFailed = true
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    DisposableEffect(Unit) {
        onDispose {
            nativeAd?.destroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MinimalGlassColors.CardSurface)
            .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Bar: [ SPONSORED ] badge + Dismiss/Options trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MinimalGlassColors.PrimaryBlueLight)
                            .border(1.dp, MinimalGlassColors.PrimaryBlue.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SPONSORED",
                            style = MinimalTypography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            ),
                            color = MinimalGlassColors.PrimaryBlue
                        )
                    }

                    if (BuildConfig.DEBUG) {
                        Text(
                            text = "(Test Ad Mode)",
                            style = MinimalTypography.labelSmall.copy(fontSize = 9.sp),
                            color = MinimalGlassColors.TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onOpenPreferences,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Ad options & Supporter settings",
                        tint = MinimalGlassColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Render the loaded AdMob NativeAdView or a graceful fallback
            val currentAd = nativeAd
            if (currentAd != null) {
                val isDark = MinimalGlassColors.isDarkTheme
                val textColorPrimary = MinimalGlassColors.TextPrimary.toArgb()
                val textColorSecondary = MinimalGlassColors.TextSecondary.toArgb()
                val accentColor = MinimalGlassColors.PrimaryBlue.toArgb()
                val btnBgColor = if (isDark) 0xFF1D283A.toInt() else 0xFF2563EB.toInt()
                val btnTextColor = if (isDark) 0xFF38BDF8.toInt() else 0xFFFFFFFF.toInt()

                AndroidView(
                    factory = { ctx ->
                        createNativeAdView(
                            ctx = ctx,
                            ad = currentAd,
                            textColorPrimary = textColorPrimary,
                            textColorSecondary = textColorSecondary,
                            btnBgColor = btnBgColor,
                            btnTextColor = btnTextColor
                        )
                    },
                    update = { view ->
                        bindNativeAdView(
                            view = view,
                            ad = currentAd,
                            textColorPrimary = textColorPrimary,
                            textColorSecondary = textColorSecondary,
                            btnBgColor = btnBgColor,
                            btnTextColor = btnTextColor
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Loading sponsor highlight...",
                        style = MinimalTypography.bodySmall.copy(fontSize = 11.sp),
                        color = MinimalGlassColors.TextMuted
                    )
                }
            } else {
                // Graceful fallback card when offline or no fill
                GracefulSponsorFallbackCard(onOpenPreferences = onOpenPreferences)
            }
        }
    }
}

/**
 * Creates the Android NativeAdView structure compatible with Google Mobile Ads guidelines.
 */
private fun createNativeAdView(
    ctx: Context,
    ad: NativeAd,
    textColorPrimary: Int,
    textColorSecondary: Int,
    btnBgColor: Int,
    btnTextColor: Int
): NativeAdView {
    val nativeAdView = NativeAdView(ctx)

    val rootLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val contentRow = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // App/Sponsor Icon
    val iconView = ImageView(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(dpToPx(ctx, 42), dpToPx(ctx, 42)).apply {
            marginEnd = dpToPx(ctx, 12)
        }
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    // Text column (Headline + Body / Advertiser)
    val textCol = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }

    val headlineView = TextView(ctx).apply {
        textSize = 13f
        setTypeface(null, Typeface.BOLD)
        setTextColor(textColorPrimary)
        maxLines = 1
    }

    val bodyView = TextView(ctx).apply {
        textSize = 11f
        setTextColor(textColorSecondary)
        maxLines = 2
    }

    textCol.addView(headlineView)
    textCol.addView(bodyView)

    // Call-To-Action Button
    val ctaButton = Button(ctx).apply {
        textSize = 11f
        setTypeface(null, Typeface.BOLD)
        setTextColor(btnTextColor)
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(ctx, 8).toFloat()
            setColor(btnBgColor)
        }
        background = bgDrawable
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dpToPx(ctx, 36)
        ).apply {
            marginStart = dpToPx(ctx, 10)
        }
        setPadding(dpToPx(ctx, 12), 0, dpToPx(ctx, 12), 0)
    }

    contentRow.addView(iconView)
    contentRow.addView(textCol)
    contentRow.addView(ctaButton)

    rootLayout.addView(contentRow)
    nativeAdView.addView(rootLayout)

    // Register view hooks with NativeAdView
    nativeAdView.headlineView = headlineView
    nativeAdView.bodyView = bodyView
    nativeAdView.iconView = iconView
    nativeAdView.callToActionView = ctaButton

    bindNativeAdView(nativeAdView, ad, textColorPrimary, textColorSecondary, btnBgColor, btnTextColor)

    return nativeAdView
}

private fun bindNativeAdView(
    view: NativeAdView,
    ad: NativeAd,
    textColorPrimary: Int,
    textColorSecondary: Int,
    btnBgColor: Int,
    btnTextColor: Int
) {
    val headline = view.headlineView as? TextView
    val body = view.bodyView as? TextView
    val icon = view.iconView as? ImageView
    val cta = view.callToActionView as? Button

    headline?.setTextColor(textColorPrimary)
    headline?.text = ad.headline ?: ""

    body?.setTextColor(textColorSecondary)
    body?.text = ad.body ?: ad.advertiser ?: ""

    if (ad.icon != null && ad.icon?.drawable != null) {
        icon?.visibility = View.VISIBLE
        icon?.setImageDrawable(ad.icon?.drawable)
    } else {
        icon?.visibility = View.GONE
    }

    if (ad.callToAction != null) {
        cta?.visibility = View.VISIBLE
        cta?.text = ad.callToAction
        cta?.setTextColor(btnTextColor)
        val bg = cta?.background as? GradientDrawable
        bg?.setColor(btnBgColor)
    } else {
        cta?.visibility = View.GONE
    }

    view.setNativeAd(ad)
}

private fun dpToPx(context: Context, dp: Int): Int {
    return (dp * context.resources.displayMetrics.density).toInt()
}

/**
 * Fallback card shown when offline, without leaving an empty or broken space.
 */
@Composable
private fun GracefulSponsorFallbackCard(
    onOpenPreferences: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MinimalGlassColors.PrimaryBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MinimalGlassColors.PrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = "Caliber is 100% Free & Open-Source",
                    style = MinimalTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = MinimalGlassColors.TextPrimary
                )
                Text(
                    text = "No locked features. Turn off ads anytime or drop a tip.",
                    style = MinimalTypography.labelSmall.copy(fontSize = 10.sp),
                    color = MinimalGlassColors.TextSecondary
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MinimalGlassColors.BackgroundSubtle)
                .clickable { onOpenPreferences() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Options",
                style = MinimalTypography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = MinimalGlassColors.PrimaryBlue
            )
        }
    }
}

/**
 * Supporter & Ad Preferences Dialog.
 * Transparent honor system: lets users disable the homescreen ad card for free,
 * or choose to support the project.
 */
@Composable
fun SupporterPreferencesDialog(
    onDismiss: () -> Unit,
    onHideAdsForFree: () -> Unit,
    onKeepAdsActive: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MinimalGlassColors.CardSurface,
        titleContentColor = MinimalGlassColors.TextPrimary,
        textContentColor = MinimalGlassColors.TextSecondary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalCafe,
                    contentDescription = null,
                    tint = MinimalGlassColors.PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Support & Ad Preferences",
                    style = MinimalTypography.headlineMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Caliber is engineered with zero paywalls, zero locked features, and zero telemetry. Ads help fund precision testing hardware.",
                    style = MinimalTypography.bodyMedium.copy(fontSize = 12.sp),
                    color = MinimalGlassColors.TextSecondary
                )

                // Option 1: Hide for Free (Honor System)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MinimalGlassColors.BackgroundSubtle)
                        .border(1.dp, MinimalGlassColors.GlassBorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { onHideAdsForFree() }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MinimalGlassColors.SuccessGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = MinimalGlassColors.SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hide Ads for Free",
                                style = MinimalTypography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = MinimalGlassColors.TextPrimary
                            )
                            Text(
                                text = "Honest gamer pledge — no questions asked. We want you to have the cleanest setup possible.",
                                style = MinimalTypography.labelSmall.copy(fontSize = 10.sp),
                                color = MinimalGlassColors.TextMuted
                            )
                        }
                    }
                }

                // Option 2: Keep Ads Active (Help the dev)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MinimalGlassColors.BackgroundSubtle)
                        .border(1.dp, MinimalGlassColors.GlassBorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { onKeepAdsActive() }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MinimalGlassColors.PrimaryBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MinimalGlassColors.PrimaryBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Keep Sponsor Card Active",
                                style = MinimalTypography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = MinimalGlassColors.TextPrimary
                            )
                            Text(
                                text = "Leave the discrete Bento card on the homescreen to support continuous updates.",
                                style = MinimalTypography.labelSmall.copy(fontSize = 10.sp),
                                color = MinimalGlassColors.TextMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MinimalGlassColors.PrimaryBlue)
            ) {
                Text(text = "Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
