package com.example.multimediaapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.multimediaapp.ui.theme.MultimediaAppTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Usamos Jetpack Compose para definir la interfaz
        setContent {
            MultimediaAppTheme {
                AppContent()
            }
        }
    }
}

/**
 * Función principal de la interfaz.
 * Muestra el botón de selección, el nombre del archivo,
 * la barra de progreso y los controles de reproducción.
 */
@Composable
fun AppContent() {
    val context = LocalContext.current

    // Estado para mostrar el nombre del archivo seleccionado
    var selectedFileName by remember { mutableStateOf("Ningún archivo seleccionado") }

    // Estado para almacenar el URI del archivo seleccionado
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    // Estado para saber si se está reproduciendo
    var isPlaying by remember { mutableStateOf(false) }

    // Estado para la barra de progreso (de 0.0 a 1.0)
    var progress by remember { mutableStateOf(0f) }

    // Referencia al reproductor multimedia
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }

    // Selector de archivos, limitado a tipo audio
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            selectedFileName = getFileNameFromUri(context, it) ?: "Archivo seleccionado"

            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, it)
            mediaPlayer?.start()
            isPlaying = true
        }
    }

    // Efecto que actualiza la barra de progreso cada 500ms mientras se reproduce
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    val current = it.currentPosition
                    val total = it.duration
                    progress = current.toFloat() / total.toFloat()
                }
            }
            delay(500)
        }
    }

    // Libera recursos del reproductor al cerrar la app
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    // Interfaz de usuario
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Botón para seleccionar archivo
            Button(onClick = {
                launcher.launch("audio/*")
            }) {
                Text(text = "Seleccionar archivo multimedia")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Muestra el nombre del archivo seleccionado
            Text(text = selectedFileName)

            Spacer(modifier = Modifier.height(24.dp))

            // Barra de progreso de reproducción
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))

            // Controles: Play, Pause, Stop
            Row {
                Button(
                    onClick = {
                        mediaPlayer?.start()
                        isPlaying = true
                    },
                    enabled = selectedUri != null && !isPlaying
                ) {
                    Text("▶ Play")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        mediaPlayer?.pause()
                        isPlaying = false
                    },
                    enabled = selectedUri != null && isPlaying
                ) {
                    Text("⏸ Pause")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        mediaPlayer?.stop()
                        mediaPlayer?.prepare()
                        isPlaying = false
                        progress = 0f
                    },
                    enabled = selectedUri != null
                ) {
                    Text("⏹ Stop")
                }
            }
        }
    }
}

/**
 * Obtiene el nombre legible del archivo desde su URI.
 */
fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}