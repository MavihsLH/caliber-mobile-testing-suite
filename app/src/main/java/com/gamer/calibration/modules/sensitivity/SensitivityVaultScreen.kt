package com.gamer.calibration.modules.sensitivity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamer.calibration.ui.components.CyberButton
import com.gamer.calibration.ui.components.TelemetryValue
import com.gamer.calibration.ui.theme.MinimalGlassColors
import com.gamer.calibration.ui.theme.MinimalTypography
import com.gamer.calibration.ui.theme.TelemetryFonts
import java.util.Locale

@Composable
fun SensitivityVaultScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { SensitivityRepository(context) }
    var profiles by remember { mutableStateOf(repository.loadProfiles()) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Screen DPI metrics
    val displayMetrics = context.resources.displayMetrics
    val screenXdpi = if (displayMetrics.xdpi > 100f) displayMetrics.xdpi else 400f

    // Converter State
    var sourceGame by remember { mutableStateOf(SupportedGame.COD_MOBILE) }
    var targetGame by remember { mutableStateOf(SupportedGame.PUBG_BGMI) }
    var sourceSens by remember { mutableFloatStateOf(sourceGame.defaultSens) }

    val convertedSens = remember(sourceGame, targetGame, sourceSens) {
        SensitivityMath.convertSensitivity(sourceGame, targetGame, sourceSens)
    }

    val sourceCm360 = remember(sourceGame, sourceSens, screenXdpi) {
        SensitivityMath.calculateCmPer360(sourceGame, sourceSens, screenXdpi)
    }

    val targetCm360 = remember(targetGame, convertedSens, screenXdpi) {
        SensitivityMath.calculateCmPer360(targetGame, convertedSens, screenXdpi)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MinimalGlassColors.Background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MinimalGlassColors.CardSurface)
                .border(1.dp, MinimalGlassColors.GlassBorder)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MinimalGlassColors.TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sensitivity Converter",
                    style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MinimalGlassColors.TextPrimary
                )
                Text(
                    text = "Convert aim sensitivity between games",
                    style = MinimalTypography.labelSmall.copy(fontSize = 11.sp),
                    color = MinimalGlassColors.TextMuted
                )
            }

            IconButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Profile",
                    tint = MinimalGlassColors.PrimaryBlue
                )
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MinimalGlassColors.CardSurface,
            contentColor = MinimalGlassColors.PrimaryBlue,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = MinimalGlassColors.PrimaryBlue
                )
            }
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Text(
                        text = "CONVERTER",
                        style = MinimalTypography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Text(
                        text = "SAVED PROFILES (${profiles.size})",
                        style = MinimalTypography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            )
        }

        if (selectedTabIndex == 0) {
            // Converter View
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Game Source Selector Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "SOURCE GAME",
                            style = MinimalTypography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MinimalGlassColors.TextMuted
                        )

                        GameSelectorComponent(
                            label = "Source Game",
                            selectedGame = sourceGame,
                            onSelect = {
                                sourceGame = it
                                sourceSens = it.defaultSens
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Camera Sensitivity",
                                style = MinimalTypography.bodyMedium,
                                color = MinimalGlassColors.TextSecondary
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f", sourceSens),
                                style = TelemetryFonts.Large,
                                color = MinimalGlassColors.PrimaryBlue
                            )
                        }

                        Slider(
                            value = sourceSens,
                            onValueChange = { sourceSens = it },
                            valueRange = 1f..sourceGame.maxSens,
                            colors = SliderDefaults.colors(
                                thumbColor = MinimalGlassColors.PrimaryBlue,
                                activeTrackColor = MinimalGlassColors.PrimaryBlue,
                                inactiveTrackColor = MinimalGlassColors.GlassBorder
                            )
                        )
                    }
                }

                // Swap / Conversion Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MinimalGlassColors.BackgroundSubtle)
                            .border(1.dp, MinimalGlassColors.GlassBorder, CircleShape)
                            .clickable {
                                val temp = sourceGame
                                sourceGame = targetGame
                                targetGame = temp
                                sourceSens = convertedSens.coerceIn(1f, sourceGame.maxSens)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap games",
                            tint = MinimalGlassColors.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Target Game Selector Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MinimalGlassColors.CardSurface)
                        .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "TARGET GAME",
                            style = MinimalTypography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MinimalGlassColors.TextMuted
                        )

                        GameSelectorComponent(
                            label = "Target Game",
                            selectedGame = targetGame,
                            onSelect = { targetGame = it }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Converted Output Display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MinimalGlassColors.PrimaryBlueLight)
                                .border(1.dp, MinimalGlassColors.PrimaryBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "EQUIVALENT SENSITIVITY",
                                    style = MinimalTypography.labelSmall,
                                    color = MinimalGlassColors.PrimaryBlue
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f", convertedSens),
                                    style = TelemetryFonts.Hero.copy(fontSize = 38.sp),
                                    color = MinimalGlassColors.PrimaryBlue
                                )
                                Text(
                                    text = "Matches your ${sourceGame.displayName} 360° swipe distance",
                                    style = MinimalTypography.bodyMedium.copy(fontSize = 12.sp),
                                    color = MinimalGlassColors.TextSecondary
                                )
                            }
                        }

                        // Physical Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Physical 360° Travel",
                                    style = MinimalTypography.labelSmall,
                                    color = MinimalGlassColors.TextMuted
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f cm", targetCm360),
                                    style = TelemetryFonts.Medium,
                                    color = MinimalGlassColors.TextPrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Digitizer Hardware DPI",
                                    style = MinimalTypography.labelSmall,
                                    color = MinimalGlassColors.TextMuted
                                )
                                Text(
                                    text = String.format(Locale.US, "%.0f DPI", screenXdpi),
                                    style = TelemetryFonts.Medium,
                                    color = MinimalGlassColors.TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CyberButton(
                                text = "Copy Value",
                                onClick = {
                                    copyToClipboard(context, String.format(Locale.US, "%.1f", convertedSens), "Sensitivity copied")
                                },
                                modifier = Modifier.weight(1f),
                                accentColor = MinimalGlassColors.PrimaryBlue,
                                filled = false
                            )

                            CyberButton(
                                text = "Save Profile",
                                onClick = {
                                    val newProfile = SensitivityProfile(
                                        name = "${targetGame.displayName} - Auto Calibrated",
                                        game = targetGame,
                                        primarySens = convertedSens,
                                        adsSens = (convertedSens * 0.85f).coerceAtLeast(1f),
                                        gyroSens = (convertedSens * 1.8f).coerceAtLeast(1f),
                                        notes = "Converted from ${sourceGame.displayName} (${String.format(Locale.US, "%.1f", sourceSens)})"
                                    )
                                    repository.saveProfile(newProfile)
                                    profiles = repository.loadProfiles()
                                    Toast.makeText(context, "Profile saved", Toast.LENGTH_SHORT).show()
                                    selectedTabIndex = 1
                                },
                                modifier = Modifier.weight(1f),
                                accentColor = MinimalGlassColors.PrimaryBlue,
                                filled = true
                            )
                        }
                    }
                }

                // Pro Tip Verification Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MinimalGlassColors.BackgroundSubtle)
                        .border(1.dp, MinimalGlassColors.GlassBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Tip",
                            tint = MinimalGlassColors.PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "Pro Tip: Practice Range 360° Verification",
                                style = MinimalTypography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = MinimalGlassColors.TextPrimary
                            )
                            Text(
                                text = "Stand at a fixed practice landmark. Swipe from screen center to the bezel edge and note your compass degrees turned. In your target game, apply this converted sensitivity to confirm the identical swipe distance replicates the exact same angle.",
                                style = MinimalTypography.bodyMedium.copy(fontSize = 11.sp),
                                color = MinimalGlassColors.TextSecondary
                            )
                        }
                    }
                }
            }
        } else {
            // Vault Profiles List View
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (profiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved profiles yet. Tap '+' to create your first profile.",
                            style = MinimalTypography.bodyMedium,
                            color = MinimalGlassColors.TextMuted
                        )
                    }
                } else {
                    profiles.forEach { profile ->
                        ProfileCard(
                            profile = profile,
                            onCopy = {
                                val text = "${profile.name}\n${profile.game.displayName}\nSens: ${profile.primarySens} | ADS: ${profile.adsSens} | Gyro: ${profile.gyroSens}"
                                copyToClipboard(context, text, "Profile copied to clipboard")
                            },
                            onDelete = {
                                repository.deleteProfile(profile.id)
                                profiles = repository.loadProfiles()
                                Toast.makeText(context, "Profile deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // Add Profile Dialog
        if (showAddDialog) {
            AddProfileDialog(
                onDismiss = { showAddDialog = false },
                onSave = { newProfile ->
                    repository.saveProfile(newProfile)
                    profiles = repository.loadProfiles()
                    showAddDialog = false
                    Toast.makeText(context, "Profile saved to Vault", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
private fun GameSelectorComponent(
    label: String,
    selectedGame: SupportedGame,
    onSelect: (SupportedGame) -> Unit
) {
    var showPickerDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Selected game card with Change button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MinimalGlassColors.BackgroundSubtle)
                .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(10.dp))
                .clickable { showPickerDialog = true }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedGame.displayName,
                        style = MinimalTypography.headlineSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        color = MinimalGlassColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${selectedGame.category} • ${selectedGame.engine}",
                            style = MinimalTypography.labelSmall.copy(fontSize = 11.sp),
                            color = MinimalGlassColors.TextMuted
                        )
                        Text(
                            text = "FOV ${selectedGame.defaultFov.toInt()}°",
                            style = MinimalTypography.labelSmall.copy(fontSize = 10.sp, color = MinimalGlassColors.PrimaryBlue),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Browse",
                        style = MinimalTypography.labelSmall.copy(fontSize = 11.sp, color = MinimalGlassColors.PrimaryBlue),
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Browse games",
                        tint = MinimalGlassColors.PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Horizontal Quick-Picks Row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(SupportedGame.entries) { game ->
                val isSelected = game == selectedGame
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MinimalGlassColors.PrimaryBlueLight else MinimalGlassColors.BackgroundSubtle)
                        .border(
                            1.dp,
                            if (isSelected) MinimalGlassColors.PrimaryBlue else MinimalGlassColors.GlassBorderSubtle,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelect(game) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = game.shortName,
                        style = MinimalTypography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) MinimalGlassColors.PrimaryBlue else MinimalGlassColors.TextSecondary
                    )
                }
            }
        }
    }

    if (showPickerDialog) {
        GamePickerDialog(
            title = "Select $label",
            currentlySelected = selectedGame,
            onDismiss = { showPickerDialog = false },
            onSelect = {
                onSelect(it)
                showPickerDialog = false
            }
        )
    }
}

@Composable
private fun GamePickerDialog(
    title: String,
    currentlySelected: SupportedGame,
    onDismiss: () -> Unit,
    onSelect: (SupportedGame) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredGames = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            SupportedGame.entries
        } else {
            val q = searchQuery.trim().lowercase()
            SupportedGame.entries.filter {
                it.displayName.lowercase().contains(q) ||
                it.shortName.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.engine.lowercase().contains(q)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MinimalGlassColors.CardSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = title,
                style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MinimalGlassColors.TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search 16 games...", color = MinimalGlassColors.TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MinimalGlassColors.PrimaryBlue,
                        unfocusedBorderColor = MinimalGlassColors.GlassBorder
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filteredGames.forEach { game ->
                        val isSelected = game == currentlySelected
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MinimalGlassColors.PrimaryBlueLight else MinimalGlassColors.BackgroundSubtle)
                                .border(
                                    1.dp,
                                    if (isSelected) MinimalGlassColors.PrimaryBlue else MinimalGlassColors.GlassBorderSubtle,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelect(game) }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = game.displayName,
                                        style = MinimalTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) MinimalGlassColors.PrimaryBlue else MinimalGlassColors.TextPrimary
                                    )
                                    Text(
                                        text = "${game.category} • ${game.engine} • FOV ${game.defaultFov.toInt()}°",
                                        style = MinimalTypography.labelSmall.copy(fontSize = 10.sp),
                                        color = MinimalGlassColors.TextMuted
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MinimalGlassColors.PrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = MinimalGlassColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun ProfileCard(
    profile: SensitivityProfile,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MinimalGlassColors.CardSurface)
            .border(1.dp, MinimalGlassColors.GlassBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = profile.name,
                        style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MinimalGlassColors.TextPrimary
                    )
                    Text(
                        text = profile.game.displayName,
                        style = MinimalTypography.labelSmall.copy(color = MinimalGlassColors.PrimaryBlue),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MinimalGlassColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MinimalGlassColors.DangerRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MinimalGlassColors.BackgroundSubtle)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryValue(
                    label = "PRIMARY",
                    value = String.format(Locale.US, "%.0f", profile.primarySens),
                    unit = "sens"
                )
                TelemetryValue(
                    label = "ADS",
                    value = String.format(Locale.US, "%.0f", profile.adsSens),
                    unit = "sens"
                )
                TelemetryValue(
                    label = "GYRO",
                    value = String.format(Locale.US, "%.0f", profile.gyroSens),
                    unit = "sens"
                )
            }

            if (profile.notes.isNotBlank()) {
                Text(
                    text = profile.notes,
                    style = MinimalTypography.bodyMedium.copy(fontSize = 12.sp),
                    color = MinimalGlassColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun AddProfileDialog(
    onDismiss: () -> Unit,
    onSave: (SensitivityProfile) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var game by remember { mutableStateOf(SupportedGame.COD_MOBILE) }
    var primarySens by remember { mutableFloatStateOf(game.defaultSens) }
    var adsSens by remember { mutableFloatStateOf(game.defaultSens * 0.85f) }
    var gyroSens by remember { mutableFloatStateOf(game.defaultSens * 1.8f) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MinimalGlassColors.CardSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "New Sensitivity Profile",
                style = MinimalTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MinimalGlassColors.TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile Name (e.g. Ranked AR)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MinimalGlassColors.PrimaryBlue,
                        unfocusedBorderColor = MinimalGlassColors.GlassBorder
                    )
                )

                Text(
                    text = "Select Title:",
                    style = MinimalTypography.labelSmall,
                    color = MinimalGlassColors.TextMuted
                )
                GameSelectorComponent(label = "Game", selectedGame = game, onSelect = { game = it })

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Primary Sens: ${primarySens.toInt()}",
                        style = MinimalTypography.bodyMedium,
                        color = MinimalGlassColors.TextPrimary
                    )
                }
                Slider(
                    value = primarySens,
                    onValueChange = { primarySens = it },
                    valueRange = 1f..game.maxSens
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ADS Sens: ${adsSens.toInt()}",
                        style = MinimalTypography.bodyMedium,
                        color = MinimalGlassColors.TextPrimary
                    )
                }
                Slider(
                    value = adsSens,
                    onValueChange = { adsSens = it },
                    valueRange = 1f..game.maxSens
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Gyro Sens: ${gyroSens.toInt()}",
                        style = MinimalTypography.bodyMedium,
                        color = MinimalGlassColors.TextPrimary
                    )
                }
                Slider(
                    value = gyroSens,
                    onValueChange = { gyroSens = it },
                    valueRange = 1f..300f
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MinimalGlassColors.PrimaryBlue,
                        unfocusedBorderColor = MinimalGlassColors.GlassBorder
                    )
                )
            }
        },
        confirmButton = {
            CyberButton(
                text = "Save Profile",
                onClick = {
                    val finalName = if (name.isBlank()) "${game.displayName} Setup" else name
                    onSave(
                        SensitivityProfile(
                            name = finalName,
                            game = game,
                            primarySens = primarySens,
                            adsSens = adsSens,
                            gyroSens = gyroSens,
                            notes = notes
                        )
                    )
                },
                accentColor = MinimalGlassColors.PrimaryBlue,
                filled = true
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = MinimalGlassColors.TextSecondary)
            }
        }
    )
}

private fun copyToClipboard(context: Context, text: String, feedbackToast: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Caliber Sensitivity", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, feedbackToast, Toast.LENGTH_SHORT).show()
}
