package com.tennis.server.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tennis.server.viewmodel.AppViewModel

/**
 * Enumeración que define las distintas pantallas o paneles
 * disponibles en la interfaz de usuario de la aplicación.
 */
enum class Screen {
    Config,       // Pantalla de configuración del servidor y ranking
    Jugadores,    // Pantalla para gestionar los jugadores inscritos
    Jornadas,     // Pantalla para ver y gestionar las jornadas y partidos
    Logs          // Pantalla para ver el registro de eventos (consola)
}

/**
 * Función composable principal que renderiza la estructura de la aplicación.
 * Contiene un menú lateral (Sidebar) y un área de contenido dinámico.
 *
 * @param viewModel El ViewModel principal que contiene la lógica y estados.
 */
@Composable
fun MainScreen(viewModel: AppViewModel) {
    // Estado que guarda la pantalla seleccionada actualmente en el menú
    var currentScreen by remember { mutableStateOf(Screen.Config) }
    
    // Observamos el estado del scheduler (si está activo generando jornadas)
    val isSchedulerRunning by viewModel.schedulerRunning.collectAsState()
    
    // Observamos los datos generales de la aplicación (configuración, etc.)
    val config by viewModel.appData.collectAsState()

    // Row principal que divide la pantalla en 2 partes: Menú Lateral (Izquierda) y Contenido (Derecha)
    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        
        // MENÚ LATERAL (SIDEBAR)
        Column(
            modifier = Modifier
                .width(260.dp) // Ancho fijo para el menú
                .fillMaxHeight()
                .background(MaterialTheme.colors.surface) // Color de fondo del sidebar
                .padding(vertical = 24.dp)
        ) {
            // Título de la aplicación
            Text(
                text = "Tenis Server",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtítulo mostrando el nombre del ranking actual
            Text(
                text = config.config.selectedRanking?.nombre ?: "Ningún ranking seleccionado",
                style = MaterialTheme.typography.subtitle1,
                color = Color.LightGray,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ELEMENTOS DEL MENÚ
            
            // Botón hacia "Configuración"
            SidebarItem(
                icon = Icons.Default.Settings,
                text = "Configuración",
                isSelected = currentScreen == Screen.Config,
                onClick = { currentScreen = Screen.Config } // Actualiza la pantalla actual
            )
            
            // Botón hacia "Jugadores"
            SidebarItem(
                icon = Icons.Default.Person,
                text = "Jugadores",
                isSelected = currentScreen == Screen.Jugadores,
                onClick = { currentScreen = Screen.Jugadores }
            )
            
            // Botón hacia "Jornadas y Partidos"
            SidebarItem(
                icon = Icons.Default.Build, // Representa las herramientas de partidos
                text = "Jornadas y Partidos",
                isSelected = currentScreen == Screen.Jornadas,
                onClick = { currentScreen = Screen.Jornadas }
            )
            
            // Botón hacia "Consola de Logs"
            SidebarItem(
                icon = Icons.Default.Info, // Representa información / log / consola
                text = "Consola de Logs",
                isSelected = currentScreen == Screen.Logs,
                onClick = { currentScreen = Screen.Logs }
            )

            // Spacer que empuja el siguiente bloque hacia la parte inferior del menú
            Spacer(modifier = Modifier.weight(1f))

            // Indicador de estado para mostrar si el generador automático está en ejecución
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Punto de color (Verde/Primario si está activo, Rojo si no lo está)
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(if (isSchedulerRunning) MaterialTheme.colors.primary else Color.Red, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Mensaje informativo del estado
                Text(
                    text = if (isSchedulerRunning) "Auto-Generación Activa" else "Auto-Generación Inactiva",
                    style = MaterialTheme.typography.caption,
                    color = Color.LightGray
                )
            }
        }
        // FIN DEL MENÚ LATERAL

        // ÁREA DE CONTENIDO PRINCIPAL
        // Box que ocupa el resto del espacio disponible (weight 1f)
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            // Evaluamos la pantalla seleccionada y renderizamos el Panel correspondiente
            when (currentScreen) {
                Screen.Config -> ConfigPanel(viewModel)
                Screen.Jugadores -> JugadoresPanel(viewModel)
                Screen.Jornadas -> JornadasPanel(viewModel)
                Screen.Logs -> LogPanel(viewModel)
            }
        }
    }
}

/**
 * Componente reutilizable para cada elemento/botón del menú lateral.
 *
 * @param icon El ícono que se mostrará a la izquierda.
 * @param text El texto representativo del botón.
 * @param isSelected Booleano que indica si este elemento es el que está seleccionado actualmente.
 * @param onClick Acción a ejecutar cuando el usuario hace clic en el elemento.
 */
@Composable
fun SidebarItem(
    icon: ImageVector,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Si está seleccionado, darle un fondo semitransparente del color primario
    val backgroundColor = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.1f) else Color.Transparent
    
    // Si está seleccionado, texto e ícono serán color primario, sino color por defecto
    val contentColor = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface

    // Contenedor principal del ítem
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Detecta el clic
            .background(backgroundColor)  // Aplica el fondo dinámico
            .padding(horizontal = 24.dp, vertical = 16.dp), // Espaciado interno
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ícono
        Icon(icon, contentDescription = text, tint = contentColor)
        Spacer(modifier = Modifier.width(16.dp))
        // Texto
        Text(text, color = contentColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}
