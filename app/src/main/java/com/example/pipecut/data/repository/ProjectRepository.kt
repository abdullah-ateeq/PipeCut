package com.example.pipecut.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.pipecut.data.model.AppThemeMode
import com.example.pipecut.data.model.CuttingSettings
import com.example.pipecut.data.model.LengthUnit
import com.example.pipecut.data.model.ProjectRecord
import com.example.pipecut.data.model.RequiredCut
import com.example.pipecut.data.model.StockPipe
import com.example.pipecut.domain.algorithm.PipeCutOptimizer
import org.json.JSONArray
import org.json.JSONObject

class ProjectRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("pipecut_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PROJECTS = "saved_projects"
        private const val KEY_DEFAULT_UNIT = "default_unit"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DEFAULT_KERF = "default_kerf"
        private const val KEY_DEFAULT_TRIM = "default_trim"
        private const val KEY_DEFAULT_REUSABLE = "default_reusable"
    }

    // Default settings loaded from user preferences
    fun getSettings(): CuttingSettings {
        val unitName = prefs.getString(KEY_DEFAULT_UNIT, LengthUnit.MM.name) ?: LengthUnit.MM.name
        val unit = LengthUnit.fromString(unitName)
        val themeName = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        val themeMode = runCatching { AppThemeMode.valueOf(themeName) }.getOrDefault(AppThemeMode.SYSTEM)

        val kerf = prefs.getFloat(KEY_DEFAULT_KERF, 3.0f).toDouble()
        val trim = prefs.getFloat(KEY_DEFAULT_TRIM, 10.0f).toDouble()
        val reusable = prefs.getFloat(KEY_DEFAULT_REUSABLE, 300.0f).toDouble()

        return CuttingSettings(
            sawKerf = kerf,
            endTrim = trim,
            trimBothEnds = false,
            extraAllowancePerPiece = 0.0,
            minReusableLength = reusable,
            unit = unit,
            themeMode = themeMode
        )
    }

    fun saveSettings(settings: CuttingSettings) {
        prefs.edit()
            .putString(KEY_DEFAULT_UNIT, settings.unit.name)
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putFloat(KEY_DEFAULT_KERF, settings.sawKerf.toFloat())
            .putFloat(KEY_DEFAULT_TRIM, settings.endTrim.toFloat())
            .putFloat(KEY_DEFAULT_REUSABLE, settings.minReusableLength.toFloat())
            .apply()
    }

    fun getAllProjects(): List<ProjectRecord> {
        val jsonStr = prefs.getString(KEY_PROJECTS, null) ?: return defaultStarterProjects()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<ProjectRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(deserializeProject(obj))
            }
            if (list.isEmpty()) defaultStarterProjects() else list
        } catch (e: Exception) {
            defaultStarterProjects()
        }
    }

    fun saveProject(project: ProjectRecord) {
        val currentList = getAllProjects().toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == project.id }
        if (existingIndex >= 0) {
            currentList[existingIndex] = project
        } else {
            currentList.add(0, project) // Place newest first
        }
        persistProjects(currentList)
    }

    fun deleteProject(projectId: String) {
        val currentList = getAllProjects().filterNot { it.id == projectId }
        persistProjects(currentList)
    }

    fun clearAllProjects() {
        prefs.edit().remove(KEY_PROJECTS).apply()
    }

    private fun persistProjects(projects: List<ProjectRecord>) {
        val jsonArray = JSONArray()
        for (project in projects) {
            jsonArray.put(serializeProject(project))
        }
        prefs.edit().putString(KEY_PROJECTS, jsonArray.toString()).apply()
    }

    private fun serializeProject(project: ProjectRecord): JSONObject {
        val obj = JSONObject()
        obj.put("id", project.id)
        obj.put("name", project.name)
        obj.put("timestamp", project.timestamp)

        // Settings
        val sObj = JSONObject()
        sObj.put("sawKerf", project.settings.sawKerf)
        sObj.put("endTrim", project.settings.endTrim)
        sObj.put("trimBothEnds", project.settings.trimBothEnds)
        sObj.put("extraAllowance", project.settings.extraAllowancePerPiece)
        sObj.put("minReusable", project.settings.minReusableLength)
        sObj.put("unit", project.settings.unit.name)
        sObj.put("themeMode", project.settings.themeMode.name)
        obj.put("settings", sObj)

        // Stock Pipes
        val stockArray = JSONArray()
        for (sp in project.stockPipes) {
            val spObj = JSONObject()
            spObj.put("id", sp.id)
            spObj.put("length", sp.length)
            spObj.put("quantity", sp.quantity)
            spObj.put("label", sp.label)
            stockArray.put(spObj)
        }
        obj.put("stockPipes", stockArray)

        // Required Cuts
        val cutsArray = JSONArray()
        for (rc in project.requiredCuts) {
            val rcObj = JSONObject()
            rcObj.put("id", rc.id)
            rcObj.put("length", rc.length)
            rcObj.put("quantity", rc.quantity)
            rcObj.put("label", rc.label)
            rcObj.put("colorHex", rc.colorHex)
            cutsArray.put(rcObj)
        }
        obj.put("requiredCuts", cutsArray)

        return obj
    }

    private fun deserializeProject(obj: JSONObject): ProjectRecord {
        val id = obj.getString("id")
        val name = obj.getString("name")
        val timestamp = obj.optLong("timestamp", System.currentTimeMillis())

        val sObj = obj.optJSONObject("settings")
        val settings = if (sObj != null) {
            CuttingSettings(
                sawKerf = sObj.optDouble("sawKerf", 3.0),
                endTrim = sObj.optDouble("endTrim", 10.0),
                trimBothEnds = sObj.optBoolean("trimBothEnds", false),
                extraAllowancePerPiece = sObj.optDouble("extraAllowance", 0.0),
                minReusableLength = sObj.optDouble("minReusable", 300.0),
                unit = LengthUnit.fromString(sObj.optString("unit", "MM")),
                themeMode = runCatching { AppThemeMode.valueOf(sObj.optString("themeMode", "SYSTEM")) }.getOrDefault(AppThemeMode.SYSTEM)
            )
        } else {
            CuttingSettings()
        }

        val stockList = mutableListOf<StockPipe>()
        val stockArray = obj.optJSONArray("stockPipes")
        if (stockArray != null) {
            for (i in 0 until stockArray.length()) {
                val spObj = stockArray.getJSONObject(i)
                stockList.add(
                    StockPipe(
                        id = spObj.optString("id"),
                        length = spObj.getDouble("length"),
                        quantity = spObj.optInt("quantity", 1),
                        label = spObj.optString("label", "")
                    )
                )
            }
        }

        val cutsList = mutableListOf<RequiredCut>()
        val cutsArray = obj.optJSONArray("requiredCuts")
        if (cutsArray != null) {
            for (i in 0 until cutsArray.length()) {
                val rcObj = cutsArray.getJSONObject(i)
                cutsList.add(
                    RequiredCut(
                        id = rcObj.optString("id"),
                        length = rcObj.getDouble("length"),
                        quantity = rcObj.optInt("quantity", 1),
                        label = rcObj.optString("label", ""),
                        colorHex = rcObj.optString("colorHex", "#0284C7")
                    )
                )
            }
        }

        // Re-calculate the result if we have stock & cuts
        val lastResult = if (stockList.isNotEmpty() && cutsList.isNotEmpty()) {
            PipeCutOptimizer.optimize(stockList, cutsList, settings)
        } else null

        return ProjectRecord(
            id = id,
            name = name,
            timestamp = timestamp,
            stockPipes = stockList,
            requiredCuts = cutsList,
            settings = settings,
            lastResult = lastResult
        )
    }

    private fun defaultStarterProjects(): List<ProjectRecord> {
        val mmSettings = CuttingSettings(
            sawKerf = 3.0,
            endTrim = 15.0,
            trimBothEnds = false,
            extraAllowancePerPiece = 0.0,
            minReusableLength = 400.0,
            unit = LengthUnit.MM
        )

        val starterStock = listOf(
            StockPipe(length = 6000.0, quantity = 4, label = "Standard 6m Steel Tube"),
            StockPipe(length = 3000.0, quantity = 2, label = "Short 3m Offcut Stock")
        )

        val starterCuts = listOf(
            RequiredCut(length = 2100.0, quantity = 3, label = "Main Frame Rail", colorHex = "#0284C7"),
            RequiredCut(length = 1450.0, quantity = 4, label = "Cross Brace", colorHex = "#10B981"),
            RequiredCut(length = 850.0, quantity = 6, label = "Support Stanchion", colorHex = "#F59E0B"),
            RequiredCut(length = 420.0, quantity = 4, label = "Foot Gusset", colorHex = "#8B5CF6")
        )

        val result = PipeCutOptimizer.optimize(starterStock, starterCuts, mmSettings)

        val starterProject = ProjectRecord(
            id = "starter_sample_project",
            name = "Handrail & Frame Prototype",
            timestamp = System.currentTimeMillis() - 3600_000,
            stockPipes = starterStock,
            requiredCuts = starterCuts,
            settings = mmSettings,
            lastResult = result
        )

        return listOf(starterProject)
    }
}
