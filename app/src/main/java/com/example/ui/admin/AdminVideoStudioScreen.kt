package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.VideoJob
import com.example.data.model.Product
import com.example.data.remote.VideoRepository
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun AdminVideoStudioScreen(viewModel: MainViewModel) {
    val products by viewModel.products.collectAsState()
    val repo = remember { VideoRepository(viewModel.repository.supabaseClient) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var jobs by remember { mutableStateOf<List<VideoJob>>(emptyList()) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { jobs = repo.listJobs() }.onFailure { message = it.message }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("AI Product Video Studio", style = MaterialTheme.typography.headlineSmall)
            Text("Create a real product video job. Provider credentials stay server-side.", style = MaterialTheme.typography.bodySmall)
        }
        item {
            Text("Product", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                products.take(8).forEach { product ->
                    FilterChip(
                        selected = selectedProduct?.id == product.id,
                        onClick = { selectedProduct = product },
                        label = { Text(product.name.take(18)) }
                    )
                }
            }
        }
        item {
            Button(
                enabled = selectedProduct != null && !busy,
                onClick = {
                    val p = selectedProduct ?: return@Button
                    scope.launch {
                        busy = true
                        message = null
                        runCatching {
                            val prompt = "Create a premium product-focused ecommerce video for " + p.name +
                                ". Use only the supplied product facts. Show benefits clearly, avoid fake claims, fake reviews, or copyrighted material."
                            repo.createJob(p.id, p.name + " — Product Video", p.shortDescription ?: p.description, prompt)
                        }.onSuccess {
                            jobs = repo.listJobs()
                            message = "Video job queued."
                        }.onFailure { message = it.message }
                        busy = false
                    }
                }
            ) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text(if (busy) "Working…" else "Queue AI Video")
            }
        }
        message?.let { msg -> item { Text(msg, color = MaterialTheme.colorScheme.primary) } }

        item { Text("Video Library / Queue", style = MaterialTheme.typography.titleMedium) }
        items(jobs, key = { it.id }) { job ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(job.title, style = MaterialTheme.typography.titleMedium)
                    Text(job.generationStatus.uppercase(), style = MaterialTheme.typography.labelSmall)
                    if (!job.errorMessage.isNullOrBlank()) Text(job.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (job.generationStatus == "queued" || job.generationStatus == "failed") {
                            OutlinedButton(enabled = !busy, onClick = {
                                scope.launch {
                                    busy = true
                                    runCatching { repo.generate(job.id) }
                                        .onSuccess { message = "Generation submitted."; jobs = repo.listJobs() }
                                        .onFailure { message = it.message }
                                    busy = false
                                }
                            }) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Generate")
                            }
                        }
                        if (!job.videoUrl.isNullOrBlank()) {
                            OutlinedButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(job.videoUrl)))
                            }) { Text("Preview") }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    busy = true
                                    runCatching { repo.publish(job.id, !job.isPublic) }
                                        .onSuccess { jobs = repo.listJobs(); message = if (job.isPublic) "Unpublished." else "Published to customer library." }
                                        .onFailure { message = it.message }
                                    busy = false
                                }
                            }) {
                                Icon(Icons.Default.Public, null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (job.isPublic) "Unpublish" else "Publish")
                            }
                        }
                    }
                }
            }
        }
    }
}
