package com.gamer.calibration.modules.sensitivity

import android.content.Context
import android.util.DisplayMetrics
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class SupportedGame(
    val id: String,
    val displayName: String,
    val shortName: String,
    val category: String,
    val defaultFov: Float,
    val yawMultiplier: Double,
    val maxSens: Float,
    val defaultSens: Float,
    val engine: String = "Unity"
) {
    COD_MOBILE("codm", "Call of Duty: Mobile", "CODM", "Competitive FPS", 75f, 0.022, 300f, 95f, "Unity"),
    PUBG_BGMI("bgmi", "PUBG Mobile / BGMI", "BGMI", "Battle Royale", 90f, 0.020, 300f, 100f, "Unreal 4"),
    BLOOD_STRIKE("bloodstrike", "Blood Strike", "Blood Strike", "Movement Shooter", 85f, 0.023, 300f, 100f, "Messiah"),
    STANDOFF_2("standoff2", "Standoff 2", "Standoff 2", "Tactical FPS", 75f, 0.022, 5.0f, 2.2f, "Unity"),
    WARZONE_MOBILE("wzm", "Warzone Mobile", "Warzone M", "Battle Royale", 80f, 0.024, 20f, 5.0f, "IW Engine"),
    ARENA_BREAKOUT("arena_breakout", "Arena Breakout", "Arena Breakout", "Tactical Extraction", 90f, 0.020, 300f, 100f, "Unreal 4"),
    DELTA_FORCE("delta", "Delta Force Mobile", "Delta Force", "Tactical FPS", 85f, 0.021, 200f, 80f, "Unreal 4"),
    FREE_FIRE("ff", "Free Fire MAX", "Free Fire", "Battle Royale", 80f, 0.028, 100f, 65f, "Unity"),
    RAINBOW_SIX_MOBILE("r6m", "Rainbow Six Mobile", "R6 Mobile", "Tactical CQB", 75f, 0.022, 100f, 50f, "Ubisoft"),
    CRITICAL_OPS("cops", "Critical Ops", "Critical Ops", "Tactical FPS", 70f, 0.022, 10.0f, 3.5f, "Unity"),
    FARLIGHT_84("farlight84", "Farlight 84", "Farlight 84", "Hero Shooter", 85f, 0.022, 200f, 85f, "Unreal 4"),
    T3_ARENA("t3arena", "T3 Arena", "T3 Arena", "Hero Shooter", 80f, 0.023, 100f, 50f, "Unity"),
    COMBAT_MASTER("combatmaster", "Combat Master", "Combat Master", "Fast FPS", 80f, 0.024, 15.0f, 4.0f, "Unity"),
    NEW_STATE_MOBILE("newstate", "NEW STATE Mobile", "New State", "Battle Royale", 90f, 0.020, 300f, 100f, "Unreal 4"),
    APEX_MOBILE("apex_mobile", "Apex Legends Mobile", "Apex M", "Hero Shooter", 90f, 0.022, 300f, 100f, "Unreal 4"),
    VALORANT_MOBILE("valorant_mobile", "Valorant Mobile", "Valorant M", "Tactical FPS", 80f, 0.022, 10.0f, 2.5f, "Unreal 4")
}

data class SensitivityProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val game: SupportedGame,
    val primarySens: Float,
    val adsSens: Float,
    val gyroSens: Float,
    val notes: String = "",
    val updatedEpochMs: Long = System.currentTimeMillis()
)

object SensitivityMath {
    /**
     * Converts sensitivity from source game to target game while preserving 360 degree rotation feel.
     */
    fun convertSensitivity(
        sourceGame: SupportedGame,
        targetGame: SupportedGame,
        sourceValue: Float
    ): Float {
        if (sourceGame == targetGame) return sourceValue
        // Normalize to raw rotation rate (degrees per pixel)
        val degPerPixel = (sourceValue / sourceGame.defaultSens) * sourceGame.yawMultiplier
        // Scale to target game's native range
        val targetSens = (degPerPixel / targetGame.yawMultiplier) * targetGame.defaultSens
        return targetSens.toFloat().coerceIn(0.1f, targetGame.maxSens)
    }

    /**
     * Calculates physical swipe distance in centimeters required for a full 360-degree rotation.
     */
    fun calculateCmPer360(
        game: SupportedGame,
        sens: Float,
        xdpi: Float
    ): Double {
        if (sens <= 0f || xdpi <= 0f) return 0.0
        val degPerPixel = (sens / game.defaultSens) * game.yawMultiplier
        if (degPerPixel <= 0.0) return 0.0
        val pixelsFor360 = 360.0 / degPerPixel
        val inchesFor360 = pixelsFor360 / xdpi
        return inchesFor360 * 2.54
    }
}

class SensitivityRepository(private val context: Context) {
    private val prefs = run {
        val oldPrefs = context.getSharedPreferences("gamersense_sens_vault", Context.MODE_PRIVATE)
        val newPrefs = context.getSharedPreferences("caliber_sens_vault", Context.MODE_PRIVATE)
        if (!newPrefs.contains("saved_profiles") && oldPrefs.contains("saved_profiles")) {
            newPrefs.edit().putString("saved_profiles", oldPrefs.getString("saved_profiles", "")).apply()
        }
        newPrefs
    }

    fun loadProfiles(): List<SensitivityProfile> {
        val rawJson = prefs.getString("saved_profiles", null)
        if (rawJson.isNullOrBlank()) {
            val defaults = getDefaultPresets()
            saveProfiles(defaults)
            return defaults
        }

        return try {
            val array = JSONArray(rawJson)
            val list = mutableListOf<SensitivityProfile>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val gameId = obj.optString("game", SupportedGame.COD_MOBILE.id)
                val game = SupportedGame.entries.find { it.id == gameId } ?: SupportedGame.COD_MOBILE
                list.add(
                    SensitivityProfile(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Custom Profile"),
                        game = game,
                        primarySens = obj.optDouble("primarySens", 95.0).toFloat(),
                        adsSens = obj.optDouble("adsSens", 85.0).toFloat(),
                        gyroSens = obj.optDouble("gyroSens", 180.0).toFloat(),
                        notes = obj.optString("notes", ""),
                        updatedEpochMs = obj.optLong("updatedEpochMs", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            getDefaultPresets()
        }
    }

    fun saveProfiles(profiles: List<SensitivityProfile>) {
        val array = JSONArray()
        for (p in profiles) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("game", p.game.id)
                put("primarySens", p.primarySens.toDouble())
                put("adsSens", p.adsSens.toDouble())
                put("gyroSens", p.gyroSens.toDouble())
                put("notes", p.notes)
                put("updatedEpochMs", p.updatedEpochMs)
            }
            array.put(obj)
        }
        prefs.edit().putString("saved_profiles", array.toString()).apply()
    }

    fun saveProfile(profile: SensitivityProfile) {
        val current = loadProfiles().toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            current[index] = profile
        } else {
            current.add(0, profile)
        }
        saveProfiles(current)
    }

    fun deleteProfile(id: String) {
        val filtered = loadProfiles().filterNot { it.id == id }
        saveProfiles(filtered)
    }

    private fun getDefaultPresets(): List<SensitivityProfile> {
        return listOf(
            SensitivityProfile(
                name = "CODM - Aggressive AR & Flick",
                game = SupportedGame.COD_MOBILE,
                primarySens = 110f,
                adsSens = 98f,
                gyroSens = 220f,
                notes = "Fast camera rotation for slide-cancelling and quick re-centering."
            ),
            SensitivityProfile(
                name = "BGMI - 4-Finger Gyro Spray",
                game = SupportedGame.PUBG_BGMI,
                primarySens = 95f,
                adsSens = 90f,
                gyroSens = 300f,
                notes = "Max gyro for tight M416 laser recoil control at 100m+."
            ),
            SensitivityProfile(
                name = "Blood Strike - Ultra Mobility",
                game = SupportedGame.BLOOD_STRIKE,
                primarySens = 115f,
                adsSens = 100f,
                gyroSens = 210f,
                notes = "Optimized for high-speed parkour sliding and close-quarters tracking."
            ),
            SensitivityProfile(
                name = "Standoff 2 - CS Precision",
                game = SupportedGame.STANDOFF_2,
                primarySens = 2.4f,
                adsSens = 1.9f,
                gyroSens = 0f,
                notes = "Consistent crosshair placement for clean one-tap headshots."
            ),
            SensitivityProfile(
                name = "Arena Breakout - Tactical DMR",
                game = SupportedGame.ARENA_BREAKOUT,
                primarySens = 90f,
                adsSens = 80f,
                gyroSens = 150f,
                notes = "Smooth scan speed with low micro-jitter for long-distance extraction fights."
            )
        )
    }
}
