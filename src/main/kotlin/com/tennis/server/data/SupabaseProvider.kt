package com.tennis.server.data
import com.tennis.server.data.SupabaseClient.client
import com.tennis.server.model.Edicion
import com.tennis.server.model.Jornada
import com.tennis.server.model.Participante
import com.tennis.server.model.Partido
import com.tennis.server.model.Ranking
import com.tennis.server.model.Set
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

import io.github.jan.supabase.storage.Storage
import java.util.Collections.emptyList
import java.util.Locale.filter
import javax.management.Query.eq
import kotlin.collections.map


// Conexión con Supabase
object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = ConfigLoader.supabaseUrl,
        supabaseKey = ConfigLoader.supabaseKey
    ) {
        install(Auth)
        install(Postgrest)
        install(io.github.jan.supabase.storage.Storage)
    }
}

suspend fun getAllRankings(): List<Ranking> {
    return try {
        val response = client.postgrest["ranking"]
            .select {}
        response.decodeList<Ranking>()
    } catch (e: Exception) {
        println("Error: ${e.message}")
        emptyList()
    }
}

suspend fun getAllEdiciones(rankingId: Int): List<Edicion> {
    return try {
        val response = client.postgrest["edicion"]
            .select {
                filter {
                    // Filtramos por el ID del ranking y por el estado de las ediciones
                    eq("id_ranking", rankingId)
                    eq("estado", "activo")
                }
            }
        response.decodeList<Edicion>()
    } catch (e: Exception) {
        println("Error: ${e.message}")
        emptyList()
    }
}

// funcion para obtener todas las jorndas de la edición seleccionada
suspend fun getAllJornadas(idEdicion: Int): Pair<List<Jornada>, List<Partido>>{

    val jornadas = try {
        val response = client.postgrest["jornada"]
            .select {
                filter {
                    // Filtramos por el ID del ranking y por el estado de las ediciones
                    eq("id_edicion", idEdicion)
                }
            }
        response.decodeList<Jornada>()
    } catch (e: Exception) {
        println("Error: ${e.message}")
        emptyList()
    }

    // Cogemos todos los partidos de cada jornada:
    // Usamos flatMap para convertir la lista de listas en una sola lista plana
    val partidos = jornadas.flatMap { jornada ->
        try {
            client.postgrest["partido"]
                .select { filter { eq("id_jornada", jornada.id) } }
                .decodeList<Partido>()
        } catch (e: Exception) {
            println("Error: ${e.message}")
            emptyList()
        }
    }
    return Pair(jornadas, partidos)
}


// Una vez se ha seleccionado el ranking y su edicion, procedemos a coger todos los participantes.
suspend fun getAllParticipantes(edicionId: Int): List<Participante> {
    return try {
        val response = client.postgrest["participa"]
            .select(Columns.raw(
                "id_jugador, puntos, partidos_jugados, historial_rivales, jugador(id, nombre_completo)")
            ) {
                filter {
                    eq("id_edicion", edicionId)
                }
                order("puntos", order = Order.DESCENDING)
            }
        response.decodeList<Participante>()
    } catch (e: Exception) {
        println("Error cargando participantes: ${e.message}")
        emptyList()
    }
}


// Función para obtener la última jornada de la edición seleccionada
suspend fun getUltimaJornada(idEdicion: Int): Jornada? {
    return try {
        client.postgrest["jornada"]
            .select {
                filter {
                    eq("id_edicion", idEdicion)
                }
                // Ordenamos por ID de forma descendente (la última creada tendrá el ID más alto)
                // También se podría ordenar por "fecha_inicio"
                order("id", order = Order.DESCENDING)
                // Limitamos a 1 para coger solo la última jornada
                limit(count = 1)
            }
            .decodeSingleOrNull<Jornada>()
    } catch (e: Exception) {
        println("Error en Supabase en getUltimaJornada: ${e.message}")
        null
    }
}

suspend fun getAllSets(idsPartidos: List<String>): List<Set> {
    if (idsPartidos.isEmpty()) return emptyList()
    return try {
        val response = client.postgrest["set"]
            .select {
                filter {
                    // Traemos todos los sets cuyo id_partido esté en nuestra lista
                    isIn("id_partido", idsPartidos)
                }
            }
        response.decodeList<Set>()
    } catch (e: Exception) {
        println("Error cargando los sets: ${e.message}")
        emptyList()
    }
}



suspend fun insertJornada(nuevaJornada: Jornada, onLog: (String) -> Unit): Jornada {

    return try{
        // Insertamos en supabase la nueva jornada creada
        val result = client.postgrest["jornada"].insert(nuevaJornada){
            select()
        }

        val jornadaInsertada = result.decodeSingle<Jornada>()

        onLog("Jornada ${jornadaInsertada.numero} insertada correctamente con ID: ${jornadaInsertada.id}")

        // Retornamos la jornada insertada:
        jornadaInsertada

    }catch (e : Exception){
        onLog("Error al insertar la nueva Jornada en supabase:  ${e.message} " )
        throw e
    }
}

suspend fun insertPartidos(partidosGenerados: List<Partido>, edicionId: Int, onLog: (String) -> Unit) {
    // No guardamos como partido el partido de descanso.
    val partidosReales = partidosGenerados.filter {
        it.idJugador1 != "SISTEMA_BYE" && it.idJugador2 != "SISTEMA_BYE"
    }

    try{
        // Insertamos en supabase los partidos
        client.postgrest["partido"].insert(partidosReales)
    }catch(e : Exception){
        onLog("Error al insertar los partidos en supabase:  ${e.message} " )
    }

//    try {
//        // Actualizamos el historial de los rivales para cada jugador
//        partidosGenerados.forEach { partido ->
//            // Si el jugador1 es real, le añadimos al jugador2 (sea real o BYE)
//            if (partido.idJugador1 != "SISTEMA_BYE") {
//                actualizarHistorialRivales(edicionId, partido.idJugador1, partido.idJugador2)
//            }
//
//            // Lo mismo para el jugador2
//            if (partido.idJugador2 != "SISTEMA_BYE") {
//                actualizarHistorialRivales(edicionId, partido.idJugador2, partido.idJugador1)
//            }
//        }
//    }catch (e : Exception){
//        onLog("Error al actualizar el historial de los rivales de cada jugador:  ${e.message} " )
//    }
}

suspend fun insertSets(partidosGenerados: List<Partido>, onLog: (String) -> Unit){
    val partidosReales = partidosGenerados.filter {
        it.idJugador1 != "SISTEMA_BYE" && it.idJugador2 != "SISTEMA_BYE"
    }

    // Creamos la lista donde guardaremos todos los sets de todos los partidos
    val todosLosSets = mutableListOf<com.tennis.server.model.Set>()

    for(partido in partidosReales){
        for (numeroSet in 1..3) {
            todosLosSets.add(
                com.tennis.server.model.Set(
                    id = java.util.UUID.randomUUID().toString(), // Generamos un ID único
                    idPartido = partido.id,
                    numeroSet = numeroSet,
                    juegosJugador1 = 0,
                    juegosJugador2 = 0
                )
            )
        }
    }

    if(todosLosSets.isNotEmpty()){
        try {
            client.postgrest["set"].insert(todosLosSets)
            onLog("Se han creado correctamente ${todosLosSets.size} sets para la jornada.")
        } catch (e: Exception) {
            onLog("Error al insertar los sets: ${e.message}")
            e.printStackTrace()
        }
    }
}

// Función para actualizar el historial de rivales de un jugador
suspend fun actualizarHistorialRivales(
    edicionId: Int,
    jugadorId: String,
    rivalId: String,
) {
    try {
        // Buscamos el historial que tiene el jugador actualmente en la DB
        val participante = client.postgrest["participa"]
            .select(Columns.raw("id_jugador, puntos, partidos_jugados, historial_rivales, jugador(id, nombre_completo)")) { // <--- AÑADIDO
                filter {
                    eq("id_edicion", edicionId)
                    eq("id_jugador", jugadorId)
                }
            }
            .decodeSingle<Participante>()

        val historialActual = participante.historialRivales

        // Mantenemos solo los últimos 3 rivales para no saturar la penalización
        val nuevoHistorial = (listOf(rivalId) + historialActual).take(3)

        client.postgrest["participa"].update(
            {
                set("historial_rivales", nuevoHistorial)
            }
        ) {
            filter {
                eq("id_edicion", edicionId)
                eq("id_jugador", jugadorId)
            }
        }

        //("SUPABASE", "Historial de rivales actualizado para $jugadorId")

    } catch (e: Exception) {
        println("SUPABASE_ERROR" + "Error al actualizar historial: ${e.message}")
    }
}

// Función que se utilizará para cambiar el estado de una jornada para marcarla como finalizada
suspend fun updateJornada(idJornada: Int, onLog: (String) -> Unit){
    try {
        client.postgrest["jornada"].update(
            {
                set("estado", "Finalizada")
            }
        ) {
            filter {
                eq("id", idJornada)
            }
        }
    }catch(e : Exception ){
        onLog("Error al actualizar el estado de la jornada: ${e.message}")
    }
}

// Función que se utilizará para actualizar las puntuaciones de cada jugador y el ganador del partido
suspend fun updatePuntuaciones(
    idEdicion: Int,
    idPartido: String,
    idJugador1: String,
    idJugador2: String,
    idGanador: String,
    puntosJugador1: Int,
    puntosJugador2: Int,
    partidosJ1: Int,
    partidosJ2: Int
    ){
    try {
        client.postgrest["partido"].update(
            {
                set("id_ganador", idGanador)
                set("estado", "Jugado")
            }
        ) {
            filter {
                eq("id", idPartido)
            }
        }
    }catch(e : Exception ){
        //onLog("Error al actualizar el estado de la jornada: ${e.message}")
    }

    try {
        client.postgrest["participa"].update(
            {
                set("puntos", puntosJugador1)
                set("partidos_jugados", partidosJ1 + 1)
            }
        ) {
            filter {
                eq("id_edicion", idEdicion)
                eq("id_jugador", idJugador1)
            }
        }
    }catch(e : Exception ){
        //onLog("Error al actualizar el estado de la jornada: ${e.message}")
    }

    try {
        client.postgrest["participa"].update(
            {
                set("puntos", puntosJugador2)
                set("partidos_jugados", partidosJ2 +1)
            }
        ) {
            filter {
                eq("id_jugador", idJugador2)
            }
        }
    }catch(e : Exception ){
        //onLog("Error al actualizar el estado de la jornada: ${e.message}")
    }

}


// Función que elimina una jornada en el caso en el que el administrador se haya equivocado y la jornada no se haya
// jugado aún
suspend fun deleteJornada(idJornada: Int, onLog: (String) -> Unit){
    try{
        client.postgrest["jornada"].delete(
        ) {
            filter {
                eq("id", idJornada)
            }
        }
    }catch (e : Exception){
        onLog("Error al eliminar la jornada ${e.message}")
    }

}


