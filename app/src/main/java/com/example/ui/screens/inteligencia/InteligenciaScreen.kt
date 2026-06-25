package com.example.ui.screens.inteligencia

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import com.example.MainViewModel
import com.example.data.MockData
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun InteligenciaScreen(
    viewModel: MainViewModel,
    intelligenceDao: com.example.data.intelligence.IntelligenceDao?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    var currentFeature by remember { mutableStateOf<IntelligenceFeature?>(null) }

    with(sharedTransitionScope) {
        Box(
            modifier = Modifier
                .sharedBounds(
                    rememberSharedContentState(key = "inteligencia_container"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .fillMaxSize()
        ) {
            if (currentFeature == null) {
                IntelligenceHub(onNavigate = { currentFeature = it })
            } else {
                IntelligenceFeatureDetail(
                    feature = currentFeature!!,
                    onBack = { currentFeature = null },
                    viewModel = viewModel,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }
    }
}

enum class IntelligenceFeature(val title: String, val icon: ImageVector) {
    BIBLIOTECA("Biblioteca Clínica", Icons.Default.Book),
    SIMULADOR("Simulador de Casos", Icons.Default.Science),
    FLASHCARDS("Flashcards", Icons.Default.Dashboard),
    PDFS("PDFs & Livros", Icons.Default.PictureAsPdf),
    ESTUDOS("Estudos Científicos", Icons.Default.School),
    PESQUISA("Pesquisa Web Curada", Icons.Default.Public)
}

@Composable
fun IntelligenceHub(onNavigate: (IntelligenceFeature) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Hub de Inteligência Clínica", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(IntelligenceFeature.entries) { feature ->
                Card(
                    modifier = Modifier.fillMaxWidth().height(120.dp).clickable { onNavigate(feature) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(feature.icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(feature.title, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun IntelligenceFeatureDetail(
    feature: IntelligenceFeature,
    onBack: () -> Unit,
    viewModel: MainViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    // Basic implementation of Detail screen structure
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(feature.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Conteúdo de: ${feature.title} será desenvolvido em breve.")
        }
    }
}
