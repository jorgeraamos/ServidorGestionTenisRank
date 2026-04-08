package com.tennis.server.data

import com.tennis.server.model.AppData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio de Datos.
 * Se encarga de leer y escribir el estado completo de la aplicación (AppData)
 * en un archivo JSON local. Esta es la capa de persistencia (Base de Datos simulada).
 *
 * @param dataPath La ruta del archivo físico donde se guardan los datos (por defecto "data.json").
 */
class DataRepository(private val dataPath: String = "data.json") {
    
    // Configuración del motor de JSON (kotlinx.serialization)
    // - ignoreUnknownKeys: Ignora campos en el JSON que ya no existan en las clases de Kotlin.
    // - prettyPrint: Formatea el JSON con saltos de línea y tabulaciones para que sea legible.
    // - encodeDefaults: Guarda los valores por defecto en el JSON (no los omite).
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true 
    }

    /**
     * Carga los datos desde el archivo json a memoria.
     * Esta función usa 'suspend' porque bloquea el hilo principal (lee de disco).
     * @return El objeto AppData cargado, o uno vacío si hay error o no existe.
     */
    suspend fun load(): AppData = withContext(Dispatchers.IO) { // Ejecútalo en hilo secundario (I/O)
        val file = File(dataPath)
        
        // Si el archivo no existe (ej. primera vez que se abre la app), creamos uno por defecto.
        if (!file.exists()) {
            val defaultData = AppData()
            save(defaultData)
            return@withContext defaultData
        }

        try {
            // Leer el bloque de texto y decodificarlo al modelo AppData
            val content = file.readText()
            json.decodeFromString<AppData>(content)
        } catch (e: Exception) {
            println("Error al cargar la base de datos local: ${e.message}")
            AppData() // Fallback a datos limpios/vacíos si el JSON estaba corrupto
        }
    }

    /**
     * Guarda el estado actual de la aplicación de vuelta al disco JSON.
     * @param data Objeto AppData a serializar y guardar.
     */
    suspend fun save(data: AppData) = withContext(Dispatchers.IO) { // Secundario
        val file = File(dataPath)
        try {
            // Convertir las clases serializables en texto JSON con el format pretty print
            val content = json.encodeToString(data)
            // Sobreescribir el archivo entero
            file.writeText(content)
        } catch (e: Exception) {
            println("Error al guardar la base de datos local: ${e.message}")
        }
    }
}
