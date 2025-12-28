package io.prism.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.prism.data.model.CustomLogo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class CustomLogoStorage(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val logosDir = File(context.filesDir, LOGOS_DIR).apply { mkdirs() }

    suspend fun saveCustomLogo(
        uri: Uri,
        name: String,
        isMonochrome: Boolean
    ): CustomLogo = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val logoFile = File(logosDir, "$id.png")

        
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input)
            FileOutputStream(logoFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            bitmap.recycle()
        }

        val customLogo = CustomLogo(
            id = id,
            name = name,
            filePath = logoFile.absolutePath,
            isMonochrome = isMonochrome
        )

        
        saveLogoMetadata(customLogo)

        customLogo
    }

    suspend fun getAllCustomLogos(): List<CustomLogo> = withContext(Dispatchers.IO) {
        val json = sharedPrefs.getString(KEY_LOGOS, "[]") ?: "[]"
        val jsonArray = JSONArray(json)

        val logos = mutableListOf<CustomLogo>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val filePath = obj.getString("filePath")

            
            if (File(filePath).exists()) {
                logos.add(
                    CustomLogo(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        filePath = filePath,
                        isMonochrome = obj.optBoolean("isMonochrome", false),
                        createdAt = obj.optLong("createdAt", 0)
                    )
                )
            }
        }

        logos
    }

    suspend fun deleteCustomLogo(id: String) = withContext(Dispatchers.IO) {
        val logos = getAllCustomLogos().toMutableList()
        val logo = logos.find { it.id == id }

        logo?.let {
            
            File(it.filePath).delete()

            
            logos.removeAll { l -> l.id == id }
            saveAllLogos(logos)
        }
    }

    suspend fun updateCustomLogo(logo: CustomLogo) = withContext(Dispatchers.IO) {
        val logos = getAllCustomLogos().toMutableList()
        val index = logos.indexOfFirst { it.id == logo.id }

        if (index >= 0) {
            logos[index] = logo
            saveAllLogos(logos)
        }
    }

    private fun saveLogoMetadata(logo: CustomLogo) {
        val logos = getAllCustomLogosSync().toMutableList()
        logos.add(logo)
        saveAllLogos(logos)
    }

    private fun getAllCustomLogosSync(): List<CustomLogo> {
        val json = sharedPrefs.getString(KEY_LOGOS, "[]") ?: "[]"
        val jsonArray = JSONArray(json)

        val logos = mutableListOf<CustomLogo>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            logos.add(
                CustomLogo(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    filePath = obj.getString("filePath"),
                    isMonochrome = obj.optBoolean("isMonochrome", false),
                    createdAt = obj.optLong("createdAt", 0)
                )
            )
        }

        return logos
    }

    private fun saveAllLogos(logos: List<CustomLogo>) {
        val jsonArray = JSONArray()
        logos.forEach { logo ->
            val obj = JSONObject().apply {
                put("id", logo.id)
                put("name", logo.name)
                put("filePath", logo.filePath)
                put("isMonochrome", logo.isMonochrome)
                put("createdAt", logo.createdAt)
            }
            jsonArray.put(obj)
        }

        sharedPrefs.edit()
            .putString(KEY_LOGOS, jsonArray.toString())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "custom_logos"
        private const val KEY_LOGOS = "logos"
        private const val LOGOS_DIR = "custom_logos"
    }
}