package com.tennis.server.data

import java.io.File
import java.util.Properties

// Objeto para obtener los datos que existen en local properties
object ConfigLoader {
    private val properties = Properties()

    init {
        val propertiesFile = File("local.properties")
        if (propertiesFile.exists()) {
            propertiesFile.inputStream().use { properties.load(it) }
        } else {
            println("Error: No se encontró el archivo local.properties")
        }
    }

    val supabaseUrl: String get() = properties.getProperty("SUPABASE_URL") ?: ""
    val supabaseKey: String get() = properties.getProperty("SUPABASE_KEY") ?: ""
}