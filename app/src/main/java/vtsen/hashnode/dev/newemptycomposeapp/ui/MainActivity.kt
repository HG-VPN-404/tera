package vtsen.hashnode.dev.newemptycomposeapp.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

// --- URL WORKER ---
const val WORKER_URL = "https://sky.publicxx.workers.dev"

// --- DATA MODEL ---
data class ApiResponse(val status: String, val total_items: Int, val data: List<FileItem>)
data class FileItem(val filename: String, val size_mb: String?, val is_folder: Boolean?, val thumb: String?, val links: Links?)
data class Links(val proxy: String?, val browse: String?)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Kita bypass Theme bawaan template, pake MaterialTheme standar aja
            MaterialTheme { Surface(color = MaterialTheme.colorScheme.background) { HomeScreen() } }
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var urlInput by remember { mutableStateOf("") }
    var fileList by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf("") }
    var historyStack by remember { mutableStateOf(listOf<String>()) }
    var videoUrlToPlay by remember { mutableStateOf<String?>(null) }

    fun fetchData(targetUrl: String) {
        isLoading = true
        statusMsg = "Loading..."
        scope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(targetUrl).build()
                val response = OkHttpClient().newCall(request).execute()
                val jsonString = response.body?.string()
                if (jsonString != null) {
                    val result: ApiResponse = Gson().fromJson(jsonString, object : TypeToken<ApiResponse>() {}.type)
                    withContext(Dispatchers.Main) {
                        if (result.status == "success") {
                            fileList = result.data
                            statusMsg = "Ditemukan ${result.total_items} item"
                            if (historyStack.isEmpty() || historyStack.last() != targetUrl) historyStack = historyStack + targetUrl
                        } else statusMsg = "Error API"
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { statusMsg = "Error: ${e.message}"; isLoading = false }
            }
        }
    }

    fun downloadFile(url: String, filename: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(filename)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        Toast.makeText(context, "Mulai download...", Toast.LENGTH_SHORT).show()
    }

    if (videoUrlToPlay != null) {
        VideoPlayerScreen(url = videoUrlToPlay!!) { videoUrlToPlay = null }
    } else {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Terabox Native") },
                    navigationIcon = {
                        if (historyStack.size > 1) {
                            IconButton(onClick = { 
                                val newStack = historyStack.dropLast(1)
                                historyStack = newStack
                                fetchData(newStack.last())
                                historyStack = newStack 
                            }) { Icon(Icons.Default.ArrowBack, "Back") }
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                if (fileList.isEmpty() && !isLoading) {
                    OutlinedTextField(value = urlInput, onValueChange = { urlInput = it }, label = { Text("Link Terabox") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { fetchData("$WORKER_URL/?url=$urlInput") }, modifier = Modifier.fillMaxWidth()) { Text("PROSES") }
                }
                if (isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Text(statusMsg, modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn {
                    items(fileList) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                                if (item.is_folder == true && item.links?.browse != null) fetchData(item.links.browse)
                            },
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (item.is_folder == true) Icons.Default.Folder else Icons.Default.VideoFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.filename, style = MaterialTheme.typography.titleMedium)
                                    Text(if (item.is_folder == true) "Folder" else "${item.size_mb} MB", style = MaterialTheme.typography.bodySmall)
                                }
                                if (item.is_folder != true && item.links?.proxy != null) {
                                    IconButton(onClick = { videoUrlToPlay = item.links.proxy }) { Icon(Icons.Default.PlayArrow, "Play") }
                                    IconButton(onClick = { downloadFile(item.links.proxy, item.filename) }) { Icon(Icons.Default.Download, "Download") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(url: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(url)); prepare(); playWhenReady = true } }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    Box(modifier = Modifier.fillMaxSize().padding(0.dp)) {
        AndroidView(factory = { PlayerView(context).apply { player = exoPlayer } }, modifier = Modifier.fillMaxSize())
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) { Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) }
    }
}