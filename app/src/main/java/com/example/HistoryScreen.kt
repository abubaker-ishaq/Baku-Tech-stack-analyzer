package com.example

import com.example.ui.theme.*

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HistoryScreen(
    viewModel: CompanyViewModel,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToScan: (() -> Unit)? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    
    val selectedCompany by viewModel.selectedCompany.collectAsState()
    val defaultCompanies by viewModel.companies.collectAsState()

    // Local lists states
    val scannedCompanies = remember { mutableStateListOf<Company>() }
    val favoritesSet = remember { mutableStateOf(setOf<String>()) }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    fun refreshState() {
        scannedCompanies.clear()
        scannedCompanies.addAll(BakuPersistence.getScannedCompanies(context))
        favoritesSet.value = BakuPersistence.getFavorites(context)
    }

    LaunchedEffect(Unit) {
        refreshState()
    }

    val combinedList = remember(scannedCompanies, defaultCompanies, showOnlyFavorites, favoritesSet.value) {
        val all = (scannedCompanies + defaultCompanies).distinctBy { it.id }
        if (showOnlyFavorites) {
            all.filter { favoritesSet.value.contains(it.id) }
        } else {
            all
        }
    }

    // Dynamic metrics calculation
    val totalReports = combinedList.size
    val companiesAnalyzed = combinedList.distinctBy { it.id }.size
    val lastUpdatedText = if (combinedList.isNotEmpty()) "Today" else "N/A"
    val totalSavingsValue = combinedList.sumOf { (it.costBefore - it.costAfterAi) * 12 }
    val formattedTotalSavings = "₼" + String.format(Locale.US, "%,d", totalSavingsValue) + "/year"

    // Analysis Insights Calculations
    val techCounts = combinedList.flatMap { it.currentStack.split(",") }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .groupBy { it }
        .mapValues { it.value.size }
    val mostCommonTech = techCounts.maxByOrNull { it.value }?.key ?: "React / Node.js"

    val maxReadiness = combinedList.maxOfOrNull { TechStackScanner.calculateReadinessScore(it) } ?: 0
    val maxReadinessComp = combinedList.maxByOrNull { TechStackScanner.calculateReadinessScore(it) }?.name ?: "None"

    val largestSavingComp = combinedList.maxByOrNull { (it.costBefore - it.costAfterAi) * 12 }
    val largestSavingStr = if (largestSavingComp != null) {
        "₼" + String.format(Locale.US, "%,d", (largestSavingComp.costBefore - largestSavingComp.costAfterAi) * 12)
    } else "₼0"
    val largestSavingName = largestSavingComp?.name ?: "None"

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Header Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "EXECUTIVE INTELLIGENCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldHighlight,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Executive Report Library",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Store, review and compare executive technology assessments, benchmarking reports and strategic recommendations.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }

            // Library Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 1.dp,
                            color = CardBorderColor,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EXECUTIVE PORTFOLIO SUMMARY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldHighlight,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Analytics Icon",
                                tint = GoldHighlight,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Summary Statistics Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TOTAL REPORTS", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$totalReports Reports", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("COMPANIES ANALYZED", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$companiesAnalyzed Companies", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LAST ASSESSMENT", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(lastUpdatedText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ESTIMATED SAVINGS", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formattedTotalSavings,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }
                        }
                    }
                }
            }

            // Analysis Insights Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "ANALYSIS INSIGHTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Most Common Technology
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("COMMON TECH", fontSize = 8.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text(mostCommonTech, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BlueAccent)
                                Text("Standardized stack pattern", fontSize = 9.sp, color = TextSecondary)
                            }
                        }

                        // Card 2: Highest AI Readiness
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("MAX AI READINESS", fontSize = 8.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text("$maxReadiness%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                                Text(maxReadinessComp, fontSize = 9.sp, color = TextSecondary, maxLines = 1)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 3: Largest Savings
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("LARGEST OPPORTUNITY", fontSize = 8.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text(largestSavingStr, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldHighlight)
                                Text("Yearly: $largestSavingName", fontSize = 9.sp, color = TextSecondary, maxLines = 1)
                            }
                        }

                        // Card 4: Most Competitive Industry
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp)
                                .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("CRITICAL INDUSTRY", fontSize = 8.5.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text("FinTech & Banking", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Highest automation disruption", fontSize = 9.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // Benchmark History Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "BENCHMARK HISTORY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    val benchmarks = listOf(
                        Triple("Kapital Bank vs PASHA Bank", "PASHA Bank", Pair("+12% Efficiency", "84% Tech Maturity | Cloud Integration")),
                        Triple("ABB vs Azercell", "Azercell", Pair("+18% AI Adoption", "78% Tech Maturity | Support Automation")),
                        Triple("SOCAR vs PASHA Technology", "PASHA Technology", Pair("+25% Infrastructure", "92% Tech Maturity | API Platform"))
                    )

                    benchmarks.forEach { bench ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = bench.first,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        color = EmeraldAccent.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "WINNER: ${bench.second}",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldAccent
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("COMPETITIVE GAP", fontSize = 8.sp, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(bench.third.first, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldHighlight)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("BUSINESS OPPORTUNITY", fontSize = 8.sp, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(bench.third.second, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Assessment History Index Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASSESSMENT HISTORY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showOnlyFavorites = !showOnlyFavorites }
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favs Filter",
                            tint = if (showOnlyFavorites) GoldHighlight else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Favorites Only",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showOnlyFavorites) GoldHighlight else TextSecondary
                        )
                    }
                }
            }

            if (combinedList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(DarkSurfaceElevated, CircleShape)
                                .border(1.dp, CardBorderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = "Folder Open",
                                tint = GoldHighlight.copy(alpha = 0.8f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "No Executive Reports Yet",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Generate your first technology assessment to build your executive intelligence library.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            lineHeight = 18.sp
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { onNavigateToScan?.invoke() },
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("generate_first_report_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldHighlight,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Generate First Report", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // History & Preset items
            itemsIndexed(combinedList) { index, comp ->
                val score = TechStackScanner.calculateReadinessScore(comp)
                val isFav = favoritesSet.value.contains(comp.id)

                val itemBorderWidth = if (selectedCompany?.id == comp.id) 1.5.dp else 1.dp
                var cardModifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 16.dp, borderWidth = itemBorderWidth)
                    .then(
                        if (selectedCompany?.id == comp.id) 
                            Modifier.background(BlueAccent.copy(alpha = 0.08f)) 
                        else 
                            Modifier
                    )
                
                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        cardModifier = cardModifier.sharedElement(
                            rememberSharedContentState(key = "card_${comp.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                }

                val annualSavings = (comp.costBefore - comp.costAfterAi) * 12
                val formattedAnnualSavings = "₼" + String.format(Locale.US, "%,d", annualSavings)

                Card(
                    modifier = cardModifier.premiumCardElevation {
                        viewModel.selectCompany(comp)
                        onShowSnackbar("Selected target profile: ${comp.name}")
                    },
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val titleModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                        with(sharedTransitionScope) {
                                            Modifier.sharedElement(
                                                rememberSharedContentState(key = "title_${comp.id}"),
                                                animatedVisibilityScope = animatedVisibilityScope
                                            )
                                        }
                                    } else {
                                        Modifier
                                    }
                                    Text(
                                        text = comp.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        modifier = titleModifier
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (comp.id.startsWith("scanned_")) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = EmeraldAccent.copy(alpha = 0.12f)),
                                            border = BorderStroke(0.5.dp, EmeraldAccent.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "ASSESSMENT",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = EmeraldAccent
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Compared Against: Industry Benchmarks",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            // Actions: Fav star and Delete button if custom scan
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        favoritesSet.value = BakuPersistence.toggleFavorite(context, comp.id)
                                        onShowSnackbar(if (favoritesSet.value.contains(comp.id)) "Saved to favorites!" else "Removed from favorites")
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Fav",
                                        tint = if (isFav) GoldHighlight else TextSecondary.copy(alpha = 0.5f)
                                    )
                                }

                                if (comp.id.startsWith("scanned_")) {
                                    IconButton(
                                        onClick = {
                                            BakuPersistence.deleteScannedCompany(context, comp.id)
                                            refreshState()
                                            onShowSnackbar("Deleted report from history")
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = TextSecondary.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Report Metrics Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("TECH SCORE", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${score - 5}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BlueAccent)
                            }

                            Column {
                                Text("AI READINESS", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$score%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("ANNUAL SAVINGS", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(formattedAnnualSavings, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldHighlight)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = EmeraldAccent.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, EmeraldAccent.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Completed Check",
                                            tint = EmeraldAccent,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Completed",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldAccent
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Report Date: Today",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.selectCompany(comp)
                                    viewModel.setReportOpen(true)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = GoldHighlight
                                ),
                                border = BorderStroke(1.dp, GoldHighlight.copy(alpha = 0.5f)),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("View Executive Report", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
