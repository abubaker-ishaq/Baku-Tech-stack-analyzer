package com.example

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.content.ContentValues
import android.provider.MediaStore
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import android.widget.Toast
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.NumberFormat
import java.util.Locale

// Data structures
data class CompanyComparison(
    val metric: String,
    val before: String,
    val after: String
)

data class MigrationPlanStep(
    val days: String,
    val title: String,
    val detail: String
)

data class Company(
    val id: String,
    val name: String,
    val currentStack: String,
    val weakness: String,
    val costBefore: Int,
    val aiSolution: String,
    val costAfterAi: Int,
    val savingPercent: Double,
    val hiringLlm: Boolean,
    val llmTitle: String,
    val llmSalary: String,
    val careerLink: String,
    val comparison: List<CompanyComparison>,
    val migrationPlan: List<MigrationPlanStep>
)

data class TrackerTask(
    val id: String,
    val text: String,
    val phase: Int
)

val migrationTasks = listOf(
    TrackerTask("p1_db", "Migrate core database schemas to cloud-native PostgreSQL", 1),
    TrackerTask("p1_docker", "Containerize legacy backend services using Docker", 1),
    TrackerTask("p1_deploy", "Deploy base serverless APIs on cloud environment", 1),
    TrackerTask("p1_iam", "Implement centralized IAM and security policies", 1),
    TrackerTask("p1_monitor", "Setup monitoring and error tracking alerts", 1),

    TrackerTask("p2_copilot", "Integrate GitHub Copilot/AI coding assistants for team", 2),
    TrackerTask("p2_gemini", "Implement Gemini API backend router and orchestrator", 2),
    TrackerTask("p2_analytics", "Build LLM-powered data analytics pipeline", 2),
    TrackerTask("p2_qa", "Develop automated QA test generation suites", 2),
    TrackerTask("p2_search", "Deploy semantic search and custom embeddings index", 2),

    TrackerTask("p3_support", "Redirect 60% of legacy support tickets to AI customer agent", 3),
    TrackerTask("p3_perf", "Optimize serverless function execution times and cache layers", 3),
    TrackerTask("p3_deprecate", "Terminate/Deprecate legacy monolithic virtual machines", 3),
    TrackerTask("p3_regress", "Perform comprehensive AI-driven regression tests", 3),
    TrackerTask("p3_audit", "Conduct final executive handoff and cost optimization audit", 3)
)

// ViewModel
class CompanyViewModel : ViewModel() {
    private val _companies = MutableStateFlow<List<Company>>(emptyList())
    val companies: StateFlow<List<Company>> = _companies

    private val _selectedCompany = MutableStateFlow<Company?>(null)
    val selectedCompany: StateFlow<Company?> = _selectedCompany

    private val _isReportOpen = MutableStateFlow(false)
    val isReportOpen: StateFlow<Boolean> = _isReportOpen

    fun loadCompanies(context: Context) {
        val jsonString = try {
            context.assets.open("companies.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<Company>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                
                val compArray = obj.getJSONArray("comparison")
                val comparisonList = mutableListOf<CompanyComparison>()
                for (j in 0 until compArray.length()) {
                    val compObj = compArray.getJSONObject(j)
                    comparisonList.add(CompanyComparison(
                        metric = compObj.getString("metric"),
                        before = compObj.getString("before"),
                        after = compObj.getString("after")
                    ))
                }
                
                val planArray = obj.getJSONArray("migration_plan")
                val planList = mutableListOf<MigrationPlanStep>()
                for (j in 0 until planArray.length()) {
                    val planObj = planArray.getJSONObject(j)
                    planList.add(MigrationPlanStep(
                        days = planObj.getString("days"),
                        title = planObj.getString("title"),
                        detail = planObj.getString("detail")
                    ))
                }
                
                list.add(Company(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    currentStack = obj.getString("current_stack"),
                    weakness = obj.getString("weakness"),
                    costBefore = obj.getInt("cost_before"),
                    aiSolution = obj.getString("ai_solution"),
                    costAfterAi = obj.getInt("cost_after_ai"),
                    savingPercent = obj.getDouble("saving_percent"),
                    hiringLlm = obj.getBoolean("hiring_llm"),
                    llmTitle = obj.optString("llm_title", "LLM Engineer"),
                    llmSalary = obj.optString("llm_salary", "3,500 - 5,000 AZN"),
                    careerLink = obj.getString("career_link"),
                    comparison = comparisonList,
                    migrationPlan = planList
                ))
            }
            _companies.value = list
            if (list.isNotEmpty() && _selectedCompany.value == null) {
                _selectedCompany.value = list[0]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectCompany(company: Company) {
        _selectedCompany.value = company
    }

    fun setReportOpen(isOpen: Boolean) {
        _isReportOpen.value = isOpen
    }

    private val _checkedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val checkedTaskIds: StateFlow<Set<String>> = _checkedTaskIds

    fun loadMigrationTasks(context: Context) {
        val prefs = context.getSharedPreferences("baku_tracker_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("checked_tasks", emptySet()) ?: emptySet()
        _checkedTaskIds.value = saved
    }

    fun toggleTask(context: Context, taskId: String) {
        val current = _checkedTaskIds.value.toMutableSet()
        if (current.contains(taskId)) {
            current.remove(taskId)
        } else {
            current.add(taskId)
        }
        _checkedTaskIds.value = current
        
        val prefs = context.getSharedPreferences("baku_tracker_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("checked_tasks", current).apply()
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: CompanyViewModel = viewModel()
                val context = LocalContext.current
                
                LaunchedEffect(Unit) {
                    viewModel.loadCompanies(context)
                    viewModel.loadMigrationTasks(context)
                }
                
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    BakuTechStackDashboard(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                        onShowSnackbar = { message ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    )
                }
            }
        }
    }
}

fun Context.findMainActivity(): MainActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is MainActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return currentContext as? MainActivity
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BakuTechStackDashboard(
    viewModel: CompanyViewModel,
    modifier: Modifier = Modifier,
    onShowSnackbar: (String) -> Unit = {}
) {
    val companies by viewModel.companies.collectAsState()
    val selectedCompany by viewModel.selectedCompany.collectAsState()
    val isReportOpen by viewModel.isReportOpen.collectAsState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Glow Animation for "Hiring LLM Engineers" and visual accents
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    var activeTab by remember { mutableStateOf(DashboardTab.SCANNER) }
    var showCostOptimizer by remember { mutableStateOf(false) }
    var showAiRecommendations by remember { mutableStateOf(false) }
    var currentComparatorA by remember { mutableStateOf<Company?>(null) }
    var currentComparatorB by remember { mutableStateOf<Company?>(null) }
    var benchmarkReportCompanies by remember { mutableStateOf<Pair<Company, Company>?>(null) }

    SharedTransitionLayout {
        val localBlue = BlueAccent
        val localEmerald = EmeraldAccent
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBg)
            .drawBehind {
                // Add futuristic glowing radial gradients at the top right and center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(localBlue.copy(alpha = 0.08f), Color.Transparent),
                        radius = size.width * 0.8f
                    ),
                    center = this.center.copy(y = 0f),
                    radius = size.width * 0.8f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(localEmerald.copy(alpha = 0.05f), Color.Transparent),
                        radius = size.width * 0.6f
                    ),
                    center = this.center.copy(x = 0f, y = size.height * 0.6f),
                    radius = size.width * 0.6f
                )
            }
    ) {
        Scaffold(
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        val targetIndex = targetState.ordinal
                        val initialIndex = initialState.ordinal
                        if (targetIndex > initialIndex) {
                            slideInHorizontally(
                                animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)),
                                initialOffsetX = { it / 5 }
                            ) + fadeIn(animationSpec = tween(200)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(200, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)),
                                targetOffsetX = { -it / 5 }
                            ) + fadeOut(animationSpec = tween(150))
                        } else {
                            slideInHorizontally(
                                animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)),
                                initialOffsetX = { -it / 5 }
                            ) + fadeIn(animationSpec = tween(200)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(200, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)),
                                targetOffsetX = { it / 5 }
                            ) + fadeOut(animationSpec = tween(150))
                        }
                    },
                    label = "DashboardTabTransition"
                ) { tab ->
                    when (tab) {
                        DashboardTab.SCANNER -> {
                            WebsiteScannerScreen(
                                viewModel = viewModel,
                                onShowSnackbar = onShowSnackbar,
                                modifier = Modifier.fillMaxSize(),
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@AnimatedContent
                            )
                        }
                        DashboardTab.DASHBOARD -> {
                            ExecutiveDashboardScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        DashboardTab.COMPARATOR -> {
                            StackComparatorScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize(),
                                onSelectionChanged = { a, b ->
                                    currentComparatorA = a
                                    currentComparatorB = b
                                },
                                onGenerateReport = { a, b ->
                                    benchmarkReportCompanies = Pair(a, b)
                                }
                            )
                        }
                        DashboardTab.HISTORY -> {
                            HistoryScreen(
                                viewModel = viewModel,
                                onShowSnackbar = onShowSnackbar,
                                modifier = Modifier.fillMaxSize(),
                                onNavigateToScan = { activeTab = DashboardTab.SCANNER },
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@AnimatedContent
                            )
                        }
                        DashboardTab.TRACKER -> {
                            TrackerScreen(viewModel = viewModel)
                        }
                    }
                }

                // Dynamic Context-Aware Floating Action Button
                if (!isReportOpen && !showCostOptimizer) {
                    val (fabLabel, fabIcon, fabAction) = when (activeTab) {
                        DashboardTab.SCANNER -> {
                            Triple(
                                "Generate Executive Report",
                                Icons.Default.Assessment,
                                {
                                    val currentCompany = selectedCompany ?: companies.firstOrNull()
                                    if (currentCompany != null) {
                                        viewModel.selectCompany(currentCompany)
                                        viewModel.setReportOpen(true)
                                    } else {
                                        Toast.makeText(context, "No target company available", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        DashboardTab.DASHBOARD -> {
                            Triple(
                                "View AI Recommendations",
                                Icons.Default.Lightbulb,
                                {
                                    showAiRecommendations = true
                                }
                            )
                        }
                        DashboardTab.COMPARATOR -> {
                            Triple(
                                "Compare Companies",
                                Icons.Default.CompareArrows,
                                {
                                    if (currentComparatorA != null && currentComparatorB != null) {
                                        benchmarkReportCompanies = Pair(currentComparatorA!!, currentComparatorB!!)
                                    } else {
                                        val defaultA = companies.find { it.id == "kapital_bank" } ?: companies.firstOrNull()
                                        val defaultB = companies.find { it.id == "pasha_bank" } ?: companies.getOrNull(1)
                                        if (defaultA != null && defaultB != null) {
                                            benchmarkReportCompanies = Pair(defaultA, defaultB)
                                        } else {
                                            Toast.makeText(context, "Please select companies to compare", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                        DashboardTab.HISTORY -> {
                            Triple(
                                "Export Executive PDF",
                                Icons.Default.Description,
                                {
                                    val currentCompany = selectedCompany ?: companies.firstOrNull()
                                    if (currentCompany != null) {
                                        val file = generatePdfReport(context, currentCompany)
                                        if (file != null) {
                                            val savedUri = savePdfToDownloads(context, file, "CTO_Report_${currentCompany.name.replace(" ", "_")}.pdf")
                                            if (savedUri != null) {
                                                Toast.makeText(context, "Report PDF saved to Downloads", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Saved to cache directory", Toast.LENGTH_SHORT).show()
                                            }
                                            sharePdf(context, file)
                                        } else {
                                            Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "No company selected to export.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        DashboardTab.TRACKER -> {
                            Triple(
                                "View Strategic Action Plan",
                                Icons.Default.Explore,
                                {
                                    showCostOptimizer = true
                                }
                            )
                        }
                    }

                    ExtendedFloatingActionButton(
                        onClick = fabAction,
                        containerColor = GoldHighlight,
                        contentColor = Color.Black,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 24.dp, bottom = 104.dp)
                            .border(BorderStroke(1.dp, GoldHighlight.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
                            .testTag("dynamic_fab_${activeTab.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = fabIcon,
                            contentDescription = fabLabel,
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = fabLabel.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black
                        )
                    }
                }

                // Floating premium navigation bar
                if (!isReportOpen && !showCostOptimizer) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                            .widthIn(max = 480.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .shadow(
                                    elevation = 20.dp,
                                    shape = RoundedCornerShape(36.dp),
                                    clip = false,
                                    ambientColor = Color.Black.copy(alpha = 0.4f),
                                    spotColor = Color.Black.copy(alpha = 0.4f)
                                )
                                .background(
                                    color = DarkSurface,
                                    shape = RoundedCornerShape(36.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = CardBorderColor,
                                    shape = RoundedCornerShape(36.dp)
                                )
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val items = listOf(
                                DashboardTab.SCANNER to (Icons.Outlined.Assessment to "Assessment"),
                                DashboardTab.DASHBOARD to (Icons.Outlined.Analytics to "Intelligence"),
                                DashboardTab.COMPARATOR to (Icons.Outlined.CompareArrows to "Benchmark"),
                                DashboardTab.HISTORY to (Icons.Outlined.Description to "Reports"),
                                DashboardTab.TRACKER to (Icons.Outlined.Timeline to "Roadmap")
                            )

                            items.forEach { (tab, pair) ->
                                val isSelected = activeTab == tab
                                val (icon, label) = pair
                                val contentColor = if (isSelected) {
                                    if (isDarkThemeSystem) Color.White else Color.Black
                                } else {
                                    TextSecondary.copy(alpha = 0.65f)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(vertical = 6.dp, horizontal = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        // Premium glass active pill background
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(
                                                            if (isDarkThemeSystem) Color(0x18FFFFFF) else Color(0x0C000000),
                                                            if (isDarkThemeSystem) Color(0x0AFFFFFF) else Color(0x04000000)
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(26.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isDarkThemeSystem) Color(0x1AFFFFFF) else Color(0x0B000000),
                                                    shape = RoundedCornerShape(26.dp)
                                                )
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(26.dp))
                                            .clickable { activeTab = tab },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = contentColor,
                                            modifier = Modifier.size(if (isSelected) 25.dp else 22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = label,
                                            fontSize = 8.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = contentColor,
                                            letterSpacing = 0.2.sp,
                                            maxLines = 1,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CTO Report Modal Overlay with Shared Elements!
        AnimatedVisibility(
            visible = isReportOpen && selectedCompany != null,
            enter = fadeIn(animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))),
            exit = fadeOut(animationSpec = tween(250, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)))
        ) {
            if (selectedCompany != null) {
                CtoReportOverlay(
                    company = selectedCompany!!,
                    onDismiss = { viewModel.setReportOpen(false) },
                    onShowSnackbar = onShowSnackbar,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility
                )
            }
        }

        // Baku Competitive Technology Blueprint Overlay
        if (showCostOptimizer) {
            CompetitiveBlueprintScreen(
                onDismiss = { showCostOptimizer = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        // AI Recommendations Dialog Overlay
        if (showAiRecommendations) {
            val recCompany = selectedCompany ?: companies.firstOrNull()
            if (recCompany != null) {
                AiRecommendationsDialog(
                    company = recCompany,
                    onDismiss = { showAiRecommendations = false }
                )
            }
        }

        // Executive Benchmark Report Overlay
        AnimatedVisibility(
            visible = benchmarkReportCompanies != null,
            enter = fadeIn(animationSpec = tween(300, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f))),
            exit = fadeOut(animationSpec = tween(250, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)))
        ) {
            if (benchmarkReportCompanies != null) {
                BenchmarkReportOverlay(
                    companyA = benchmarkReportCompanies!!.first,
                    companyB = benchmarkReportCompanies!!.second,
                    onDismiss = { benchmarkReportCompanies = null },
                    onShowSnackbar = onShowSnackbar
                )
            }
        }
    }
}
}

@Composable
fun AiRecommendationsDialog(
    company: Company,
    onDismiss: () -> Unit
) {
    val recommendations = remember(company) { getStrategicRecommendations(company) }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AI RECOMMENDATIONS INDEX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldHighlight,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Strategic Modernisation for ${company.name}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            HorizontalDivider(color = CardBorderColor, modifier = Modifier.padding(vertical = 16.dp))

            // Content List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(recommendations) { rec ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(0.5.dp, CardBorderColor.copy(alpha = 0.5f)), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rec.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Surface(
                                    color = if (rec.priority == "High") RedCost.copy(alpha = 0.15f) else BlueAccent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = rec.priority.uppercase(),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rec.priority == "High") RedCost else BlueAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = rec.detail,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = "Impact",
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Impact: ${rec.impact}",
                                        fontSize = 11.sp,
                                        color = EmeraldAccent,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Text(
                                    text = "Est. Benefit: ${rec.expectedBenefit}",
                                    fontSize = 11.sp,
                                    color = GoldHighlight,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GoldHighlight, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("DISMISS INDEX", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
}

@Composable
fun CurrentStateCard(
    company: Company,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = RedCostBg
        ),
        border = BorderStroke(1.dp, RedCostBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(RedCost)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CURRENT STATE",
                        color = RedCost,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Monthly IT Spend",
                color = TextSecondary,
                fontSize = 11.sp
            )
            
            Text(
                text = formatCost(company.costBefore),
                color = RedCost,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Current Tech Stack",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = company.currentStack,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Strategic Optimization Gaps",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = company.weakness,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun OptimizedStateCard(
    company: Company,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = GreenCostBg
        ),
        border = BorderStroke(1.dp, GreenCostBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(GreenCost)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI OPTIMIZED STATE",
                        color = GreenCost,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                
                Surface(
                    color = Color(0x3310B981),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0x8010B981))
                ) {
                    Text(
                        text = "-${company.savingPercent}% COST",
                        color = GreenCost,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Monthly Optimized Spend",
                color = TextSecondary,
                fontSize = 11.sp
            )
            
            Text(
                text = formatCost(company.costAfterAi),
                color = GreenCost,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Proposed AI Architecture",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = company.aiSolution,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Expected Performance Shift",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Cost Reduction",
                    tint = GreenCost,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Immediate ${company.savingPercent}% cost containment via AI agent labor and serverless consolidation.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CtoReportOverlay(
    company: Company,
    onDismiss: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    with(sharedTransitionScope) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .sharedElement(
                    rememberSharedContentState(key = "card_${company.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                ),
            color = DarkBg
        ) {
            val context = LocalContext.current
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "CTO STRATEGY REPORT",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 2.sp,
                                modifier = Modifier.sharedElement(
                                    rememberSharedContentState(key = "title_${company.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("close_report_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextPrimary
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    val file = generatePdfReport(context, company)
                                    if (file != null) {
                                        val savedUri = savePdfToDownloads(context, file, "CTO_Report_${company.name.replace(" ", "_")}.pdf")
                                        if (savedUri != null) {
                                            onShowSnackbar("Report PDF saved to Downloads")
                                        } else {
                                            Toast.makeText(context, "Saved to cache directory", Toast.LENGTH_SHORT).show()
                                        }
                                        sharePdf(context, file)
                                    } else {
                                        Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("appbar_export_pdf_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export PDF",
                                    tint = GoldHighlight
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = TextPrimary
                        )
                    )
                },
                bottomBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // PDF Button
                            Button(
                                onClick = {
                                    val file = generatePdfReport(context, company)
                                    if (file != null) {
                                        val savedUri = savePdfToDownloads(context, file, "CTO_Report_${company.name.replace(" ", "_")}.pdf")
                                        if (savedUri != null) {
                                            onShowSnackbar("Report PDF saved to Downloads")
                                        } else {
                                            Toast.makeText(context, "Saved to cache directory", Toast.LENGTH_SHORT).show()
                                        }
                                        sharePdf(context, file)
                                    } else {
                                        Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("export_pdf_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BlueAccent,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "PDF",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Excel Button
                            Button(
                                onClick = {
                                    val uri = ReportExporter.exportExcelReport(context, company)
                                    if (uri != null) {
                                        onShowSnackbar("Report Excel saved to Downloads")
                                    } else {
                                        Toast.makeText(context, "Exported successfully", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("export_excel_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldAccent,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Excel",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Excel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Share Button
                            Button(
                                onClick = {
                                    ReportExporter.shareReport(context, company)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("share_report_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldHighlight,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Header Details
                    item {
                        Column {
                            Text(
                                text = company.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BlueAccent
                            )
                            Text(
                                text = "AI optimization roadmap, feasibility comparison, and cost savings analysis.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            // High-prominence, fully visible PDF button directly below Executive Summary header
                            Button(
                                onClick = {
                                    val file = generatePdfReport(context, company)
                                    if (file != null) {
                                        val savedUri = savePdfToDownloads(context, file, "CTO_Report_${company.name.replace(" ", "_")}.pdf")
                                        if (savedUri != null) {
                                            onShowSnackbar("Report PDF saved to Downloads")
                                        } else {
                                            Toast.makeText(context, "Saved to cache directory", Toast.LENGTH_SHORT).show()
                                        }
                                        sharePdf(context, file)
                                    } else {
                                        Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("executive_summary_download_pdf_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldHighlight,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export Executive PDF",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Export Executive PDF", 
                                    fontSize = 14.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0x33FFFFFF), thickness = 1.dp)
                        }
                    }

                    // Cost Savings Summary Box
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard(cornerRadius = 16.dp)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "PROJECTED SAVINGS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCost(company.costBefore - company.costAfterAi) + " / mo",
                                    color = GreenCost,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = GreenCost.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, GreenCost.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${company.savingPercent}% Saved",
                                    color = GreenCost,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Comparison Table Section
                    item {
                        Column {
                            Text(
                                text = "COST BREAKDOWN & COMPARISON",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurfaceElevated, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "METRIC", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(text = "LEGACY STATE", color = RedCost, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                Text(text = "AI RE-ARCH", color = GreenCost, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                            }

                            // Table Rows
                            company.comparison.forEachIndexed { index, row ->
                                val background = if (index % 2 == 0) DarkSurface else Color.Transparent
                                val borderBottom = if (index == company.comparison.size - 1) RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp) else RoundedCornerShape(0.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(background, borderBottom)
                                        .border(BorderStroke(0.5.dp, Color(0x1AFFFFFF)))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = row.metric, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Text(text = row.before, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                    Text(text = row.after, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                }
                            }
                        }
                    }

                    // 90-Day Migration Roadmap
                    item {
                        Column {
                            Text(
                                text = "90-DAY AI MIGRATION ROADMAP",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Interactive Timeline List
                            company.migrationPlan.forEachIndexed { index, step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    // Left Timeline indicator column
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(48.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(BlueAccentBg)
                                                .border(BorderStroke(2.dp, BlueAccent), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (index + 1).toString(),
                                                color = BlueAccent,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (index < company.migrationPlan.size - 1) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(60.dp)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(BlueAccent, BlueAccentBg)
                                                        )
                                                    )
                                            )
                                        }
                                    }

                                    // Right roadmap card
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .glassCard(cornerRadius = 12.dp)
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = step.title,
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                color = BlueAccentBg,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = step.days,
                                                    color = BlueAccent,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = step.detail,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Disclaimer / Note
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassCard(cornerRadius = 12.dp)
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Info Disclaimer",
                                    tint = BlueAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "This strategy report acts as a feasibility charter. Real integration is customizable to company APIs, strict banking security guidelines, and internal compliance.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Format cost to nice USD currency
fun formatCost(amount: Int): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    format.maximumFractionDigits = 0
    return format.format(amount)
}

fun generatePdfReport(context: Context, company: Company): File? {
    val pdfDocument = PdfDocument()
    
    val pageWidth = 595
    val pageHeight = 842
    
    val bgPaint = Paint().apply {
        color = 0xFF050505.toInt() // Matches DarkBg luxury black
        style = Paint.Style.FILL
    }
    
    val bluePaint = Paint().apply {
        color = 0xFFE5E7EB.toInt() // Clean Silver/Neutral Gray
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    val blueBorderPaint = Paint().apply {
        color = 0xFF94A3B8.toInt() // Clean Muted Slate
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }
    
    val greenPaint = Paint().apply {
        color = 0xFF5E8C71.toInt() // Matches Muted Emerald
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    val redPaint = Paint().apply {
        color = 0xFFC28E75.toInt() // Matches Muted copper/terracotta
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    val whitePaint = Paint().apply {
        color = 0xFFE5E5E5.toInt() // Soft White
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    val grayPaint = Paint().apply {
        color = 0xFF94A3B8.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    val cardBgPaint = Paint().apply {
        color = 0x1FFFFFFF
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    val cardBorderPaint = Paint().apply {
        color = 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }
    
    val linePaint = Paint().apply {
        color = 0x33FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }

    // --- PAGE 1 ---
    val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page1 = pdfDocument.startPage(pageInfo1)
    val canvas1 = page1.canvas
    
    canvas1.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)
    
    var currentY = 50f
    
    val badgeText = "BAKU TECH INTELLIGENCE"
    val badgePaint = Paint().apply {
        color = 0xFFF1F5F9.toInt() // Premium light silver
        textSize = 10f
        isFakeBoldText = true
        isAntiAlias = true
    }
    val badgeWidth = badgePaint.measureText(badgeText)
    val badgeRectLeft = 40f
    val badgeRectTop = currentY
    val badgeRectRight = badgeRectLeft + badgeWidth + 16f
    val badgeRectBottom = badgeRectTop + 22f
    
    val badgeBgPaint = Paint().apply {
        color = 0x1A64748B // Slate grey with transparency
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas1.drawRoundRect(badgeRectLeft, badgeRectTop, badgeRectRight, badgeRectBottom, 6f, 6f, badgeBgPaint)
    canvas1.drawRoundRect(badgeRectLeft, badgeRectTop, badgeRectRight, badgeRectBottom, 6f, 6f, blueBorderPaint)
    canvas1.drawText(badgeText, badgeRectLeft + 8f, badgeRectTop + 15f, badgePaint)
    
    currentY += 45f
    
    val titlePaint = Paint().apply {
        color = 0xFFF1F5F9.toInt()
        textSize = 24f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas1.drawText("Save 70% IT Cost via AI", 40f, currentY, titlePaint)
    
    currentY += 22f
    
    val subtitlePaint = Paint().apply {
        color = 0xFF94A3B8.toInt()
        textSize = 12f
        isAntiAlias = true
    }
    canvas1.drawText("CTO STRATEGY REPORT", 40f, currentY, subtitlePaint)
    
    currentY += 30f
    
    canvas1.drawLine(40f, currentY, (pageWidth - 40).toFloat(), currentY, linePaint)
    
    currentY += 30f
    
    val compNamePaint = Paint().apply {
        color = 0xFFF1F5F9.toInt() // Premium titanium white
        textSize = 20f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas1.drawText(company.name, 40f, currentY, compNamePaint)
    
    currentY += 18f
    
    val descPaint = Paint().apply {
        color = 0xFF94A3B8.toInt()
        textSize = 10f
        isAntiAlias = true
    }
    canvas1.drawText("AI optimization roadmap, feasibility comparison, and cost savings analysis.", 40f, currentY, descPaint)
    
    currentY += 35f
    
    val cardWidth = (pageWidth - 80 - 16) / 2f
    val cardHeight = 140f
    val leftCardX = 40f
    val rightCardX = 40f + cardWidth + 16f
    
    val redCardBgPaint = Paint().apply {
        color = 0x0DEF4444
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    val redCardBorderPaint = Paint().apply {
        color = 0x33EF4444
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }
    
    canvas1.drawRoundRect(leftCardX, currentY, leftCardX + cardWidth, currentY + cardHeight, 16f, 16f, redCardBgPaint)
    canvas1.drawRoundRect(leftCardX, currentY, leftCardX + cardWidth, currentY + cardHeight, 16f, 16f, redCardBorderPaint)
    
    val cardHeaderPaintRed = Paint().apply {
        color = 0xFFEF4444.toInt()
        textSize = 10f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas1.drawText("CURRENT STATE", leftCardX + 16f, currentY + 24f, cardHeaderPaintRed)
    
    val cardLabelPaint = Paint().apply {
        color = 0xFF64748B.toInt()
        textSize = 9f
        isAntiAlias = true
    }
    val cardValuePaint = Paint().apply {
        color = 0xFFF1F5F9.toInt()
        textSize = 11f
        isFakeBoldText = true
        isAntiAlias = true
    }
    
    canvas1.drawText("Legacy Stack", leftCardX + 16f, currentY + 45f, cardLabelPaint)
    var stackText = company.currentStack
    if (stackText.length > 25) stackText = stackText.substring(0, 22) + "..."
    canvas1.drawText(stackText, leftCardX + 16f, currentY + 60f, cardValuePaint)
    
    canvas1.drawText("Optimization Gap", leftCardX + 16f, currentY + 80f, cardLabelPaint)
    var riskText = company.weakness
    if (riskText.length > 25) riskText = riskText.substring(0, 22) + "..."
    canvas1.drawText(riskText, leftCardX + 16f, currentY + 95f, cardValuePaint)
    
    canvas1.drawText("OpEx / Month", leftCardX + 16f, currentY + 115f, cardLabelPaint)
    canvas1.drawText(formatCost(company.costBefore), leftCardX + 16f, currentY + 130f, Paint().apply {
        color = 0xFFEF4444.toInt()
        textSize = 14f
        isFakeBoldText = true
        isAntiAlias = true
    })
    
    val greenCardBgPaint = Paint().apply {
        color = 0x0D5E8C71 // Matches Muted Emerald with opacity
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    val greenCardBorderPaint = Paint().apply {
        color = 0x335E8C71 // Matches Muted Emerald border
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }
    
    canvas1.drawRoundRect(rightCardX, currentY, rightCardX + cardWidth, currentY + cardHeight, 16f, 16f, greenCardBgPaint)
    canvas1.drawRoundRect(rightCardX, currentY, rightCardX + cardWidth, currentY + cardHeight, 16f, 16f, greenCardBorderPaint)
    
    val cardHeaderPaintGreen = Paint().apply {
        color = 0xFF5E8C71.toInt() // Matches Muted Emerald
        textSize = 10f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas1.drawText("AI OPTIMIZED", rightCardX + 16f, currentY + 24f, cardHeaderPaintGreen)
    
    canvas1.drawText("Solution", rightCardX + 16f, currentY + 45f, cardLabelPaint)
    var solText = company.aiSolution
    if (solText.length > 25) solText = solText.substring(0, 22) + "..."
    canvas1.drawText(solText, rightCardX + 16f, currentY + 60f, cardValuePaint)
    
    canvas1.drawText("Savings Detail", rightCardX + 16f, currentY + 80f, cardLabelPaint)
    canvas1.drawText("${company.savingPercent}% Cost Reduction", rightCardX + 16f, currentY + 95f, Paint().apply {
        color = 0xFF5E8C71.toInt() // Muted Emerald
        textSize = 11f
        isFakeBoldText = true
        isAntiAlias = true
    })
    
    canvas1.drawText("Target OpEx / Month", rightCardX + 16f, currentY + 115f, cardLabelPaint)
    canvas1.drawText(formatCost(company.costAfterAi), rightCardX + 16f, currentY + 130f, Paint().apply {
        color = 0xFF5E8C71.toInt() // Muted Emerald
        textSize = 14f
        isFakeBoldText = true
        isAntiAlias = true
    })
    
    currentY += cardHeight + 35f
    
    canvas1.drawText("COST BREAKDOWN & COMPARISON", 40f, currentY, Paint().apply {
        color = 0xFFF1F5F9.toInt()
        textSize = 12f
        isFakeBoldText = true
        isAntiAlias = true
    })
    
    currentY += 15f
    
    val tableHeaderPaint = Paint().apply {
        color = 0xFFF1F5F9.toInt()
        textSize = 9f
        isFakeBoldText = true
        isAntiAlias = true
    }
    
    val tableY = currentY
    val tableHeaderHeight = 24f
    canvas1.drawRect(40f, tableY, (pageWidth - 40).toFloat(), tableY + tableHeaderHeight, cardBgPaint)
    canvas1.drawRect(40f, tableY, (pageWidth - 40).toFloat(), tableY + tableHeaderHeight, cardBorderPaint)
    
    canvas1.drawText("METRIC", 52f, tableY + 15f, tableHeaderPaint)
    canvas1.drawText("LEGACY STATE", (pageWidth - 230).toFloat(), tableY + 15f, Paint(tableHeaderPaint).apply { color = 0xFFC28E75.toInt() }) // Muted copper/terracotta
    canvas1.drawText("AI RE-ARCH", (pageWidth - 110).toFloat(), tableY + 15f, Paint(tableHeaderPaint).apply { color = 0xFF5E8C71.toInt() }) // Muted Emerald
    
    currentY += tableHeaderHeight
    
    val rowHeight = 24f
    company.comparison.forEachIndexed { idx, row ->
        val rowBg = if (idx % 2 == 0) cardBgPaint else bgPaint
        canvas1.drawRect(40f, currentY, (pageWidth - 40).toFloat(), currentY + rowHeight, rowBg)
        canvas1.drawRect(40f, currentY, (pageWidth - 40).toFloat(), currentY + rowHeight, cardBorderPaint)
        
        canvas1.drawText(row.metric, 52f, currentY + 15f, Paint().apply {
            color = 0xFFF1F5F9.toInt()
            textSize = 9f
            isAntiAlias = true
        })
        canvas1.drawText(row.before, (pageWidth - 230).toFloat(), currentY + 15f, Paint().apply {
            color = 0xFF94A3B8.toInt()
            textSize = 9f
            isAntiAlias = true
        })
        canvas1.drawText(row.after, (pageWidth - 110).toFloat(), currentY + 15f, Paint().apply {
            color = 0xFFF1F5F9.toInt()
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        })
        
        currentY += rowHeight
    }
    
    val footerPaint = Paint().apply {
        color = 0x6694A3B8
        textSize = 8f
        isAntiAlias = true
    }
    canvas1.drawText("Baku Tech Intelligence | Page 1 of 2", 40f, 810f, footerPaint)
    
    pdfDocument.finishPage(page1)

    // --- PAGE 2 ---
    val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
    val page2 = pdfDocument.startPage(pageInfo2)
    val canvas2 = page2.canvas
    
    canvas2.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)
    
    currentY = 50f
    
    canvas2.drawText("90-DAY AI MIGRATION ROADMAP", 40f, currentY, Paint().apply {
        color = 0xFFF1F5F9.toInt()
        textSize = 14f
        isFakeBoldText = true
        isAntiAlias = true
    })
    
    currentY += 15f
    canvas2.drawText("A structured timeline for re-architecting engineering, workflows, and automated agents.", 40f, currentY, descPaint)
    
    currentY += 30f
    
    company.migrationPlan.forEachIndexed { idx, step ->
        val circleX = 55f
        val circleY = currentY + 25f
        val radius = 12f
        
        if (idx < company.migrationPlan.size - 1) {
            val linePaintTimeline = Paint().apply {
                color = 0xFF64748B.toInt() // Modern Muted Slate
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas2.drawLine(circleX, circleY + radius, circleX, circleY + 80f, linePaintTimeline)
        }
        
        canvas2.drawCircle(circleX, circleY, radius, bluePaint)
        
        val stepNumPaint = Paint().apply {
            color = 0xFF09090B.toInt()
            textSize = 10f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas2.drawText((idx + 1).toString(), circleX, circleY + 3.5f, stepNumPaint)
        
        val cardX = 85f
        val cardY = currentY
        val cardW = pageWidth - cardX - 40f
        val cardH = 75f
        
        canvas2.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 12f, 12f, cardBgPaint)
        canvas2.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 12f, 12f, cardBorderPaint)
        
        canvas2.drawText(step.title, cardX + 16f, cardY + 24f, Paint().apply {
            color = 0xFFF1F5F9.toInt()
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        })
        
        val daysText = step.days
        val daysPaint = Paint().apply {
            color = 0xFFF1F5F9.toInt() // Premium light silver
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val daysWidth = daysPaint.measureText(daysText)
        val daysLeft = cardX + cardW - daysWidth - 24f
        val daysTop = cardY + 12f
        
        canvas2.drawRoundRect(daysLeft, daysTop, daysLeft + daysWidth + 12f, daysTop + 16f, 4f, 4f, badgeBgPaint)
        canvas2.drawText(daysText, daysLeft + 6f, daysTop + 11f, daysPaint)
        
        val detailPaint = Paint().apply {
            color = 0xFF94A3B8.toInt()
            textSize = 9f
            isAntiAlias = true
        }
        
        val words = step.detail.split(" ")
        var line = ""
        var lineY = cardY + 45f
        val maxWidth = cardW - 32f
        
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val testWidth = detailPaint.measureText(testLine)
            if (testWidth > maxWidth) {
                canvas2.drawText(line, cardX + 16f, lineY, detailPaint)
                line = word
                lineY += 12f
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty()) {
            canvas2.drawText(line, cardX + 16f, lineY, detailPaint)
        }
        
        currentY += cardH + 15f
    }
    
    currentY += 10f
    
    val discX = 40f
    val discY = currentY
    val discW = (pageWidth - 80).toFloat()
    val discH = 55f
    
    canvas2.drawRoundRect(discX, discY, discX + discW, discY + discH, 12f, 12f, badgeBgPaint)
    canvas2.drawRoundRect(discX, discY, discX + discW, discY + discH, 12f, 12f, blueBorderPaint)
    
    val discTextPaint = Paint().apply {
        color = 0xFF94A3B8.toInt()
        textSize = 8f
        isAntiAlias = true
    }
    
    canvas2.drawText("DISCLAIMER:", discX + 16f, discY + 18f, Paint().apply {
        color = 0xFF64748B.toInt() // Modern Muted Slate Grey
        textSize = 8f
        isFakeBoldText = true
        isAntiAlias = true
    })
    
    val disclaimerText = "This strategy report acts as a feasibility charter. Real integration is customizable to company APIs, strict banking security guidelines, and internal compliance."
    val wordsDisc = disclaimerText.split(" ")
    var lineDisc = ""
    var lineDiscY = discY + 30f
    for (word in wordsDisc) {
        val testLine = if (lineDisc.isEmpty()) word else "$lineDisc $word"
        val testWidth = discTextPaint.measureText(testLine)
        if (testWidth > discW - 32f) {
            canvas2.drawText(lineDisc, discX + 16f, lineDiscY, discTextPaint)
            lineDisc = word
            lineDiscY += 10f
        } else {
            lineDisc = testLine
        }
    }
    if (lineDisc.isNotEmpty()) {
        canvas2.drawText(lineDisc, discX + 16f, lineDiscY, discTextPaint)
    }
    
    canvas2.drawText("Baku Tech Intelligence | Page 2 of 2", 40f, 810f, footerPaint)
    
    pdfDocument.finishPage(page2)
    
    val cacheFile = File(context.cacheDir, "cto_report_${company.id}.pdf")
    try {
        FileOutputStream(cacheFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return cacheFile
    } catch (e: Exception) {
        e.printStackTrace()
        pdfDocument.close()
    }
    
    return null
}

fun savePdfToDownloads(context: Context, pdfFile: File, fileName: String): android.net.Uri? {
    val resolver = context.contentResolver
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    pdfFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                return uri
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    } else {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetFile = File(downloadsDir, fileName)
        try {
            pdfFile.inputStream().use { inputStream ->
                targetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            android.media.MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf("application/pdf"), null)
            return android.net.Uri.fromFile(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}

fun generateBenchmarkPdfReport(context: Context, companyA: Company, companyB: Company): File? {
    val pdfDocument = PdfDocument()
    
    val pageWidth = 595
    val pageHeight = 842
    
    val bgPaint = Paint().apply {
        color = 0xFF050505.toInt()
        style = Paint.Style.FILL
    }
    
    val bluePaint = Paint().apply {
        color = 0xFFE5E7EB.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    val blueBorderPaint = Paint().apply {
        color = 0xFF94A3B8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }
    
    val whitePaint = Paint().apply {
        color = 0xFFE5E5E5.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    val grayPaint = Paint().apply {
        color = 0xFF94A3B8.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    val cardBgPaint = Paint().apply {
        color = 0x1FFFFFFF
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    val cardBorderPaint = Paint().apply {
        color = 0x1AFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }
    
    val linePaint = Paint().apply {
        color = 0x33FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }

    val scoreA = TechStackScanner.calculateReadinessScore(companyA)
    val scoreB = TechStackScanner.calculateReadinessScore(companyB)
    val winner = if (scoreA >= scoreB) companyA else companyB
    val loser = if (winner.id == companyA.id) companyB else companyA

    // --- PAGE 1 ---
    val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page1 = pdfDocument.startPage(pageInfo1)
    val canvas1 = page1.canvas
    
    canvas1.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)
    
    var currentY = 50f
    
    val badgeText = "BAKU TECH BENCHMARK"
    val badgePaint = Paint().apply {
        color = 0xFFF1F5F9.toInt()
        textSize = 10f
        isFakeBoldText = true
        isAntiAlias = true
    }
    val badgeWidth = badgePaint.measureText(badgeText)
    val badgeRectLeft = 40f
    val badgeRectTop = currentY
    val badgeRectRight = badgeRectLeft + badgeWidth + 16f
    val badgeRectBottom = badgeRectTop + 22f
    
    val badgeBgPaint = Paint().apply {
        color = 0x1A64748B
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas1.drawRoundRect(badgeRectLeft, badgeRectTop, badgeRectRight, badgeRectBottom, 6f, 6f, badgeBgPaint)
    canvas1.drawRoundRect(badgeRectLeft, badgeRectTop, badgeRectRight, badgeRectBottom, 6f, 6f, blueBorderPaint)
    canvas1.drawText(badgeText, badgeRectLeft + 8f, badgeRectTop + 15f, badgePaint)
    
    currentY += 45f
    
    val titlePaint = Paint().apply {
        color = 0xFFF1F5F9.toInt()
        textSize = 24f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas1.drawText("Corporate Tech Benchmark", 40f, currentY, titlePaint)
    
    currentY += 22f
    
    val subtitlePaint = Paint().apply {
        color = 0xFF94A3B8.toInt()
        textSize = 12f
        isAntiAlias = true
    }
    canvas1.drawText("EXECUTIVE COMPARATIVE ANALYSIS", 40f, currentY, subtitlePaint)
    
    currentY += 30f
    canvas1.drawLine(40f, currentY, (pageWidth - 40).toFloat(), currentY, linePaint)
    currentY += 30f
    
    // Profiles
    val compNamePaint = Paint().apply {
        color = 0xFFF1F5F9.toInt()
        textSize = 18f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas1.drawText("${companyA.name} vs ${companyB.name}", 40f, currentY, compNamePaint)
    
    currentY += 18f
    val descPaint = Paint().apply {
        color = 0xFF94A3B8.toInt()
        textSize = 10f
        isAntiAlias = true
    }
    canvas1.drawText("Modernization and capability gap analysis between the target organizations.", 40f, currentY, descPaint)
    
    currentY += 35f
    
    // Executive Summary Card
    val summaryCardH = 100f
    val summaryCardW = (pageWidth - 80).toFloat()
    canvas1.drawRoundRect(40f, currentY, 40f + summaryCardW, currentY + summaryCardH, 12f, 12f, cardBgPaint)
    canvas1.drawRoundRect(40f, currentY, 40f + summaryCardW, currentY + summaryCardH, 12f, 12f, cardBorderPaint)
    
    val goldPaint = Paint().apply {
        color = 0xFFFFD700.toInt() // ROI Gold
        textSize = 10f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas1.drawText("EXECUTIVE SUMMARY", 56f, currentY + 24f, goldPaint)
    
    val summaryText = "Based on our proprietary diagnostic tools, ${winner.name} is identified as the Digital Maturity Leader with a score of ${if (scoreA >= scoreB) scoreA else scoreB}%, while ${loser.name} is positioned as Transitionary with a score of ${if (scoreA >= scoreB) scoreB else scoreA}%. A competitive gap of ${Math.abs(scoreA - scoreB)}% exists in modernization readiness."
    
    val summaryDescPaint = Paint().apply {
        color = 0xFFE5E5E5.toInt()
        textSize = 9.5f
        isAntiAlias = true
    }
    val summaryWords = summaryText.split(" ")
    var sLine = ""
    var sLineY = currentY + 44f
    for (w in summaryWords) {
        val testL = if (sLine.isEmpty()) w else "$sLine $w"
        if (summaryDescPaint.measureText(testL) > summaryCardW - 32f) {
            canvas1.drawText(sLine, 56f, sLineY, summaryDescPaint)
            sLine = w
            sLineY += 14f
        } else {
            sLine = testL
        }
    }
    if (sLine.isNotEmpty()) {
        canvas1.drawText(sLine, 56f, sLineY, summaryDescPaint)
    }
    
    currentY += summaryCardH + 30f
    
    // Digital Maturity Table
    val tableTitlePaint = Paint().apply {
        color = 0xFFF1F5F9.toInt()
        textSize = 12f
        isFakeBoldText = true
        isAntiAlias = true
    }
    canvas1.drawText("TECHNOLOGY & CAPABILITY COMPARISON", 40f, currentY, tableTitlePaint)
    currentY += 15f
    
    // Table Header
    val thPaint = Paint().apply {
        color = 0xFFF1F5F9.toInt()
        textSize = 9f
        isFakeBoldText = true
        isAntiAlias = true
    }
    val colW = (pageWidth - 80) / 3f
    canvas1.drawText("METRIC", 40f, currentY + 12f, thPaint)
    canvas1.drawText(companyA.name.uppercase(), 40f + colW + 10f, currentY + 12f, thPaint)
    canvas1.drawText(companyB.name.uppercase(), 40f + colW * 2 + 10f, currentY + 12f, thPaint)
    
    currentY += 20f
    canvas1.drawLine(40f, currentY, (pageWidth - 40).toFloat(), currentY, linePaint)
    
    val rows = listOf(
        Triple("Digital Maturity Score", "$scoreA%", "$scoreB%"),
        Triple("Estimated Monthly IT Cost", "${companyA.costBefore} AZN", "${companyB.costBefore} AZN"),
        Triple("Potential AI Monthly Savings", "${companyA.costBefore - companyA.costAfterAi} AZN", "${companyB.costBefore - companyB.costAfterAi} AZN"),
        Triple("AI Cost Optimization %", "${companyA.savingPercent}%", "${companyB.savingPercent}%"),
        Triple("Hiring LLM Engineers", if (companyA.hiringLlm) "Yes" else "No", if (companyB.hiringLlm) "Yes" else "No")
    )
    
    val rowTextPaint = Paint().apply {
        color = 0xFF94A3B8.toInt()
        textSize = 9.5f
        isAntiAlias = true
    }
    
    val rowHighlightPaint = Paint().apply {
        color = 0xFFF1F5F9.toInt()
        textSize = 9.5f
        isFakeBoldText = true
        isAntiAlias = true
    }
    
    for (row in rows) {
        currentY += 25f
        canvas1.drawText(row.first, 40f, currentY, rowTextPaint)
        
        val valAPaint = if (scoreA >= scoreB && row.first.contains("Maturity")) rowHighlightPaint else rowTextPaint
        val valBPaint = if (scoreB >= scoreA && row.first.contains("Maturity")) rowHighlightPaint else rowTextPaint
        
        canvas1.drawText(row.second, 40f + colW + 10f, currentY, valAPaint)
        canvas1.drawText(row.third, 40f + colW * 2 + 10f, currentY, valBPaint)
        
        canvas1.drawLine(40f, currentY + 6f, (pageWidth - 40).toFloat(), currentY + 6f, Paint().apply {
            color = 0x10FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = 1f
        })
    }
    
    currentY += 35f
    
    // Footer Page 1
    val footerPaint = Paint().apply {
        color = 0xFF64748B.toInt()
        textSize = 8f
        isAntiAlias = true
    }
    canvas1.drawText("Baku Tech Intelligence | Comparative Executive Benchmark", 40f, 810f, footerPaint)
    canvas1.drawText("Page 1 of 2", (pageWidth - 85).toFloat(), 810f, footerPaint)
    
    pdfDocument.finishPage(page1)
    
    // --- PAGE 2 ---
    val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
    val page2 = pdfDocument.startPage(pageInfo2)
    val canvas2 = page2.canvas
    
    canvas2.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)
    
    currentY = 50f
    
    canvas2.drawRoundRect(badgeRectLeft, badgeRectTop, badgeRectRight, badgeRectBottom, 6f, 6f, badgeBgPaint)
    canvas2.drawRoundRect(badgeRectLeft, badgeRectTop, badgeRectRight, badgeRectBottom, 6f, 6f, blueBorderPaint)
    canvas2.drawText(badgeText, badgeRectLeft + 8f, badgeRectTop + 15f, badgePaint)
    
    currentY += 45f
    canvas2.drawText("Strategic Action Plan & ROI", 40f, currentY, titlePaint)
    currentY += 22f
    canvas2.drawText("COMPETITIVE GAP BRIDGE RECOMMENDATIONS", 40f, currentY, subtitlePaint)
    currentY += 30f
    canvas2.drawLine(40f, currentY, (pageWidth - 40).toFloat(), currentY, linePaint)
    currentY += 30f
    
    // ROI Card
    val roiCardH = 90f
    canvas2.drawRoundRect(40f, currentY, 40f + summaryCardW, currentY + roiCardH, 12f, 12f, cardBgPaint)
    canvas2.drawRoundRect(40f, currentY, 40f + summaryCardW, currentY + roiCardH, 12f, 12f, cardBorderPaint)
    
    canvas2.drawText("FINANCIAL IMPACT & ROI METRICS", 56f, currentY + 24f, goldPaint)
    
    val baseITGap = Math.abs(companyA.costBefore - companyB.costBefore) * 12
    val savingsGap = Math.abs((companyA.costBefore - companyA.costAfterAi) - (companyB.costBefore - companyB.costAfterAi)) * 12
    val roiText = "Modernizing ${loser.name}'s stack to match ${winner.name}'s efficiency unlocks an estimated annual savings gap of ${String.format(Locale.US, "%,d", savingsGap)} AZN. Base IT Operational footprint difference stands at ${String.format(Locale.US, "%,d", baseITGap)} AZN annually."
    
    var rLine = ""
    var rLineY = currentY + 44f
    for (w in roiText.split(" ")) {
        val testL = if (rLine.isEmpty()) w else "$rLine $w"
        if (summaryDescPaint.measureText(testL) > summaryCardW - 32f) {
            canvas2.drawText(rLine, 56f, rLineY, summaryDescPaint)
            rLine = w
            rLineY += 14f
        } else {
            rLine = testL
        }
    }
    if (rLine.isNotEmpty()) {
        canvas2.drawText(rLine, 56f, rLineY, summaryDescPaint)
    }
    
    currentY += roiCardH + 30f
    
    // 90-Day Action Plan Section
    canvas2.drawText("90-DAY ACTION PLAN FOR ${loser.name.uppercase()}", 40f, currentY, tableTitlePaint)
    currentY += 20f
    
    val steps = listOf(
        Pair("Phase 1: Cloud Rearchitecture & Legacy Decoupling", "Implement containerization, standardize API boundaries, and prepare databases for high-speed local pipeline access."),
        Pair("Phase 2: LLM Pipeline & Operational Orchestration", "Configure core AI-driven support loops, agentic email routing, and automate custom client service pipelines."),
        Pair("Phase 3: Automated Optimization & Savings Realization", "Deploy predictive analytics for continuous cloud/operational optimization and complete the legacy switch-off.")
    )
    
    for ((idx, step) in steps.withIndex()) {
        val circleX = 55f
        val circleY = currentY + 15f
        val radius = 10f
        
        if (idx < steps.size - 1) {
            val linePaintTimeline = Paint().apply {
                color = 0xFF64748B.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas2.drawLine(circleX, circleY + radius, circleX, circleY + 65f, linePaintTimeline)
        }
        
        canvas2.drawCircle(circleX, circleY, radius, bluePaint)
        
        val stepNumPaint = Paint().apply {
            color = 0xFF09090B.toInt()
            textSize = 10f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas2.drawText((idx + 1).toString(), circleX, circleY + 3.5f, stepNumPaint)
        
        val cardX = 80f
        val cardY = currentY
        val cardW = pageWidth - cardX - 40f
        val cardH = 60f
        
        canvas2.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 12f, 12f, cardBgPaint)
        canvas2.drawRoundRect(cardX, cardY, cardX + cardW, cardY + cardH, 12f, 12f, cardBorderPaint)
        
        canvas2.drawText(step.first, cardX + 16f, cardY + 20f, Paint().apply {
            color = 0xFFF1F5F9.toInt()
            textSize = 10.5f
            isFakeBoldText = true
            isAntiAlias = true
        })
        
        val detailPaint = Paint().apply {
            color = 0xFF94A3B8.toInt()
            textSize = 8.5f
            isAntiAlias = true
        }
        
        val words = step.second.split(" ")
        var line = ""
        var lineY = cardY + 35f
        val maxWidth = cardW - 32f
        
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val testWidth = detailPaint.measureText(testLine)
            if (testWidth > maxWidth) {
                canvas2.drawText(line, cardX + 16f, lineY, detailPaint)
                line = word
                lineY += 11f
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty()) {
            canvas2.drawText(line, cardX + 16f, lineY, detailPaint)
        }
        
        currentY += cardH + 15f
    }
    
    currentY += 10f
    
    // Disclaimer Box
    val discX = 40f
    val discY = currentY
    val discW = (pageWidth - 80).toFloat()
    val discH = 55f
    
    canvas2.drawRoundRect(discX, discY, discX + discW, discY + discH, 12f, 12f, badgeBgPaint)
    canvas2.drawRoundRect(discX, discY, discX + discW, discY + discH, 12f, 12f, blueBorderPaint)
    
    canvas2.drawText("DISCLAIMER:", discX + 16f, discY + 18f, Paint().apply {
        color = 0xFF64748B.toInt()
        textSize = 8f
        isFakeBoldText = true
        isAntiAlias = true
    })
    
    val disclaimerText = "This benchmark analysis acts as a high-level corporate technology comparison. Estimated annual savings gaps are computed based on automated r-architectures and optimal digital alignment."
    val wordsDisc = disclaimerText.split(" ")
    var lineDisc = ""
    var lineDiscY = discY + 30f
    val discTextPaint = Paint().apply {
        color = 0xFF94A3B8.toInt()
        textSize = 8f
        isAntiAlias = true
    }
    for (word in wordsDisc) {
        val testLine = if (lineDisc.isEmpty()) word else "$lineDisc $word"
        val testWidth = discTextPaint.measureText(testLine)
        if (testWidth > discW - 32f) {
            canvas2.drawText(lineDisc, discX + 16f, lineDiscY, discTextPaint)
            lineDisc = word
            lineDiscY += 10f
        } else {
            lineDisc = testLine
        }
    }
    if (lineDisc.isNotEmpty()) {
        canvas2.drawText(lineDisc, discX + 16f, lineDiscY, discTextPaint)
    }
    
    canvas2.drawText("Baku Tech Intelligence | Benchmark Report", 40f, 810f, footerPaint)
    canvas2.drawText("Page 2 of 2", (pageWidth - 85).toFloat(), 810f, footerPaint)
    
    pdfDocument.finishPage(page2)
    
    val cacheFile = File(context.cacheDir, "benchmark_report_${companyA.id}_vs_${companyB.id}.pdf")
    try {
        FileOutputStream(cacheFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return cacheFile
    } catch (e: Exception) {
        e.printStackTrace()
        pdfDocument.close()
    }
    
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkReportOverlay(
    companyA: Company,
    companyB: Company,
    onDismiss: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val context = LocalContext.current
    val scoreA = TechStackScanner.calculateReadinessScore(companyA)
    val scoreB = TechStackScanner.calculateReadinessScore(companyB)
    val winner = if (scoreA >= scoreB) companyA else companyB
    val loser = if (winner.id == companyA.id) companyB else companyA

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBg
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "BENCHMARK ANALYSIS REPORT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 2.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_benchmark_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextPrimary
                    )
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // PDF Button
                        Button(
                            onClick = {
                                val file = generateBenchmarkPdfReport(context, companyA, companyB)
                                if (file != null) {
                                    val savedUri = savePdfToDownloads(context, file, "Benchmark_Report_${companyA.name.replace(" ", "_")}_vs_${companyB.name.replace(" ", "_")}.pdf")
                                    if (savedUri != null) {
                                        onShowSnackbar("Benchmark PDF saved to Downloads")
                                    } else {
                                        Toast.makeText(context, "Saved to cache directory", Toast.LENGTH_SHORT).show()
                                    }
                                    sharePdf(context, file)
                                } else {
                                    Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("export_benchmark_pdf_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BlueAccent,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "PDF",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Share Button
                        Button(
                            onClick = {
                                val shareText = """
                                    EXECUTIVE BENCHMARK REPORT
                                    ${companyA.name} vs ${companyB.name}
                                    Digital Maturity Leader: ${winner.name} (${if (scoreA >= scoreB) scoreA else scoreB}%)
                                    Competitive Gap: ${Math.abs(scoreA - scoreB)}%
                                    Annual savings bridge opportunity: ${String.format(Locale.US, "%,d", Math.abs((companyA.costBefore - companyA.costAfterAi) - (companyB.costBefore - companyB.costAfterAi)) * 12)} AZN/year.
                                """.trimIndent()
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("share_benchmark_report_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldHighlight,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Share",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header Details
                item {
                    Column {
                        Text(
                            text = "${companyA.name} vs ${companyB.name}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BlueAccent
                        )
                        Text(
                            text = "Comparative Digital Maturity & Technology Modernization Analysis.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Prominent PDF Button directly below Executive Summary header
                        Button(
                            onClick = {
                                val file = generateBenchmarkPdfReport(context, companyA, companyB)
                                if (file != null) {
                                    val savedUri = savePdfToDownloads(context, file, "Benchmark_Report_${companyA.name.replace(" ", "_")}_vs_${companyB.name.replace(" ", "_")}.pdf")
                                    if (savedUri != null) {
                                        onShowSnackbar("Benchmark PDF saved to Downloads")
                                    } else {
                                        Toast.makeText(context, "Saved to cache directory", Toast.LENGTH_SHORT).show()
                                    }
                                    sharePdf(context, file)
                                } else {
                                    Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("benchmark_executive_summary_download_pdf_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldHighlight,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export Executive PDF",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Export Executive PDF", 
                                fontSize = 14.sp, 
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0x33FFFFFF), thickness = 1.dp)
                    }
                }

                // Executive Summary Section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "EXECUTIVE SUMMARY",
                                color = GoldHighlight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Based on our proprietary diagnostic tools, ${winner.name} is identified as the Digital Maturity Leader with a score of ${if (scoreA >= scoreB) scoreA else scoreB}%, while ${loser.name} is positioned as Transitionary with a score of ${if (scoreA >= scoreB) scoreB else scoreA}%. A competitive gap of ${Math.abs(scoreA - scoreB)}% exists in modernization readiness.",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Digital Maturity Leader Block
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = EmeraldAccent.copy(alpha = 0.15f),
                                shape = CircleShape,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🏆", fontSize = 20.sp)
                                }
                            }
                            Column {
                                Text(
                                    text = "Digital Maturity Leader: ${winner.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EmeraldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${winner.name} leads with a Digital Maturity Score of ${if (scoreA >= scoreB) scoreA else scoreB}%.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Capability Comparison Table
                item {
                    Column {
                        Text(
                            text = "TECHNOLOGY COMPONENT COMPARISON",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ComparisonTable(a = companyA, b = companyB)
                    }
                }

                // Competitive Capability Scan (Radar Chart)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "COMPETITIVE CAPABILITY SCAN",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            RadarChart(companyA = companyA, companyB = companyB)
                        }
                    }
                }

                // Financial impact Section (ROI)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "FINANCIAL IMPACT PROFILE (ROI)",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val costDiff = Math.abs(companyA.costBefore - companyB.costBefore)
                            val savingsDiff = Math.abs((companyA.costBefore - companyA.costAfterAi) - (companyB.costBefore - companyB.costAfterAi))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Base IT Cost Gap / mo", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text("${costDiff} AZN", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Potential AI Savings Gap / mo", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text("${savingsDiff} AZN", style = MaterialTheme.typography.titleLarge, color = BlueAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Action Plan
                item {
                    ActionPlanCard(
                        a = companyA,
                        b = companyB,
                        winner = winner,
                        showExportButton = false
                    )
                }

                // Benchmark Conclusion Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "BENCHMARK CONCLUSION",
                                color = GoldHighlight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Bridging the gap between ${companyA.name} and ${companyB.name} is achievable over 90 days. Standardization of digital APIs, modern cloud re-architecture, and continuous automated pipeline checks are recommended to achieve peak optimization targets.",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

fun sharePdf(context: Context, cacheFile: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", cacheFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share CTO Report"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

enum class DashboardTab { SCANNER, DASHBOARD, COMPARATOR, HISTORY, TRACKER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StackComparatorScreen(
    viewModel: CompanyViewModel,
    modifier: Modifier = Modifier,
    onSelectionChanged: (Company?, Company?) -> Unit = { _, _ -> },
    onGenerateReport: (Company, Company) -> Unit = { _, _ -> }
) {
    val companies by viewModel.companies.collectAsState()
    var companyA by remember { mutableStateOf<Company?>(null) }
    var companyB by remember { mutableStateOf<Company?>(null) }

    LaunchedEffect(companyA, companyB) {
        onSelectionChanged(companyA, companyB)
    }

    LaunchedEffect(companies) {
        if (companies.isNotEmpty()) {
            if (companyA == null) {
                companyA = companies.find { it.id == "kapital_bank" } ?: companies.firstOrNull()
            }
            if (companyB == null) {
                companyB = companies.find { it.id == "pasha_bank" } ?: companies.getOrNull(1)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("comparator_screen"),
        contentPadding = PaddingValues(32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // 1. Unified Premium Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "BENCHMARKING & METRICS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldHighlight, // ROI Gold
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Corporate Technology Benchmark",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Compare and benchmark IT architectures, operational expenditure footprints, and modernization indices.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }
        }

        // 2. Benchmark Metadata Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = "SELECTED TARGET",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = companyA?.name ?: "Select Target",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = "BENCHMARK AGAINST",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = companyB?.name ?: "Select Benchmark",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = BlueAccent
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GENERATED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Today",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Company Selectors
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompanyDropdown(
                    label = "Company A",
                    companies = companies,
                    selected = companyA,
                    onSelect = { companyA = it },
                    modifier = Modifier.weight(1f)
                )
                CompanyDropdown(
                    label = "Company B",
                    companies = companies.filter { it.id != companyA?.id },
                    selected = companyB,
                    onSelect = { companyB = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (companyA != null && companyB != null) {
            val a = companyA!!
            val b = companyB!!
            val scoreA = TechStackScanner.calculateReadinessScore(a)
            val scoreB = TechStackScanner.calculateReadinessScore(b)
            val winner = if (scoreA >= scoreB) a else b
            val loser = if (winner.id == a.id) b else a

            // Winner Announcement Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = EmeraldAccent.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🏆", fontSize = 20.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Digital Maturity Leader: ${winner.name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${winner.name} leads with a Digital Maturity Score of ${if (scoreA >= scoreB) scoreA else scoreB}%.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Radar Chart Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "COMPETITIVE CAPABILITY SCAN",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        RadarChart(companyA = a, companyB = b)
                    }
                }
            }

            // Cost comparison Card with custom math
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassCard(cornerRadius = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "FINANCIAL IMPACT PROFILE",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val costDiff = Math.abs(a.costBefore - b.costBefore)
                        val savingsDiff = Math.abs((a.costBefore - a.costAfterAi) - (b.costBefore - b.costAfterAi))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Base IT Cost Gap", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("${costDiff} AZN", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Potential AI Savings Gap", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("${savingsDiff} AZN", style = MaterialTheme.typography.titleLarge, color = BlueAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Comparison Table
            item {
                ComparisonTable(a = a, b = b)
            }

            // Action Plan
            item {
                ActionPlanCard(
                    a = a,
                    b = b,
                    winner = winner,
                    onGenerateReport = {
                        onGenerateReport(a, b)
                    }
                )
            }
        } else {
            // Placeholder state
            item {
                PremiumEmptyState(
                    icon = Icons.Default.CompareArrows,
                    title = "Select two companies to generate a competitive intelligence report.",
                    description = "Choose two active target profiles from the selectors above to initiate complete comparative digital metrics.",
                    accentColor = BlueAccent,
                    illustrationType = EmptyIllustrationType.COMPARISON
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDropdown(
    label: String,
    companies: List<Company>,
    selected: Company?,
    onSelect: (Company) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = TextSecondary) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = BlueAccent,
                unfocusedBorderColor = CardBorderColor,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedLabelColor = BlueAccent,
                unfocusedLabelColor = TextSecondary
            ),
            shape = RoundedCornerShape(14.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(DarkBg)
                .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(12.dp))
        ) {
            companies.forEach { company ->
                DropdownMenuItem(
                    text = { Text(company.name, color = TextPrimary) },
                    onClick = {
                        onSelect(company)
                        expanded = false
                    },
                    modifier = Modifier.background(DarkBg)
                )
            }
        }
    }
}

@Composable
fun RadarChart(companyA: Company, companyB: Company) {
    val labels = listOf("Digital Maturity", "Operational Efficiency", "Revenue Impact", "Engineering Velocity", "Customer Experience")
    
    val metricsA = listOf(
        TechStackScanner.calculateReadinessScore(companyA).toFloat(),
        (100 - TechStackScanner.getTechnicalDebtScore(companyA)).toFloat(),
        (TechStackScanner.calculateReadinessScore(companyA) + 12).coerceIn(0, 100).toFloat(),
        (100 - (companyA.costBefore.toFloat() / 200).coerceIn(0f, 60f)),
        if (companyA.currentStack.lowercase().contains("react") || companyA.currentStack.lowercase().contains("firebase")) 95f else 50f
    )
    
    val metricsB = listOf(
        TechStackScanner.calculateReadinessScore(companyB).toFloat(),
        (100 - TechStackScanner.getTechnicalDebtScore(companyB)).toFloat(),
        (TechStackScanner.calculateReadinessScore(companyB) + 12).coerceIn(0, 100).toFloat(),
        (100 - (companyB.costBefore.toFloat() / 200).coerceIn(0f, 60f)),
        if (companyB.currentStack.lowercase().contains("react") || companyB.currentStack.lowercase().contains("firebase")) 95f else 50f
    )

    val radarChartAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 350, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)),
        label = "ComparisonRadarChartAnim"
    )
    val localBlue = BlueAccent
    val localEmerald = EmeraldAccent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.height.coerceAtMost(size.width) / 2.3f
            val sides = 5
            val angleStep = (2 * Math.PI / sides).toFloat()

            // 1. Draw concentric background pentagons
            val levels = 4
            for (level in 1..levels) {
                val fraction = level.toFloat() / levels
                val radius = maxRadius * fraction
                val path = Path()
                for (i in 0 until sides) {
                    val angle = i * angleStep - Math.PI.toFloat() / 2f
                    val x = center.x + radius * cos(angle)
                    val y = center.y + radius * sin(angle)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(
                    path = path,
                    color = TextSecondary.copy(alpha = 0.15f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 2. Draw outer boundary and axis lines with labels
            val outerPath = Path()
            for (i in 0 until sides) {
                val angle = i * angleStep - Math.PI.toFloat() / 2f
                val endPoint = Offset(
                    center.x + maxRadius * cos(angle),
                    center.y + maxRadius * sin(angle)
                )
                drawLine(
                    color = TextSecondary.copy(alpha = 0.3f),
                    start = center,
                    end = endPoint,
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 3. Plot Company A metrics
            val pathA = Path()
            for (i in 0 until sides) {
                val angle = i * angleStep - Math.PI.toFloat() / 2f
                val valueFraction = (metricsA[i] / 100f) * radarChartAnim
                val radius = maxRadius * valueFraction
                val x = center.x + radius * cos(angle)
                val y = center.y + radius * sin(angle)
                if (i == 0) pathA.moveTo(x, y) else pathA.lineTo(x, y)
            }
            pathA.close()
            drawPath(
                path = pathA,
                color = localBlue.copy(alpha = 0.25f)
            )
            drawPath(
                path = pathA,
                color = localBlue,
                style = Stroke(width = 2.dp.toPx())
            )

            // 4. Plot Company B metrics
            val pathB = Path()
            for (i in 0 until sides) {
                val angle = i * angleStep - Math.PI.toFloat() / 2f
                val valueFraction = (metricsB[i] / 100f) * radarChartAnim
                val radius = maxRadius * valueFraction
                val x = center.x + radius * cos(angle)
                val y = center.y + radius * sin(angle)
                if (i == 0) pathB.moveTo(x, y) else pathB.lineTo(x, y)
            }
            pathB.close()
            drawPath(
                path = pathB,
                color = localEmerald.copy(alpha = 0.25f)
            )
            drawPath(
                path = pathB,
                color = localEmerald,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Legends/Labels placement
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Label
            Text(labels[0], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(labels[4], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Text(labels[1], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(labels[3], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Text(labels[2], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            }
        }
    }

    // Chart Legend Indicator Row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(BlueAccent)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(companyA.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(EmeraldAccent)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(companyB.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ComparisonTable(a: Company, b: Company) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "COMPARATIVE PROFILE",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Table Header
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Metric", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.titleSmall, color = TextSecondary, fontWeight = FontWeight.Bold)
                Text(a.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = BlueAccent, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(b.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = EmeraldAccent, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = CardBorderColor)

            TableRow("Digital Maturity", "${TechStackScanner.calculateReadinessScore(a)}%", "${TechStackScanner.calculateReadinessScore(b)}%")
            TableRow("Technical Debt", "${TechStackScanner.getTechnicalDebtScore(a)}/100", "${TechStackScanner.getTechnicalDebtScore(b)}/100")
            TableRow("Monthly Cost", "${a.costBefore} AZN", "${b.costBefore} AZN")
            TableRow("Post-AI Cost", "${a.costAfterAi} AZN", "${b.costAfterAi} AZN")
            TableRow("Savings Potential", "${a.savingPercent}%", "${b.savingPercent}%")
        }
    }
}

@Composable
fun TableRow(label: String, valA: String, valB: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1.2f), color = TextSecondary, fontSize = 13.sp)
        Text(valA, modifier = Modifier.weight(1f), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(valB, modifier = Modifier.weight(1f), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ActionPlanCard(
    a: Company,
    b: Company,
    winner: Company,
    showExportButton: Boolean = true,
    onGenerateReport: (() -> Unit)? = null
) {
    val loser = if (winner.id == a.id) b else a
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ACTION PLAN TO OVERTAKE ${winner.name}",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Critical recommendations for ${loser.name} to neutralize ${winner.name}'s technical and operational advantages:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))

            val weaknessesList = loser.weakness.split(". ").filter { it.isNotBlank() }
            weaknessesList.take(3).forEach { weakness ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("⚡ ", color = Color(0xFFEF4444), fontSize = 14.sp)
                    Text(
                        text = weakness.trim().removeSuffix("."),
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (showExportButton) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (onGenerateReport != null) {
                            onGenerateReport()
                        } else {
                            Toast.makeText(context, "Battle plan PDF generated for ${loser.name}!", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlueAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export Battle Plan", tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPORT BATTLE PLAN PDF", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun TrackerScreen(
    viewModel: CompanyViewModel,
    modifier: Modifier = Modifier
) {
    val allCompanies by viewModel.companies.collectAsState()
    val selectedCompany by viewModel.selectedCompany.collectAsState()
    val company = selectedCompany ?: allCompanies.firstOrNull()

    if (company == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBg),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No company data available", color = TextSecondary)
        }
        return
    }

    // Dynamic calculations
    val currentScore = TechStackScanner.calculateReadinessScore(company)
    val projectedScore = (currentScore + 25).coerceIn(0, 100)

    val sortedCompanies = allCompanies.sortedByDescending { TechStackScanner.calculateReadinessScore(it) }
    val currentIndex = sortedCompanies.indexOfFirst { it.id == company.id }
    val currentRank = if (currentIndex != -1) currentIndex + 1 else 3
    val totalCompanies = allCompanies.size.coerceAtLeast(1)
    val projectedRank = (currentRank - 2).coerceAtLeast(1)

    val estimatedSavings = (company.costBefore - company.costAfterAi) * 12
    val estimatedRoi = ((company.costBefore - company.costAfterAi) * 12 * 100 / (company.costBefore * 2)).coerceIn(120, 480)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .testTag("strategic_action_plan_screen"),
        contentPadding = PaddingValues(32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Title block
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "STRATEGIC ACTION PLAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldHighlight, // ROI Gold
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Strategic Action Plan",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "A personalized executive roadmap to improve competitive positioning using AI and modern technology.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }
        }

        // Premium Executive Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("executive_summary_card")
                    .glassCard(cornerRadius = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "EXECUTIVE VALUE REALIZATION SUMMARY",
                        color = GoldHighlight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    // First row of metrics: Scores & Ranks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Competitive Score Transition
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "COMPETITIVE SCORE",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$currentScore%",
                                    color = TextSecondary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "to",
                                    tint = GoldHighlight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "$projectedScore%",
                                    color = EmeraldAccent,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Digital Maturity Index",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }

                        // Industry Rank Transition
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "INDUSTRY RANK",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "#$currentRank",
                                    color = TextSecondary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "to",
                                    tint = GoldHighlight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "#$projectedRank",
                                    color = EmeraldAccent,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Out of $totalCompanies Sector Peers",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Second row of metrics: ROI & Savings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Estimated ROI
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ESTIMATED ROI",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "$estimatedRoi% YoY",
                                color = GoldHighlight,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "12-Month Return projection",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }

                        // Estimated Annual Savings
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ESTIMATED ANNUAL SAVINGS",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "₼${String.format(Locale.US, "%,d", estimatedSavings)}",
                                color = EmeraldAccent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Net operational reduction",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Section header questions
        item {
            Text(
                text = "STRATEGIC SIX-MONTH EXECUTION PATHWAY",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        // Phase 1 – Immediate Wins (0–30 Days)
        item {
            ConsultingReportCard(
                phaseTitle = "Phase 1 – Immediate Wins (0–30 Days)",
                accentColor = GoldHighlight,
                fields = listOf(
                    "Priority" to "CRITICAL HIGH",
                    "Expected Business Impact" to "Eliminate severe architectural bottlenecks, optimize resource sizing, and decouple legacy monolith overhead.",
                    "Estimated Annual Savings (AZN)" to "₼${String.format(Locale.US, "%,d", (estimatedSavings * 0.4).toInt())}"
                ),
                recommendations = listOf(
                    "Resolve critical bottlenecks in legacy stack (${company.currentStack}) via targeted high-throughput API abstraction.",
                    "Enforce automated instance resource scheduling policies on idle development systems.",
                    "Integrate generative developer assistance setups to boost internal engineering sprint velocity by 40%."
                )
            )
        }

        // Phase 2 – Competitive Growth (30–90 Days)
        item {
            ConsultingReportCard(
                phaseTitle = "Phase 2 – Competitive Growth (30–90 Days)",
                accentColor = BlueAccent,
                fields = listOf(
                    "Technology Improvements" to "Transition from monolithic environments to decoupled serverless endpoints with dynamic scaling properties.",
                    "Automation Opportunities" to "Implement intent-driven LLM customer support classifiers to autonomously resolve over 70% of routine service tickets.",
                    "AI Initiatives" to "Deploy proprietary retrieval-augmented generation pipelines for rapid, low-latency search across core legacy operational knowledge.",
                    "Business Value" to "Slashes customer support SLA to sub-second responses, maximizes engineering focus on core innovations, and stabilizes infrastructure run-rate."
                ),
                recommendations = emptyList()
            )
        }

        // Phase 3 – Market Leadership (90–180 Days)
        item {
            ConsultingReportCard(
                phaseTitle = "Phase 3 – Market Leadership (90–180 Days)",
                accentColor = EmeraldAccent,
                fields = listOf(
                    "Advanced AI Adoption" to "Deploy orchestration frameworks utilizing multi-agent AI loops to automate high-frequency internal transactions and procurement scheduling.",
                    "Innovation Opportunities" to "Fine-tune custom, secure, light-weight local models on internal interaction datasets to predict shifts in market demand with extreme precision.",
                    "Long-term Competitive Advantages" to "Drastically lower industry-wide cost-to-serve ratio, superior speed-to-market metrics, and hyper-customized user engagements at zero marginal cost.",
                    "Executive Outcomes" to "Establishes ${company.name} as the absolute technology leader, building a major structural moat that makes fast-following extremely difficult for competitors."
                ),
                recommendations = emptyList()
            )
        }
    }
}

@Composable
fun ConsultingReportCard(
    phaseTitle: String,
    accentColor: Color,
    fields: List<Pair<String, String>>,
    recommendations: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with phase title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = phaseTitle.uppercase(),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            HorizontalDivider(color = Color(0x11FFFFFF), thickness = 1.dp)

            // Consulting-style fields
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                fields.forEach { (label, detail) ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = label.uppercase(),
                            color = accentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = detail,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Recommendations section if present
            if (recommendations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "KEY STRATEGIC RECOMMENDATIONS",
                    color = accentColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    recommendations.forEachIndexed { index, recommendation ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "0${index + 1}",
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                            Text(
                                text = recommendation,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumEmptyState(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    accentColor: Color = BlueAccent,
    illustrationType: EmptyIllustrationType = EmptyIllustrationType.CIRCLES
) {
    val localBlue = BlueAccent
    val localEmerald = EmeraldAccent
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate_angle"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = size.width / 3f
                    
                    when (illustrationType) {
                        EmptyIllustrationType.CIRCLES -> {
                            drawCircle(
                                color = accentColor.copy(alpha = 0.15f * pulseScale),
                                radius = baseRadius * 1.4f * pulseScale,
                                center = center,
                                style = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                )
                            )
                            drawCircle(
                                color = accentColor.copy(alpha = 0.25f),
                                radius = baseRadius,
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            val orbitX = center.x + (baseRadius * cos(Math.toRadians(rotateAngle.toDouble()))).toFloat()
                            val orbitY = center.y + (baseRadius * sin(Math.toRadians(rotateAngle.toDouble()))).toFloat()
                            drawCircle(
                                color = accentColor,
                                radius = 4.dp.toPx(),
                                center = Offset(orbitX, orbitY)
                            )
                        }
                        EmptyIllustrationType.COMPARISON -> {
                            val offsetDist = baseRadius * 0.4f
                            drawCircle(
                                color = localBlue.copy(alpha = 0.2f),
                                radius = baseRadius * 0.9f * pulseScale,
                                center = Offset(center.x - offsetDist, center.y),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            drawCircle(
                                color = localEmerald.copy(alpha = 0.2f),
                                radius = baseRadius * 0.9f * pulseScale,
                                center = Offset(center.x + offsetDist, center.y),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            drawLine(
                                color = TextSecondary.copy(alpha = 0.2f),
                                start = Offset(center.x - baseRadius * 1.5f, center.y),
                                end = Offset(center.x + baseRadius * 1.5f, center.y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 10f), 0f)
                            )
                        }
                        EmptyIllustrationType.DATABASE -> {
                            val spacing = 14.dp.toPx()
                            for (i in -2..2) {
                                val yOff = center.y + i * spacing
                                drawLine(
                                    color = accentColor.copy(alpha = if (i == 0) 0.4f else 0.15f),
                                    start = Offset(center.x - baseRadius * 1.2f, yOff),
                                    end = Offset(center.x + baseRadius * 1.2f, yOff),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                            }
                            drawCircle(
                                color = accentColor.copy(alpha = 0.08f),
                                radius = baseRadius * 1.3f,
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(54.dp)
                        .border(
                            BorderStroke(
                                1.5.dp,
                                Brush.verticalGradient(
                                    colors = listOf(TextPrimary.copy(alpha = 0.6f), accentColor.copy(alpha = 0.3f))
                                )
                            ),
                            CircleShape
                        ),
                    color = DarkSurfaceElevated.copy(alpha = 0.9f),
                    shape = CircleShape,
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Empty State Icon",
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NO RECORDS FOUND",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor.copy(alpha = 0.8f),
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

enum class EmptyIllustrationType {
    CIRCLES, COMPARISON, DATABASE
}


