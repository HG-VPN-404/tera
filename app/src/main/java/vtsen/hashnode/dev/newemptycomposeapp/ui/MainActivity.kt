package vtsen.hashnode.dev.newemptycomposeapp.ui

import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// --- KONFIGURASI ---
const val WORKER_URL = "https://sky.publicxx.workers.dev"

// --- WARNA SAAS / CORPORATE ---
val PrimaryBlue = Color(0xFF2563EB)   // Royal Blue
val DarkSlate = Color(0xFF0F172A)     // Background Header
val SurfaceBg = Color(0xFFF1F5F9)     // Light Gray Background
val TextPrimary = Color(0xFF1E293B)   // Dark Text
val TextSecondary = Color(0xFF64748B) // Gray Text

// --- DATA MODEL ---
data class ApiResponse(val status: String, val total_items: Int, val data: List<FileItem>)
data class FileItem(val filename: String, val size_mb: String?, val is_folder: Boolean?, val thumb: String?, val links: Links?)
data class Links(val proxy: String?, val browse: String?)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = PrimaryBlue,
                    background = SurfaceBg,
                    surface = Color.White
                )
            ) {
                // Set status bar color logic here if needed
                HomeScreen()
            }
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

    // --- LOGIC FUNCTIONS ---
    fun fetchData(targetUrl: String) {
        isLoading = true
        statusMsg = "Memuat data..."
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
                            statusMsg = "Menampilkan ${result.total_items} file"
                            if (historyStack.isEmpty() || historyStack.last() != targetUrl) {
                                historyStack = historyStack + targetUrl
                            }
                        } else statusMsg = "Gagal memuat data API"
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { statusMsg = "Error koneksi: ${e.message}"; isLoading = false }
            }
        }
    }

    fun goBack() {
        if (historyStack.size > 1) {
            val newStack = historyStack.dropLast(1)
            historyStack = newStack
            fetchData(newStack.last())
        } else {
            fileList = emptyList()
            historyStack = emptyList()
            statusMsg = ""
        }
    }

    BackHandler(enabled = historyStack.isNotEmpty() || videoUrlToPlay != null) {
        if (videoUrlToPlay != null) videoUrlToPlay = null else goBack()
    }

    fun downloadFile(url: String, filename: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(filename)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        Toast.makeText(context, "Download dimulai...", Toast.LENGTH_SHORT).show()
    }

    // --- UI LAYOUT ---
    if (videoUrlToPlay != null) {
        VideoPlayerScreen(url = videoUrlToPlay!!) { videoUrlToPlay = null }
    } else {
        Scaffold(
            containerColor = SurfaceBg,
            topBar = {
                // Custom Header Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp) // Header lebih tinggi ala dashboard
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(DarkSlate, Color(0xFF334155))
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (historyStack.size > 1) {
                            IconButton(onClick = { goBack() }) {
                                Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White)
                            }
                        } else {
                            // Logo Placeholder
                            Icon(Icons.Outlined.CloudDownload, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = "Terabox Player",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if(fileList.isNotEmpty()) "File Manager" else "Selamat Datang",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                
                // INPUT SECTION (Floating Card style)
                if (fileList.isEmpty() && !isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text(
                                    "Mulai Streaming",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Tempel link folder atau file Terabox kamu di bawah ini untuk memulai.",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                OutlinedTextField(
                                    value = urlInput, 
                                    onValueChange = { urlInput = it }, 
                                    label = { Text("https://terabox.com/s/...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            urlInput = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                        }) {
                                            Icon(Icons.Default.ContentPaste, "Paste", tint = PrimaryBlue)
                                        }
                                    }
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Button(
                                    onClick = { fetchData("$WORKER_URL/?url=$urlInput") },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Icon(Icons.Default.RocketLaunch, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Proses Link", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // LOADING STATE
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                
                // LIST FILE SECTION
                if (fileList.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusMsg,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 20.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(fileList) { item ->
                            val isFolder = item.is_folder == true
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = isFolder) {
                                        if (isFolder && item.links?.browse != null) fetchData(item.links.browse)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // ICON CONTAINER
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isFolder) Color(0xFFFFF7ED) else Color(0xFFEFF6FF)), // Orange tint vs Blue tint
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isFolder) {
                                            Icon(Icons.Outlined.Folder, null, tint = Color(0xFFF97316), modifier = Modifier.size(26.dp))
                                        } else {
                                            // Thumbnail or File Icon
                                            if (item.thumb != null) {
                                                AsyncImage(
                                                    model = item.thumb,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            } else {
                                                Icon(Icons.Outlined.InsertDriveFile, null, tint = PrimaryBlue, modifier = Modifier.size(26.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    // TEXT INFO
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.filename,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            maxLines = 2
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isFolder) "Folder" else "${item.size_mb} MB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    
                                    // ACTION BUTTONS
                                    if (!isFolder && item.links?.proxy != null) {
                                        Row {
                                            IconButton(onClick = { videoUrlToPlay = item.links.proxy }) { 
                                                Icon(Icons.Default.PlayCircle, "Play", tint = PrimaryBlue, modifier = Modifier.size(28.dp)) 
                                            }
                                            IconButton(onClick = { downloadFile(item.links.proxy, item.filename) }) { 
                                                Icon(Icons.Default.DownloadForOffline, "Download", tint = TextSecondary, modifier = Modifier.size(24.dp)) 
                                            }
                                        }
                                    } else if (isFolder) {
                                        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- VIDEO PLAYER (Tetap sama fungsionalnya) ---
@Composable
fun VideoPlayerScreen(url: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(url)); prepare(); playWhenReady = true } }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { PlayerView(context).apply { player = exoPlayer } }, modifier = Modifier.fillMaxSize())
        
        // Custom Back Button Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha=0.7f), Color.Transparent)))
                .padding(16.dp)
        ) {
            IconButton(onClick = onClose) { 
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White) 
            }
        }
    }
}