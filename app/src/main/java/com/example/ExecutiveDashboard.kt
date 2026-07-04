package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class StrategicRecommendation(
    val title: String,
    val detail: String,
    val priority: String, // High, Medium, Low
    val impact: String,   // High, Medium, Low
    val expectedBenefit: String
)

@Composable
fun Modifier.premiumPressScale(
    scaleFactor: Float = 0.98f,
    onPressedAlpha: Float = 0.95f,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleFactor else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "PressScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) onPressedAlpha else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "PressAlpha"
    )
    
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = onClick != null
        ) {
            onClick?.invoke()
        }
}

@Composable
fun AnimatedGridBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "grid_anim")
    val gridOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "grid_offset"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val gridSpacing = 40.dp.toPx()
        
        // Horizontal lines
        var y = gridOffset % gridSpacing
        while (y < height) {
            drawLine(
                color = Color.White.copy(alpha = 0.03f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
            y += gridSpacing
        }

        // Vertical lines
        var x = gridOffset % gridSpacing
        while (x < width) {
            drawLine(
                color = Color.White.copy(alpha = 0.03f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
            x += gridSpacing
        }
    }
}

@Composable
fun KpiMetricItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White
) {
    Column(
        modifier = modifier
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        // Minimal subtle indicator pill above the value
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(accentColor)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Giant Number
        Text(
            text = value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = (-1).sp,
            lineHeight = 36.sp
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Minimal uppercase label
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.4f),
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
fun ExecutiveDashboardScreen(
    viewModel: CompanyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedCompany by viewModel.selectedCompany.collectAsState()
    val defaultCompanies by viewModel.companies.collectAsState()
    
    // Combine preset companies with local scanned companies
    val scannedCompanies = remember { BakuPersistence.getScannedCompanies(context) }
    val allCompanies = remember(defaultCompanies, scannedCompanies) {
        (scannedCompanies + defaultCompanies).distinctBy { it.id }
    }

    val activeCompany = selectedCompany ?: allCompanies.firstOrNull()

    if (activeCompany == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            PremiumEmptyState(
                icon = Icons.Default.BusinessCenter,
                title = "No company selected",
                description = "Choose an organization from the target selector on the Home screen or perform a website scan to begin.",
                accentColor = BlueAccent,
                illustrationType = EmptyIllustrationType.CIRCLES
            )
        }
        return
    }

    fun calculateCisScore(company: Company): Int {
        val aiReadiness = TechStackScanner.calculateReadinessScore(company)
        val techDebt = TechStackScanner.getTechnicalDebtScore(company)
        val automation = ((100 - techDebt) * 1.05f).toInt().coerceIn(15, 95)
        val cloudAdoption = if (company.currentStack.lowercase(Locale.ROOT).contains("on-premises") || 
                                company.currentStack.lowercase(Locale.ROOT).contains("on-prem")) {
            28
        } else if (company.currentStack.lowercase(Locale.ROOT).contains("cloud") || 
                   company.currentStack.lowercase(Locale.ROOT).contains("serverless") || 
                   company.currentStack.lowercase(Locale.ROOT).contains("fargate") || 
                   company.currentStack.lowercase(Locale.ROOT).contains("gcp")) {
            88
        } else {
            58
        }
        val modernity = (100 - techDebt).coerceIn(12, 95)
        return ((aiReadiness + automation + cloudAdoption + modernity) / 4).coerceIn(0, 100)
    }

    // Bind Company A to active selection
    val companyA = activeCompany

    // Dynamic Selectable Competitor Company B
    var selectedCompB by remember { mutableStateOf<Company?>(null) }
    val companyB = selectedCompB ?: remember(allCompanies, companyA) {
        val defaultComp = allCompanies.find { it.id == "pasha_bank" } ?: allCompanies.firstOrNull { it.id != companyA.id } ?: allCompanies.first()
        if (companyA.id == defaultComp.id) {
            allCompanies.find { it.id == "kapital_bank" } ?: allCompanies.first { it.id != companyA.id }
        } else {
            defaultComp
        }
    }

    // Master Score & Component Calculations for A
    val scoreA = calculateCisScore(companyA)
    val aiReadinessA = TechStackScanner.calculateReadinessScore(companyA)
    val techDebtA = TechStackScanner.getTechnicalDebtScore(companyA)
    val automationA = ((100 - techDebtA) * 1.05f).toInt().coerceIn(15, 95)
    val cloudAdoptionA = if (companyA.currentStack.lowercase(Locale.ROOT).contains("on-premises") || 
                            companyA.currentStack.lowercase(Locale.ROOT).contains("on-prem")) {
        28
    } else if (companyA.currentStack.lowercase(Locale.ROOT).contains("cloud") || 
               companyA.currentStack.lowercase(Locale.ROOT).contains("serverless") || 
               companyA.currentStack.lowercase(Locale.ROOT).contains("fargate") || 
               companyA.currentStack.lowercase(Locale.ROOT).contains("gcp")) {
        88
    } else {
        58
    }
    val technologyModernityA = (100 - techDebtA).coerceIn(12, 95)
    val innovationA = (scoreA * 1.12f).coerceIn(20f, 95f)

    // Master Score & Component Calculations for B
    val scoreB = calculateCisScore(companyB)
    val aiReadinessB = TechStackScanner.calculateReadinessScore(companyB)
    val techDebtB = TechStackScanner.getTechnicalDebtScore(companyB)
    val automationB = ((100 - techDebtB) * 1.05f).toInt().coerceIn(15, 95)
    val cloudAdoptionB = if (companyB.currentStack.lowercase(Locale.ROOT).contains("on-premises") || 
                            companyB.currentStack.lowercase(Locale.ROOT).contains("on-prem")) {
        28
    } else if (companyB.currentStack.lowercase(Locale.ROOT).contains("cloud") || 
               companyB.currentStack.lowercase(Locale.ROOT).contains("serverless") || 
               companyB.currentStack.lowercase(Locale.ROOT).contains("fargate") || 
               companyB.currentStack.lowercase(Locale.ROOT).contains("gcp")) {
        88
    } else {
        58
    }
    val technologyModernityB = (100 - techDebtB).coerceIn(12, 95)
    val innovationB = (scoreB * 1.12f).coerceIn(20f, 95f)

    val cisCategory = when {
        scoreA <= 35 -> "Legacy State"
        scoreA <= 60 -> "Transitionary"
        scoreA <= 80 -> "Competitively Aligned"
        else -> "Digital Pioneer"
    }
    
    val totalCost = companyA.costBefore
    val potentialSavings = companyA.costBefore - companyA.costAfterAi
    val roiMultiple = (potentialSavings.toFloat() / (companyA.costAfterAi.coerceAtLeast(1000))) * 2.5f

    // Animated values for premium counter and transition effects
    val animatedCisScore by animateFloatAsState(
        targetValue = scoreA.toFloat(),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "CisScoreAnimation"
    )
    val animatedTotalCost by animateFloatAsState(
        targetValue = totalCost.toFloat(),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "TotalCostAnimation"
    )
    val animatedPotentialSavings by animateFloatAsState(
        targetValue = potentialSavings.toFloat(),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "PotentialSavingsAnimation"
    )
    val animatedRoiMultiple by animateFloatAsState(
        targetValue = roiMultiple,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "RoiMultipleAnimation"
    )
    val animatedTechDebt by animateFloatAsState(
        targetValue = techDebtA.toFloat(),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "TechDebtAnimation"
    )

    // Confidence Level based on profile maturity
    val confidenceLevel = if (companyA.id.startsWith("scanned_")) 87 else 91

    // Generate consulting strategic recommendations
    val recommendations = remember(companyA) { getStrategicRecommendations(companyA) }

    var isReportMode by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = isReportMode,
        transitionSpec = {
            fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) togetherWith
            fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
        },
        label = "ReportModeTransition"
    ) { reportModeActive ->
        if (reportModeActive) {
            ExecutiveReportView(
                activeCompany = companyA,
                onExitReport = { isReportMode = false },
                cisScore = scoreA,
                cisCategory = cisCategory,
                recommendations = recommendations,
                potentialSavings = potentialSavings,
                modifier = modifier
            )
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // PREMIUM BOARDROOM HEADER WITH SUBTITLE (Redesigned into a Elegant Consulting Header)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "PORTFOLIO INTELLIGENCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldHighlight, // ROI Gold
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Maturity & Capability Dashboard",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Analyze high-fidelity technology postures, formulate target digital maturity scores, and map dynamic modernization trajectories.",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            lineHeight = 22.sp
                        )
                    }
                }

                // HIGH-FIDELITY OVERALL SCORE COMPARISON CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                        border = BorderStroke(1.dp, CardBorderColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = companyA.name.uppercase(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        letterSpacing = (-0.2).sp
                                    )
                                    Text(
                                        text = "vs",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = TextSecondary.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = companyB.name.uppercase(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        letterSpacing = (-0.2).sp
                                    )
                                }

                                Surface(
                                    color = GoldHighlight.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, GoldHighlight.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = cisCategory.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GoldHighlight,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "$scoreA",
                                            fontSize = 64.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            lineHeight = 64.sp,
                                            letterSpacing = (-2).sp
                                        )

                                        Column {
                                            Text(
                                                text = "Overall Maturity Score",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            val diff = scoreA - 76
                                            val diffText = if (diff >= 0) "+$diff%" else "$diff%"
                                            val diffColor = if (diff >= 0) EmeraldAccent else RedCost
                                            Text(
                                                text = "$diffText ahead of industry average",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = diffColor
                                            )
                                        }
                                    }
                                }
                                
                                // Clean, elegant circular progress meter
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color.White.copy(alpha = 0.01f), CircleShape)
                                        .border(BorderStroke(1.dp, CardBorderColor), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        progress = { scoreA / 100f },
                                        color = GoldHighlight,
                                        strokeWidth = 3.dp,
                                        trackColor = Color.White.copy(alpha = 0.05f),
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = "$scoreA%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldHighlight
                                    )
                                }
                            }
                        }
                    }
                }

                // DUAL SELECTORS ROW (Bloomberg & Stripe Reference)
                item {
                    var menuAExpanded by remember { mutableStateOf(false) }
                    var menuBExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Selector Company A
                        Box(modifier = Modifier.weight(1f)) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { menuAExpanded = true },
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                border = BorderStroke(1.dp, CardBorderColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "SELECTED TARGET",
                                        fontSize = 8.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = companyA.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = TextSecondary
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = menuAExpanded,
                                onDismissRequest = { menuAExpanded = false },
                                modifier = Modifier
                                    .background(DarkSurface)
                                    .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(8.dp))
                            ) {
                                allCompanies.forEach { comp ->
                                    DropdownMenuItem(
                                        text = { Text(comp.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            viewModel.selectCompany(comp)
                                            menuAExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Selector Company B
                        Box(modifier = Modifier.weight(1f)) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { menuBExpanded = true },
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                border = BorderStroke(1.dp, CardBorderColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "BENCHMARK AGAINST",
                                        fontSize = 8.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = companyB.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = TextSecondary
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = menuBExpanded,
                                onDismissRequest = { menuBExpanded = false },
                                modifier = Modifier
                                    .background(DarkSurface)
                                    .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(8.dp))
                            ) {
                                allCompanies.forEach { comp ->
                                    DropdownMenuItem(
                                        text = { Text(comp.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            selectedCompB = comp
                                            menuBExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ==================== LUXURY KPI BOARD (Bloomberg & Stripe Reference) ====================
                item {
                    val capitalInvestment = (potentialSavings * 0.18).toInt().coerceAtLeast(15000)
                    val annualizedSavings = potentialSavings.coerceAtLeast(45000)
                    val roiPercent = ((annualizedSavings.toFloat() / capitalInvestment) * 100).toInt()
                    val paybackMonths = String.format(Locale.ROOT, "%.1f", (capitalInvestment.toFloat() / (annualizedSavings / 12f)))

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 16.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.02f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        val isWide = maxWidth > 600.dp
                        
                        if (isWide) {
                            // 4 columns in 1 row for wide screens (Bloomberg terminal layout)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KpiMetricItem(
                                    value = "₼${annualizedSavings / 1000}k",
                                    label = "ANNUAL OPPORTUNITY",
                                    modifier = Modifier.weight(1f),
                                    accentColor = EmeraldAccent
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(48.dp)
                                        .background(Color.White.copy(alpha = 0.06f))
                                )
                                KpiMetricItem(
                                    value = "$roiPercent%",
                                    label = "ROI",
                                    modifier = Modifier.weight(1f),
                                    accentColor = GoldHighlight
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(48.dp)
                                        .background(Color.White.copy(alpha = 0.06f))
                                )
                                KpiMetricItem(
                                    value = "$paybackMonths Mo",
                                    label = "PAYBACK PERIOD",
                                    modifier = Modifier.weight(1f),
                                    accentColor = BlueAccent
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(48.dp)
                                        .background(Color.White.copy(alpha = 0.06f))
                                )
                                KpiMetricItem(
                                    value = "$scoreA",
                                    label = "COMPETITIVE SCORE",
                                    modifier = Modifier.weight(1f),
                                    accentColor = Color.White
                                )
                            }
                        } else {
                            // 2x2 grid for compact mobile screens
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    KpiMetricItem(
                                        value = "₼${annualizedSavings / 1000}k",
                                        label = "ANNUAL OPPORTUNITY",
                                        modifier = Modifier.weight(1f),
                                        accentColor = EmeraldAccent
                                    )
                                    KpiMetricItem(
                                        value = "$roiPercent%",
                                        label = "ROI",
                                        modifier = Modifier.weight(1f),
                                        accentColor = GoldHighlight
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color.White.copy(alpha = 0.06f))
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    KpiMetricItem(
                                        value = "$paybackMonths Mo",
                                        label = "PAYBACK PERIOD",
                                        modifier = Modifier.weight(1f),
                                        accentColor = BlueAccent
                                    )
                                    KpiMetricItem(
                                        value = "$scoreA",
                                        label = "COMPETITIVE SCORE",
                                        modifier = Modifier.weight(1f),
                                        accentColor = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // ==================== STORY SECTION 1: CURRENT POSITION ====================
                item {
                    StorySectionConnector(label = "CURRENT POSITION")
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(32.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CURRENT POSTURE BASELINE",
                                    fontSize = 11.sp,
                                    color = BlueAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.5.sp
                                )
                                Surface(
                                    color = BlueAccent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = cisCategory.uppercase(Locale.ROOT),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BlueAccent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Evaluating ${companyA.name}'s current digital core and cognitive maturity baseline.",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 24.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Visual progress score bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Cognitive Integration Index (CIS)", fontSize = 12.sp, color = TextSecondary)
                                        Text("$scoreA / 100", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x11FFFFFF))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(scoreA / 100f)
                                                .fillMaxHeight()
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(BlueAccent, EmeraldAccent)
                                                    )
                                                )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = Color(0x0EFFFFFF), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "AI COGNITIVE OBSERVATIONS",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val cloudInsight = if (companyA.currentStack.lowercase(Locale.ROOT).contains("on-prem") ||
                                companyA.currentStack.lowercase(Locale.ROOT).contains("on-premises") ||
                                companyA.currentStack.lowercase(Locale.ROOT).contains("legacy")) {
                                "Cloud core is currently operating below banking industry benchmarks (largely on-premise components)."
                            } else {
                                "Core cloud infrastructure is active, though missing optimized automated cognitive request gateways."
                            }
                            val automationInsight = "Engineering workflow automation is under-utilized, creating release-velocity bottleneck."
                            val savingsPct = (companyA.savingPercent * 100).toInt()
                            val aiInsight = "Gemini cognitive layers show a projected operational runrate reduction of $savingsPct%."

                            listOf(cloudInsight, automationInsight, aiInsight).forEach { observation ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingFlat,
                                        contentDescription = null,
                                        tint = BlueAccent,
                                        modifier = Modifier.size(16.dp).offset(y = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = observation,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // ==================== STORY SECTION 2: COMPETITIVE GAP ====================
                item {
                    StorySectionConnector(label = "COMPETITIVE GAP")
                }

                item {
                    val isWinningA = scoreA >= scoreB
                    val gap = Math.abs(scoreA - scoreB)
                    val winner = if (scoreA >= scoreB) companyA else companyB
                    val loser = if (scoreA >= scoreB) companyB else companyA

                    val winnerAdvantages = when (winner.id) {
                        "pasha_bank" -> listOf(
                            "Superb Cloud-Native Integration",
                            "AI-Assisted Operations & Automation",
                            "Rapid Delivery Cycles & Automated QA",
                            "Minimal Technical Debt Footprint"
                        )
                        "kapital_bank" -> listOf(
                            "Highly Scalable Enterprise Architecture",
                            "Strong Microservices Paradigm Signature",
                            "Advanced Cloud-Hybrid Infrastructure",
                            "Consistent Digital Execution Flow"
                        )
                        else -> listOf(
                            "Agile Framework Adoption Patterns",
                            "Modern Headless Content Topologies",
                            "Highly Optimized Static Asset Loading",
                            "Direct Client Cache Optimization"
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(32.dp)) {
                            Text(
                                text = "COMPETITIVE GAP DIFFERENTIAL",
                                fontSize = 11.sp,
                                color = BlueAccent,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isWinningA) {
                                    "${companyA.name} currently commands a technological maturity lead of $gap% over ${companyB.name}."
                                } else {
                                    "${companyA.name} trails ${companyB.name} by a technological maturity gap of $gap%."
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 24.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // VS Comparison row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Company A
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = companyA.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Score: $scoreA / 100", fontSize = 13.sp, color = if (isWinningA) EmeraldAccent else TextSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x0EFFFFFF))
                                        .border(BorderStroke(1.dp, Color(0x22FFFFFF)), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("VS", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }

                                // Company B
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = companyB.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Score: $scoreB / 100", fontSize = 13.sp, color = if (!isWinningA) EmeraldAccent else TextSecondary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = Color(0x0EFFFFFF), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Strengths and vulnerabilities
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "WINNER ADVANTAGES",
                                        fontSize = 9.sp,
                                        color = EmeraldAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    winnerAdvantages.take(3).forEach { adv ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(adv, fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CHALLENGER GAP",
                                        fontSize = 9.sp,
                                        color = AmberWarning,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val weaknesses = loser.weakness.split(". ").filter { it.isNotBlank() }
                                    if (weaknesses.isNotEmpty()) {
                                        weaknesses.take(2).forEach { weak ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text("•", fontSize = 12.sp, color = AmberWarning, modifier = Modifier.width(10.dp))
                                                Text(weak.trim().removeSuffix("."), fontSize = 11.sp, color = TextSecondary, lineHeight = 14.sp)
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("•", fontSize = 12.sp, color = AmberWarning, modifier = Modifier.width(10.dp))
                                            Text("High on-premises legacy dependencies and data isolation issues.", fontSize = 11.sp, color = TextSecondary, lineHeight = 14.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(color = Color(0x0EFFFFFF), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Radar chart title
                            Text(
                                text = "MATURITY RADAR SURFACE METRICS",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Unified Radar Chart (Premium Redesign: Glass Container, Glowing Paths, Mathematical Labels, and Winner Highlight)
                            val valsA = listOf(aiReadinessA.toFloat(), automationA.toFloat(), cloudAdoptionA.toFloat(), technologyModernityA.toFloat(), innovationA)
                            val valsB = listOf(aiReadinessB.toFloat(), automationB.toFloat(), cloudAdoptionB.toFloat(), technologyModernityB.toFloat(), innovationB)

                            // High-end spring animation to make the paths bloom smoothly and settle elegantly
                            val radarAnimProgress by animateFloatAsState(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "RadarChartAnim"
                            )

                            val isAWinner = scoreA >= scoreB
                            // Highlight the winner with glowing GoldHighlight and runner-up with a sleek Slate color
                            val colorA = if (isAWinner) GoldHighlight else Color(0x9994A3B8)
                            val colorB = if (isAWinner) Color(0x9994A3B8) else GoldHighlight

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .glassCard(cornerRadius = 20.dp)
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.08f),
                                                Color.White.copy(alpha = 0.02f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(290.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val center = Offset(size.width / 2f, size.height / 2f)
                                            val radius = size.minDimension * 0.35f
                                            val angleStep = 2 * Math.PI / 5

                                            // Outer concentric rings with modern subtle transparency
                                            for (level in listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f)) {
                                                val levelRadius = radius * level
                                                val path = Path().apply {
                                                    for (i in 0 until 5) {
                                                        val angle = i * angleStep - Math.PI / 2
                                                        val x = center.x + levelRadius * cos(angle).toFloat()
                                                        val y = center.y + levelRadius * sin(angle).toFloat()
                                                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                                                    }
                                                    close()
                                                }
                                                val ringAlpha = if (level == 1.0f) 0.15f else 0.05f
                                                drawPath(
                                                    path = path, 
                                                    color = Color.White.copy(alpha = ringAlpha), 
                                                    style = Stroke(width = if (level == 1.0f) 1.5.dp.toPx() else 1.dp.toPx())
                                                )
                                            }

                                            // Radial axes
                                            for (i in 0 until 5) {
                                                val angle = i * angleStep - Math.PI / 2
                                                drawLine(
                                                    color = Color.White.copy(alpha = 0.06f),
                                                    start = center,
                                                    end = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat()),
                                                    strokeWidth = 1.dp.toPx()
                                                )
                                            }

                                            // Plot A - Animated with core and glow stroke
                                            val pathA = Path().apply {
                                                for (i in 0 until 5) {
                                                    val angle = i * angleStep - Math.PI / 2
                                                    val r = radius * (valsA[i] / 100f).coerceIn(0.1f, 1.0f) * radarAnimProgress
                                                    if (i == 0) moveTo(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat())
                                                    else lineTo(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat())
                                                }
                                                close()
                                            }
                                            // Soft solid background fill
                                            drawPath(pathA, colorA.copy(alpha = if (isAWinner) 0.08f else 0.02f))
                                            // Core solid line
                                            drawPath(pathA, colorA, style = Stroke(width = 1.5.dp.toPx()))

                                            // Plot B - Animated
                                            val pathB = Path().apply {
                                                for (i in 0 until 5) {
                                                    val angle = i * angleStep - Math.PI / 2
                                                    val r = radius * (valsB[i] / 100f).coerceIn(0.1f, 1.0f) * radarAnimProgress
                                                    if (i == 0) moveTo(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat())
                                                    else lineTo(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat())
                                                }
                                                close()
                                            }
                                            // Soft solid background fill
                                            drawPath(pathB, colorB.copy(alpha = if (!isAWinner) 0.08f else 0.02f))
                                            // Core solid line
                                            drawPath(pathB, colorB, style = Stroke(width = 1.5.dp.toPx()))

                                            // Plot Nodes
                                            for (i in 0 until 5) {
                                                val angle = i * angleStep - Math.PI / 2
                                                
                                                // Node A
                                                val rA = radius * (valsA[i] / 100f).coerceIn(0.1f, 1.0f) * radarAnimProgress
                                                val nodeCenterA = Offset(center.x + rA * cos(angle).toFloat(), center.y + rA * sin(angle).toFloat())
                                                drawCircle(colorA, radius = 3.dp.toPx(), center = nodeCenterA)

                                                // Node B
                                                val rB = radius * (valsB[i] / 100f).coerceIn(0.1f, 1.0f) * radarAnimProgress
                                                val nodeCenterB = Offset(center.x + rB * cos(angle).toFloat(), center.y + rB * sin(angle).toFloat())
                                                drawCircle(colorB, radius = 3.dp.toPx(), center = nodeCenterB)
                                            }
                                        }

                                        // Mathematical, fully responsive label positioning overlay with side-by-side values and winning indicators
                                        val radiusDp = 92.dp
                                        val labelNames = listOf("AI Readiness", "Automation", "Cloud Adoption", "Tech Modernity", "Innovation")
                                        
                                        labelNames.forEachIndexed { i, label ->
                                            val angle = i * (2 * Math.PI / 5) - Math.PI / 2
                                            
                                            // Offset labels slightly outside of the outermost ring (1.36x)
                                            val distanceMultiplier = 1.36f
                                            val xOffset = (radiusDp.value * distanceMultiplier * cos(angle)).dp
                                            val yOffset = (radiusDp.value * distanceMultiplier * sin(angle)).dp

                                            Column(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .offset(x = xOffset, y = yOffset),
                                                horizontalAlignment = when {
                                                    cos(angle) > 0.1 -> Alignment.Start
                                                    cos(angle) < -0.1 -> Alignment.End
                                                    else -> Alignment.CenterHorizontally
                                                }
                                            ) {
                                                Text(
                                                    text = label.uppercase(Locale.ROOT),
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    fontFamily = FontFamily.Monospace,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    val valA = valsA[i].toInt()
                                                    val valB = valsB[i].toInt()
                                                    Text(
                                                        text = "$valA%",
                                                        fontSize = 9.sp,
                                                        fontWeight = if (valA >= valB) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (valA >= valB) colorA else colorA.copy(alpha = 0.6f)
                                                    )
                                                    Text(
                                                        text = "/",
                                                        fontSize = 8.sp,
                                                        color = Color.White.copy(alpha = 0.2f)
                                                    )
                                                    Text(
                                                        text = "$valB%",
                                                        fontSize = 9.sp,
                                                        fontWeight = if (valB >= valA) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (valB >= valA) colorB else colorB.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Custom Legend with Dynamic Winner Indicators
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(colorA)
                                            )
                                            Text(
                                                text = companyA.name.uppercase(Locale.ROOT),
                                                fontSize = 10.sp,
                                                color = if (isAWinner) Color.White else TextSecondary,
                                                fontWeight = if (isAWinner) FontWeight.ExtraBold else FontWeight.Medium
                                            )
                                            if (isAWinner) {
                                                Surface(
                                                    color = GoldHighlight.copy(alpha = 0.15f),
                                                    border = BorderStroke(1.dp, GoldHighlight.copy(alpha = 0.3f)),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "WINNER",
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GoldHighlight,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.width(32.dp))
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(colorB)
                                            )
                                            Text(
                                                text = companyB.name.uppercase(Locale.ROOT),
                                                fontSize = 10.sp,
                                                color = if (!isAWinner) Color.White else TextSecondary,
                                                fontWeight = if (!isAWinner) FontWeight.ExtraBold else FontWeight.Medium
                                            )
                                            if (!isAWinner) {
                                                Surface(
                                                    color = GoldHighlight.copy(alpha = 0.15f),
                                                    border = BorderStroke(1.dp, GoldHighlight.copy(alpha = 0.3f)),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "WINNER",
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GoldHighlight,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== STORY SECTION 3: BUSINESS IMPACT ====================
                item {
                    StorySectionConnector(label = "BUSINESS IMPACT")
                }

                item {
                    val debt = TechStackScanner.getTechnicalDebtScore(companyA)
                    val wasteCost = (potentialSavings * 0.4).toInt()
                    val maintenanceCost = (companyA.costBefore * 0.25).toInt()
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(32.dp)) {
                            Text(
                                text = "BUSINESS IMPACT & INACTION COSTS",
                                fontSize = 11.sp,
                                color = BlueAccent,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "The high cost of maintaining ${companyA.name}'s legacy architectural profile and technical debt.",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 24.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            // Score Grid of Impact Metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("TECHNICAL DEBT INDEX", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$debt %", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Structural drag on release speed", fontSize = 9.sp, color = TextSecondary)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("ESTIMATED HOURLY LEAK", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("₼${String.format(Locale.ROOT, "%,d", wasteCost)} / yr", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Lost through manual operations", fontSize = 9.sp, color = TextSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = Color(0x0EFFFFFF), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "COMPETITIVE INEFFICIENCY ATTRIBUTION",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Detailed line items
                            listOf(
                                "Server Hosting Surcharges" to "₼${String.format(Locale.ROOT, "%,d", maintenanceCost)}/yr spent on oversized, redundant physical boxes.",
                                "Manual Operations Friction" to "Approx 40 engineering hours per week wasted on custom release patching.",
                                "Slow Support Turnaround" to "4.5 minutes average turnaround delay due to localized database siloing."
                            ).forEach { item ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.first, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(item.second, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
                                }
                            }
                        }
                    }
                }

                // ==================== STORY SECTION 4: RECOMMENDATIONS ====================
                item {
                    StorySectionConnector(label = "RECOMMENDATIONS")
                }

                item {
                    val savings = companyA.costBefore - companyA.costAfterAi
                    
                    val immediateWinsBenefit = "₼" + String.format(Locale.ROOT, "%,d", (savings * 0.4).toInt())
                    val plan30DaysBenefit = "₼" + String.format(Locale.ROOT, "%,d", (savings * 0.3).toInt())
                    val plan90DaysBenefit = "₼" + String.format(Locale.ROOT, "%,d", (savings * 0.2).toInt())
                    val plan180DaysBenefit = "₼" + String.format(Locale.ROOT, "%,d", (savings * 0.1).toInt())

                    val overtakePlan = listOf(
                        Triple("Immediate Wins (1-7 Days)", "Establish unified AI-powered client routing proxy gates to streamline high-traffic digital touchpoints.", "Benefit: $immediateWinsBenefit/yr"),
                        Triple("30-Day Transition Phase", "Deconstruct legacy relational database silos and port to highly performant serverless cloud databases.", "Benefit: $plan30DaysBenefit/yr"),
                        Triple("90-Day Modernization Stage", "Onboard in-house development engineering squads to advanced Gemini/Copilot coding platforms.", "Benefit: $plan90DaysBenefit/yr"),
                        Triple("180-Day Complete Integration", "Formally deprecate physical on-premises monolithic virtual servers, capturing ultimate efficiency goals.", "Benefit: $plan180DaysBenefit/yr")
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(32.dp)) {
                            Text(
                                text = "STRATEGIC TARGET RECOMMENDATIONS",
                                fontSize = 11.sp,
                                color = BlueAccent,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "A phased, high-impact consulting action plan to outperform and close the gap with ${companyB.name}.",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 24.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            overtakePlan.forEachIndexed { index, stage ->
                                Column(modifier = Modifier.padding(vertical = 10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(BlueAccent.copy(alpha = 0.15f))
                                                    .border(BorderStroke(1.dp, BlueAccent), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BlueAccent,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = stage.first,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        Text(
                                            text = stage.third,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldAccent,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = stage.second,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.padding(start = 30.dp)
                                    )
                                    if (index < overtakePlan.size - 1) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = Color(0x08FFFFFF), thickness = 0.5.dp, modifier = Modifier.padding(start = 30.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== STORY SECTION 5: ROI ====================
                item {
                    StorySectionConnector(label = "ROI")
                }

                item {
                    val capitalInvestment = (potentialSavings * 0.18).toInt().coerceAtLeast(15000)
                    val annualizedSavings = potentialSavings.coerceAtLeast(45000)
                    val roiPercent = ((annualizedSavings.toFloat() / capitalInvestment) * 100).toInt()
                    val paybackMonths = String.format(Locale.ROOT, "%.1f", (capitalInvestment.toFloat() / (annualizedSavings / 12f)))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(32.dp)) {
                            Text(
                                text = "HIGH-FIDELITY RETURN MODEL",
                                fontSize = 11.sp,
                                color = BlueAccent,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Financial cost-reduction dynamics and amortization of the recommended transition plan.",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 24.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text("CAPITAL OUTLAY", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("₼${String.format(Locale.ROOT, "%,d", capitalInvestment)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Hardware & consulting services setup", fontSize = 9.sp, color = TextSecondary)
                                    }
                                    
                                    Column {
                                        Text("PAYBACK AMORTIZATION", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("$paybackMonths Months", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Full capital amortization timeline", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text("PROJECTED SAVINGS", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("₼${String.format(Locale.ROOT, "%,d", annualizedSavings)} / yr", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Amortized operational cost containment", fontSize = 9.sp, color = TextSecondary)
                                    }

                                    Column {
                                        Text("PROJECTED NET ROI", fontSize = 8.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("$roiPercent %", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GoldHighlight)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("Net strategic return on investment", fontSize = 9.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // ==================== STORY SECTION 6: FUTURE STATE ====================
                item {
                    StorySectionConnector(label = "FUTURE STATE")
                }

                item {
                    TechnologyEvolutionTimelineSection(companyA = companyA, scoreA = scoreA)
                }

                // Executive Consulting Report Trigger Button at bottom
                item {
                    Button(
                        onClick = { isReportMode = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("enter_report_mode_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = BlueAccent
                        ),
                        border = BorderStroke(1.dp, BlueAccent.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Report Mode",
                            modifier = Modifier.size(18.dp),
                            tint = BlueAccent
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GENERATE FULL EXECUTIVE DOSSIER REPORT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StorySectionConnector(
    modifier: Modifier = Modifier,
    label: String? = null
) {
    Spacer(modifier = modifier.height(24.dp))
}

@Composable
fun BulletPointItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(BlueAccent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ConfidenceSourceItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(10.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 10.sp,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ComponentScoreRow(label: String, score: Int, color: Color) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "ComponentScoreAnimation"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = "${animatedScore.toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedScore / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Color(0x0EFFFFFF)
        )
    }
}

@Composable
fun BadgeIndicator(label: String, valStr: String) {
    val containerColor = when (valStr) {
        "High" -> RedCost.copy(alpha = 0.12f)
        "Medium" -> AmberWarning.copy(alpha = 0.12f)
        else -> BlueAccent.copy(alpha = 0.12f)
    }
    val contentColor = when (valStr) {
        "High" -> RedCost
        "Medium" -> AmberWarning
        else -> BlueAccent
    }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )
        Surface(
            color = containerColor,
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(0.5.dp, contentColor.copy(alpha = 0.3f))
        ) {
            Text(
                text = valStr.uppercase(Locale.ROOT),
                color = contentColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ActionPlanTimelineCard(company: Company) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            val planSteps = listOf(
                Triple(
                    "30-DAY PLAN",
                    "BASELINE AUDIT & DATABASE SANITATION",
                    "Transition legacy self-hosted database schemas to fully isolated cloud PostgreSQL instances. Establish unified IAM permissions, configure serverless API routers, and map operational baselines."
                ),
                Triple(
                    "90-DAY PLAN",
                    "AI ORCHESTRATION & AGENT CO-PILOT SANDBOX",
                    "Embed secure generative LLM support routers to automate up to 60% of tier-1 localized inquiries. Provision localized embedding indexes and deploy team-wide AI coding assistance tools."
                ),
                Triple(
                    "180-DAY PLAN",
                    "TOTAL CLOUD SWITCH-OVER & EX-ANTE AUDITING",
                    "Decommission remaining physical virtual machine nodes, shifting computation entirely to edge functions and serverless runtimes. Finalize ex-ante cost audits showing projected savings."
                )
            )

            planSteps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(28.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(BlueAccent.copy(alpha = 0.15f))
                                .border(BorderStroke(1.dp, BlueAccent), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                color = BlueAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        if (index < planSteps.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(100.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(BlueAccent, Color.Transparent)
                                        )
                                    )
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = step.first,
                                fontSize = 10.sp,
                                color = BlueAccent,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = step.second,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = step.third,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

fun getStrategicRecommendations(company: Company): List<StrategicRecommendation> {
    val hasWordPress = company.currentStack.lowercase(Locale.ROOT).contains("wordpress")
    val hasShopify = company.currentStack.lowercase(Locale.ROOT).contains("shopify")
    val hasLaravel = company.currentStack.lowercase(Locale.ROOT).contains("laravel")
    
    val savings = company.costBefore - company.costAfterAi
    
    return when {
        hasWordPress -> listOf(
            StrategicRecommendation(
                title = "Static & Headless Architecture Migration",
                detail = "Deconstruct legacy WordPress monolithic architectures. Convert Content management systems into static pipelines integrated with Next.js/React frontend elements, delivering rapid pageload speed and eliminating database security gaps.",
                priority = "High",
                impact = "High",
                expectedBenefit = "₼${String.format("%,d", (savings * 0.5 * 12).toInt())} / yr"
            ),
            StrategicRecommendation(
                title = "Gemini Support Conversational Flow",
                detail = "Deploy customized LLM support routers to address consumer inquiries. Automates high-frequency requests securely while interfacing directly with localized customer databases.",
                priority = "High",
                impact = "Medium",
                expectedBenefit = "₼${String.format("%,d", (savings * 0.3 * 12).toInt())} / yr"
            ),
            StrategicRecommendation(
                title = "Team Copilot Platform Onboarding",
                detail = "Equip in-house developer cadres with AI completion and generation engines, lowering release-cycle overhead and QA debug bottlenecks.",
                priority = "Medium",
                impact = "Medium",
                expectedBenefit = "₼${String.format("%,d", (savings * 0.2 * 12).toInt())} / yr"
            )
        )
        hasShopify -> listOf(
            StrategicRecommendation(
                title = "Headless Storefront Conversions",
                detail = "Transition standard templates to Next.js custom heads connecting via Shopify Storefront APIs. Eliminates high-tier plugin overheads and monthly transaction fees.",
                priority = "High",
                impact = "High",
                expectedBenefit = "₼${String.format("%,d", (savings * 0.6 * 12).toInt())} / yr"
            ),
            StrategicRecommendation(
                title = "Dynamic LLM Recommendations Indexing",
                detail = "Embed vector embeddings based product suggestions. Elevates checkout transaction ratios up to 22% compared to standard keyword indexes.",
                priority = "Medium",
                impact = "High",
                expectedBenefit = "₼${String.format("%,d", (savings * 0.25 * 12).toInt())} / yr"
            ),
            StrategicRecommendation(
                title = "Serverless Payment Gateway Integration",
                detail = "Develop localized gateway pipelines (e.g. Pashapay, GoldenPay) inside lightweight serverless handlers to decrease billing discrepancies.",
                priority = "Low",
                impact = "Medium",
                expectedBenefit = "₼${String.format("%,d", (savings * 0.15 * 12).toInt())} / yr"
            )
        )
        hasLaravel -> listOf(
            StrategicRecommendation(
                title = "Monolithic Decomposition into Serverless Handlers",
                detail = "Refactor legacy PHP backends into containerized microservices hosted on AWS Fargate or serverless containers. Mitigates high idle server pricing during off-peak times.",
                priority = "High",
                impact = "High",
                expectedBenefit = "₼${String.format("%,d", (savings * 0.5 * 12).toInt())} / yr"
            ),
            StrategicRecommendation(
                title = "LLM-Powered Operational Pipeline Routing",
                detail = "Incorporate custom generative analytical handlers to evaluate internal reporting databases, bypassing slow human audit pipelines.",
                priority = "Medium",
                impact = "Medium",
                expectedBenefit = "₼${String.format("%,d", (savings * 0.3 * 12).toInt())} / yr"
            ),
            StrategicRecommendation(
                title = "Fully Managed Database Consolidation",
                detail = "Migrate unmanaged self-hosted SQL nodes to secure serverless database structures with integrated point-in-time recovery setups.",
                priority = "Medium",
                impact = "Medium",
                expectedBenefit = "₼${String.format("%,d", (savings * 0.2 * 12).toInt())} / yr"
            )
        )
        else -> listOf(
            StrategicRecommendation(
                title = "Unified Conversational Gemini Customer Orchestration",
                detail = "Deploy secure, enterprise-grade AI chat assistants capable of resolving up to 65% of customer routing queries without human delays.",
                priority = "High",
                impact = "High",
                expectedBenefit = "₼${String.format("%,d", (savings * 0.55 * 12).toInt())} / yr"
            ),
            StrategicRecommendation(
                title = "Edge Compute Containerization",
                detail = "Transition standard hosting VMs to fully serverless auto-scaling node pools, achieving zero-cost operations during idle cycles.",
                priority = "High",
                impact = "High",
                expectedBenefit = "₼${String.format("%,d", (savings * 0.3 * 12).toInt())} / yr"
            ),
            StrategicRecommendation(
                title = "Generative Platform QA Scanning",
                detail = "Integrate automatic LLM-driven integration test creation suites. Drops code debug cycles from days to minutes while keeping complete codebase test coverage.",
                priority = "Medium",
                impact = "Medium",
                expectedBenefit = "₼${String.format("%,d", (savings * 0.15 * 12).toInt())} / yr"
            )
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    numericValue: Float? = null,
    valueSuffix: String = "",
    value: String = "",
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "MetricCardScale"
    )
    
    val containerColor by animateColorAsState(
        targetValue = if (isPressed) color.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "MetricCardColor"
    )

    val displayValue = if (numericValue != null) {
        if (valueSuffix.contains("ROI") || valueSuffix.contains("x")) {
            String.format(Locale.US, "%.1fx ROI", numericValue)
        } else if (valueSuffix.contains("%")) {
            "${numericValue.toInt()}%"
        } else {
            val formattedNum = String.format(Locale.US, "%,d", numericValue.toInt())
            "$formattedNum ₼"
        }
    } else {
        value
    }

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {}
            .glassCard(cornerRadius = 16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Icon(icon, contentDescription = title, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
            }
            Text(
                text = displayValue,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextSecondary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun LuxuryExecutiveHeader(
    activeCompany: Company,
    allCompanies: List<Company>,
    cisScore: Int,
    cisCategory: String,
    confidenceLevel: Int,
    onCompanySelected: (Company) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Calculate industry ranking dynamically compared to all companies
    val companyScores = remember(allCompanies) {
        allCompanies.map { comp ->
            val compAiReadiness = TechStackScanner.calculateReadinessScore(comp)
            val compTechDebt = TechStackScanner.getTechnicalDebtScore(comp)
            val compAutomation = ((100 - compTechDebt) * 1.05f).toInt().coerceIn(15, 95)
            val compCloudAdoption = if (comp.currentStack.lowercase(Locale.ROOT).contains("on-premises") || 
                                    comp.currentStack.lowercase(Locale.ROOT).contains("on-prem")) {
                28
            } else if (comp.currentStack.lowercase(Locale.ROOT).contains("cloud") || 
                       comp.currentStack.lowercase(Locale.ROOT).contains("serverless") || 
                       comp.currentStack.lowercase(Locale.ROOT).contains("fargate") || 
                       comp.currentStack.lowercase(Locale.ROOT).contains("gcp")) {
                88
            } else {
                58
            }
            val compTechModernity = (100 - compTechDebt).coerceIn(12, 95)
            val compCis = ((compAiReadiness + compAutomation + compCloudAdoption + compTechModernity) / 4).coerceIn(0, 100)
            comp.id to compCis
        }.sortedByDescending { it.second }
    }

    val rankIndex = companyScores.indexOfFirst { it.first == activeCompany.id }
    val industryRank = if (rankIndex != -1) rankIndex + 1 else 1
    val totalRanked = companyScores.size

    val formattedTime = remember(activeCompany) {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm z", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        sdf.format(java.util.Date())
    }

    // Infinite transition for high-fidelity subtle shimmer animations
    val infiniteTransition = rememberInfiniteTransition(label = "luxury_header_effects")
    
    // Smooth, slow movement of the specular metallic shine highlight across the glass body
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_offset"
    )

    // Bloomberg telemetry live system connection blinking dot
    val feedIndicatorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "feed_alpha"
    )

    // Animated linear gradient mimicking luxury physical frosted glass under studio lighting
    val animatedGlassBrush = Brush.linearGradient(
        colors = listOf(
            Color(0x06FFFFFF), // Frosted body base
            Color(0x1AFFFFFF), // Moving bright silver metallic reflection line
            Color(0x04FFFFFF), // Mid translucent body
            Color(0x0DFFFFFF), // Subtle ambient glow point
            Color(0x02FFFFFF)  // Dark shadow bleed
        ),
        start = Offset(shimmerOffset * 900f, 0f),
        end = Offset((shimmerOffset + 0.5f) * 900f, 650f)
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(animatedGlassBrush)
                .border(
                    width = 1.2.dp,
                    brush = MetallicBorderBrush,
                    shape = RoundedCornerShape(20.dp)
                )
                .premiumPressScale { expanded = true }
                .testTag("luxury_executive_header"),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Meta Row: Bloomberg Terminal Live Feeds Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent.copy(alpha = feedIndicatorAlpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BLOOMBERG COGNITIVE SUITE",
                            fontSize = 8.5.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )
                    }
                    
                    Text(
                        text = "REAL-TIME FEED",
                        fontSize = 8.5.sp,
                        color = BlueAccent.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Company Name Header Section (with Dropdown selector indicator)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CORPORATE ENTITY",
                            fontSize = 9.sp,
                            color = BlueAccent,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeCompany.name.uppercase(Locale.ROOT),
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.3.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch profile",
                                tint = BlueAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Luxury Portfolio indicator badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x0CFFFFFF),
                        border = BorderStroke(1.dp, CardBorderColor)
                    ) {
                        Text(
                            text = "PORTFOLIO",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            fontSize = 9.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = CardBorderColor, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // Bloomberg-Style Dynamic Executive Grid Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Column A: Score & Rank
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "COMPETITIVE INTEL SCORE",
                            fontSize = 8.5.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%02d", cisScore),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "/100",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 3.dp, start = 2.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "INDUSTRY RANKING",
                            fontSize = 8.5.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.US, "#%02d OF %02d", industryRank, totalRanked),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldHighlight, // luxurious champagne gold
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Vertical high-end divider
                    Box(
                        modifier = Modifier
                            .height(110.dp)
                            .width(1.dp)
                            .background(Color(0x0CFFFFFF))
                    )
                    Spacer(modifier = Modifier.width(16.dp))

                    // Column B: Status, Confidence & Update Timestamp
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text(
                            text = "COMPETITIVE POSITION",
                            fontSize = 8.5.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        val positionBadgeColor = when (cisCategory) {
                            "Digital Pioneer" -> EmeraldAccent
                            "Competitively Aligned" -> BlueAccent
                            "Transitionary" -> AmberWarning
                            else -> RedCost
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = positionBadgeColor.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, positionBadgeColor.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = cisCategory.uppercase(Locale.ROOT),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = positionBadgeColor
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "CONFIDENCE LEVEL",
                                    fontSize = 8.5.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "$confidenceLevel% (HIGH-Q)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "LAST INTEL UPDATE",
                            fontSize = 8.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formattedTime.uppercase(Locale.ROOT),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Expanded interactive dropdown menu to switch selected entity
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(DarkBg)
                .border(BorderStroke(1.dp, CardBorderColor))
        ) {
            allCompanies.forEach { comp ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            text = comp.name, 
                            color = if (comp.id == activeCompany.id) BlueAccent else Color.White, 
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ) 
                    },
                    onClick = {
                        onCompanySelected(comp)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ==================== EXECUTIVE REPORT MODE ====================

enum class FirmStyle {
    MCKINSEY, BCG, ACCENTURE, BLOOMBERG
}

@Composable
fun ExecutiveReportView(
    activeCompany: Company,
    onExitReport: () -> Unit,
    cisScore: Int,
    cisCategory: String,
    recommendations: List<StrategicRecommendation>,
    potentialSavings: Int,
    modifier: Modifier = Modifier
) {
    var selectedFirm by remember { mutableStateOf(FirmStyle.MCKINSEY) }
    var isPresentationMode by remember { mutableStateOf(false) }
    
    // Theme configurations depending on firm (with smooth animated transitions)
    val targetBg = when (selectedFirm) {
        FirmStyle.MCKINSEY -> Color(0xFF040A15) // Deep Blue Ink
        FirmStyle.BCG -> Color(0xFF020905)      // Deep Forest Green
        FirmStyle.ACCENTURE -> Color(0xFF07030F) // Deep Violet Dark
        FirmStyle.BLOOMBERG -> Color(0xFF020408) // Bloomberg Obsidian Black
    }
    val themeBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "ThemeBgTransition"
    )
    
    val targetAccent = when (selectedFirm) {
        FirmStyle.MCKINSEY -> Color(0xFF0A84FF) // McKinsey Azure
        FirmStyle.BCG -> Color(0xFF00E676)      // BCG Mint Emerald
        FirmStyle.ACCENTURE -> Color(0xFF9E00FF) // Accenture Hyper Violet
        FirmStyle.BLOOMBERG -> Color(0xFFFF5000) // Bloomberg Neon Orange Accent
    }
    val themeAccent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "ThemeAccentTransition"
    )
    
    val targetAccentMuted = when (selectedFirm) {
        FirmStyle.MCKINSEY -> Color(0x1F0A84FF)
        FirmStyle.BCG -> Color(0x1F00E676)
        FirmStyle.ACCENTURE -> Color(0x1F9E00FF)
        FirmStyle.BLOOMBERG -> Color(0x1FFF5000)
    }
    val themeAccentMuted by animateColorAsState(
        targetValue = targetAccentMuted,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "ThemeAccentMutedTransition"
    )

    val targetDivider = when (selectedFirm) {
        FirmStyle.MCKINSEY -> Color(0x22FFFFFF)
        FirmStyle.BCG -> Color(0x1DFFFFFF)
        FirmStyle.ACCENTURE -> Color(0x26FFFFFF)
        FirmStyle.BLOOMBERG -> Color(0x2BFFFFFF)
    }
    val themeDivider by animateColorAsState(
        targetValue = targetDivider,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "ThemeDividerTransition"
    )
    
    val signatureName = when (selectedFirm) {
        FirmStyle.MCKINSEY -> "McKinsey & Company"
        FirmStyle.BCG -> "BOSTON CONSULTING GROUP"
        FirmStyle.ACCENTURE -> "accenture"
        FirmStyle.BLOOMBERG -> "Bloomberg Professional"
    }

    val subtitlePractice = when (selectedFirm) {
        FirmStyle.MCKINSEY -> "Baku Practice | Corporate Strategy Group"
        FirmStyle.BCG -> "Eurasia Technology & Digital Strategy Division"
        FirmStyle.ACCENTURE -> "Baku Technology Excellence Center"
        FirmStyle.BLOOMBERG -> "Baku Intelligence Terminal | Operational Analytics"
    }

    val coreQuote = when (selectedFirm) {
        FirmStyle.MCKINSEY -> "“Unlocking structural efficiency through systematic generative intelligence.”"
        FirmStyle.BCG -> "“Shaping the future of digital ecosystems with algorithmic operational scale.”"
        FirmStyle.ACCENTURE -> "“Delivering high performance at the intersection of AI and Baku enterprise ingenuity.”"
        FirmStyle.BLOOMBERG -> "“Quantifying strategic alignment and tech equity yield curves inside Azerbaijani commercial vectors.”"
    }

    // Scaled-up sizes for boardroom TV/Projector presentation readability
    val heading1 = if (isPresentationMode) 38.sp else 28.sp
    val heading2 = if (isPresentationMode) 24.sp else 16.sp
    val bodyLarge = if (isPresentationMode) 22.sp else 14.sp
    val bodyMedium = if (isPresentationMode) 18.sp else 12.sp
    val bodySmall = if (isPresentationMode) 16.sp else 10.sp
    val labelLarge = if (isPresentationMode) 18.sp else 11.sp
    val labelSmall = if (isPresentationMode) 14.sp else 8.sp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(themeBg)
            .padding(if (isPresentationMode) 36.dp else 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(if (isPresentationMode) 40.dp else 28.dp)
        ) {
            // Elegant report meta-header (Close button, Style selector, Presentation Mode trigger)
            item {
                if (isPresentationMode) {
                    // Boardroom project header bar (minimal, beautiful, television safe)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.5.dp, themeAccent.copy(alpha = 0.4f)), RoundedCornerShape(16.dp))
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(themeAccent)
                            )
                            Text(
                                text = "BOARDROOM PRESENTATION // ${signatureName.uppercase(Locale.ROOT)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = themeAccent,
                                letterSpacing = 2.sp
                            )
                        }
                        
                        // Exit Presentation Trigger
                        Button(
                            onClick = { isPresentationMode = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.1f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("exit_presentation_mode_button"),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TvOff,
                                contentDescription = "Exit Presentation",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EXIT PRESENTATION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onExitReport,
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(16.dp),
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "EXIT REPORT MODE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Presentation Mode switch
                            Surface(
                                color = themeAccentMuted,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, themeAccent.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { isPresentationMode = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = "Presentation Mode",
                                        tint = themeAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "PRESENT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = themeAccent
                                    )
                                }
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0x11FFFFFF))
                            ) {
                                Row(modifier = Modifier.padding(2.dp)) {
                                    FirmStyle.values().forEach { firm ->
                                        val isSelected = selectedFirm == firm
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) themeAccentMuted else Color.Transparent)
                                                .clickable { selectedFirm = firm }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = when (firm) {
                                                    FirmStyle.MCKINSEY -> "McKinsey"
                                                    FirmStyle.BCG -> "BCG"
                                                    FirmStyle.ACCENTURE -> "Accenture"
                                                    FirmStyle.BLOOMBERG -> "Bloomberg"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isSelected) themeAccent else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // COVER / HEADER BLOCK
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = if (isPresentationMode) 20.dp else 12.dp)
                ) {
                    Text(
                        text = signatureName.uppercase(Locale.ROOT),
                        fontSize = if (isPresentationMode) 26.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = when (selectedFirm) {
                            FirmStyle.MCKINSEY -> FontFamily.Serif
                            FirmStyle.BLOOMBERG -> FontFamily.Monospace
                            else -> FontFamily.SansSerif
                        },
                        letterSpacing = if (selectedFirm == FirmStyle.BCG) 3.sp else 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitlePractice,
                        fontSize = if (isPresentationMode) 15.sp else 11.sp,
                        color = themeAccent,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    
                    Spacer(modifier = Modifier.height(if (isPresentationMode) 24.dp else 16.dp))
                    HorizontalDivider(color = themeDivider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(if (isPresentationMode) 24.dp else 16.dp))
                    
                    Text(
                        text = if (isPresentationMode) "BOARDROOM CONSULTING STRATEGY" else "ENTERPRISE ALIGNMENT REPORT",
                        fontSize = if (isPresentationMode) 16.sp else 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activeCompany.name.uppercase(Locale.ROOT),
                        fontSize = heading1,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = if (isPresentationMode) 46.sp else 34.sp,
                        fontFamily = when (selectedFirm) {
                            FirmStyle.MCKINSEY -> FontFamily.Serif
                            FirmStyle.BLOOMBERG -> FontFamily.Monospace
                            else -> FontFamily.SansSerif
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(if (isPresentationMode) 24.dp else 16.dp))
                    Text(
                        text = coreQuote,
                        fontSize = if (isPresentationMode) 18.sp else 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary,
                        style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.SansSerif
                    )
                    
                    Spacer(modifier = Modifier.height(if (isPresentationMode) 28.dp else 20.dp))
                    HorizontalDivider(color = themeDivider, thickness = 0.5.dp)
                }
            }

            // 1. Executive Summary
            item {
                ReportSectionContainer(
                    title = "01 // EXECUTIVE SUMMARY",
                    selectedFirm = selectedFirm,
                    themeAccent = themeAccent,
                    isPresentationMode = isPresentationMode
                ) {
                    Text(
                        text = "Through our rigorous digital diagnostics audit, we have evaluated the technological infrastructure and operating costs of ${activeCompany.name}. " +
                               "Currently built upon a legacy core utilizing ${activeCompany.currentStack}, the enterprise experiences strategic bottlenecks due to ${activeCompany.weakness.lowercase(Locale.ROOT)}. " +
                               "To neutralize this technical debt and capture market advantages in Baku, we recommend migrating immediately towards ${activeCompany.aiSolution}. " +
                               "This architectural shift is projected to trigger a massive structural cost reduction, scaling your overall operational speed and intelligence posture.",
                        fontSize = bodyLarge,
                        color = TextPrimary,
                        lineHeight = if (isPresentationMode) 34.sp else 24.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.SansSerif
                    )
                }
            }

            // 2. Competitive Gap (Competitive Analysis)
            item {
                ReportSectionContainer(
                    title = "02 // COMPETITIVE ANALYSIS",
                    selectedFirm = selectedFirm,
                    themeAccent = themeAccent,
                    isPresentationMode = isPresentationMode
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(if (isPresentationMode) 24.dp else 16.dp)) {
                        Text(
                            text = "A comparative posture assessment reveals the performance gap between ${activeCompany.name} and top-tier digital peers in Azerbaijan:",
                            fontSize = if (isPresentationMode) 18.sp else 13.sp,
                            color = TextSecondary,
                            lineHeight = if (isPresentationMode) 28.sp else 20.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.SansSerif
                        )
                        
                        val sectorLeader = "PASHA Bank OJSC"
                        val isLeader = activeCompany.id == "pasha_bank"
                        val leaderScore = 86
                        val gap = if (isLeader) 0 else (leaderScore - cisScore).coerceAtLeast(4)
                        
                        // Clean, minimal square bar comparing current AI readiness to benchmark
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeCompany.name,
                                    fontSize = bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.SansSerif
                                )
                                Text(
                                    text = "$cisScore% (Digital Maturity)",
                                    fontSize = bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = themeAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isPresentationMode) 10.dp else 6.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(cisScore.toFloat() / 100f)
                                        .fillMaxHeight()
                                        .background(themeAccent)
                                )
                            }
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$sectorLeader (Sector Benchmark)",
                                    fontSize = bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.SansSerif
                                )
                                Text(
                                    text = "$leaderScore%",
                                    fontSize = bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isPresentationMode) 10.dp else 6.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(leaderScore.toFloat() / 100f)
                                        .fillMaxHeight()
                                        .background(Color.White.copy(alpha = 0.4f))
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Redesigned Insight Callout box: Left border accent, 0.dp corners, plenty of whitespace
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.015f))
                                .drawBehind {
                                    val strokeWidth = 3.dp.toPx()
                                    drawLine(
                                        color = if (isLeader) themeAccent else themeAccent.copy(alpha = 0.5f),
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, size.height),
                                        strokeWidth = strokeWidth
                                    )
                                    drawLine(
                                        color = themeDivider.copy(alpha = 0.5f),
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, 0f),
                                        strokeWidth = 0.5.dp.toPx()
                                    )
                                    drawLine(
                                        color = themeDivider.copy(alpha = 0.5f),
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 0.5.dp.toPx()
                                    )
                                }
                                .padding(if (isPresentationMode) 24.dp else 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isLeader) Icons.Default.TrendingUp else Icons.Default.Warning,
                                    contentDescription = "Insights",
                                    tint = if (isLeader) themeAccent else themeAccent.copy(alpha = 0.7f),
                                    modifier = Modifier.size(if (isPresentationMode) 24.dp else 18.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = if (isLeader) {
                                        "Enterprise holds a dominant technological position, maintaining a 14% capability advantage over Azerbaijan competitors."
                                    } else {
                                        "A digital capability delta of $gap% is identified. Closing this strategic gap is imperative to protect market share from agile AI entrants."
                                    },
                                    fontSize = bodyMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = if (isPresentationMode) 26.sp else 18.sp,
                                    fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.SansSerif
                                )
                            }
                        }
                    }
                }
            }

            // 3. Strategic Recommendations (HIDDEN IN PRESENTATION MODE!)
            if (!isPresentationMode) {
                item {
                    ReportSectionContainer(
                        title = "03 // STRATEGIC RECOMMENDATIONS",
                        selectedFirm = selectedFirm,
                        themeAccent = themeAccent,
                        isPresentationMode = false
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            recommendations.forEachIndexed { idx, rec ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "STRATEGY 0${idx + 1} // ${rec.title.uppercase(Locale.ROOT)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.SansSerif,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = rec.expectedBenefit,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themeAccent,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = rec.detail,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.SansSerif
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        ReportBadge("PRIORITY", rec.priority, themeAccent, themeAccentMuted, selectedFirm)
                                        ReportBadge("IMPACT", rec.impact, themeAccent, themeAccentMuted, selectedFirm)
                                    }
                                    if (idx < recommendations.lastIndex) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(color = themeDivider.copy(alpha = 0.3f), thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Expected Opportunity (ROI)
            item {
                ReportSectionContainer(
                    title = if (isPresentationMode) "03 // FINANCIAL ROI OPPORTUNITY" else "04 // EXPECTED OPPORTUNITY",
                    selectedFirm = selectedFirm,
                    themeAccent = themeAccent,
                    isPresentationMode = isPresentationMode
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(if (isPresentationMode) 28.dp else 20.dp)) {
                        Text(
                            text = "Financial forecasting based on systemic cloud/AI consolidation over a 3-year adoption curve:",
                            fontSize = if (isPresentationMode) 18.sp else 13.sp,
                            color = TextSecondary,
                            lineHeight = if (isPresentationMode) 28.sp else 20.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.SansSerif
                        )
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            // Professional Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FINANCIAL METRIC",
                                    fontSize = if (isPresentationMode) 13.sp else 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeAccent.copy(alpha = 0.8f),
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "PROJECTED VALUE (₼ / AZN)",
                                    fontSize = if (isPresentationMode) 13.sp else 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeAccent.copy(alpha = 0.8f),
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            HorizontalDivider(color = themeAccent.copy(alpha = 0.4f), thickness = 1.dp)
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Professional Table Rows with high-contrast text and thin dividers
                            FinancialRow("Legacy OpEx Baseline", "₼${String.format("%,d", activeCompany.costBefore)} / mo", Color.White, isHeader = false, isPresentationMode = isPresentationMode, selectedFirm = selectedFirm)
                            HorizontalDivider(color = themeDivider.copy(alpha = 0.3f), thickness = 0.5.dp)
                            
                            FinancialRow("Target AI-Optimized OpEx", "₼${String.format("%,d", activeCompany.costAfterAi)} / mo", themeAccent, isHeader = false, isPresentationMode = isPresentationMode, selectedFirm = selectedFirm)
                            HorizontalDivider(color = themeDivider.copy(alpha = 0.3f), thickness = 0.5.dp)
                            
                            FinancialRow("Immediate Monthly Savings", "₼${String.format("%,d", potentialSavings)} / mo", themeAccent, isHeader = true, isPresentationMode = isPresentationMode, selectedFirm = selectedFirm)
                            HorizontalDivider(color = themeDivider.copy(alpha = 0.3f), thickness = 0.5.dp)
                            
                            FinancialRow("Projected Annualized Savings", "₼${String.format("%,d", potentialSavings * 12)} / yr", themeAccent, isHeader = true, isPresentationMode = isPresentationMode, selectedFirm = selectedFirm)
                            HorizontalDivider(color = themeAccent.copy(alpha = 0.3f), thickness = 1.dp)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (isPresentationMode) 24.dp else 16.dp)
                        ) {
                            // Redesigned Callout 1: Payback Period
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.White.copy(alpha = 0.015f))
                                    .drawBehind {
                                        val strokeWidth = 3.dp.toPx()
                                        drawLine(
                                            color = themeAccent,
                                            start = Offset(0f, 0f),
                                            end = Offset(0f, size.height),
                                            strokeWidth = strokeWidth
                                        )
                                        drawLine(
                                            color = themeDivider.copy(alpha = 0.4f),
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, 0f),
                                            strokeWidth = 0.5.dp.toPx()
                                        )
                                        drawLine(
                                            color = themeDivider.copy(alpha = 0.4f),
                                            start = Offset(0f, size.height),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = 0.5.dp.toPx()
                                        )
                                    }
                                    .padding(
                                        start = if (isPresentationMode) 24.dp else 16.dp,
                                        end = if (isPresentationMode) 16.dp else 12.dp,
                                        top = if (isPresentationMode) 16.dp else 12.dp,
                                        bottom = if (isPresentationMode) 16.dp else 12.dp
                                    )
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "PROJECTED PAYBACK PERIOD",
                                        fontSize = if (isPresentationMode) 13.sp else 8.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "3.5 Months",
                                        fontSize = if (isPresentationMode) 32.sp else 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.SansSerif
                                    )
                                    Text(
                                        text = "Immediate investment amortization",
                                        fontSize = if (isPresentationMode) 13.sp else 9.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            
                            // Redesigned Callout 2: 36-Month System ROI
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.White.copy(alpha = 0.015f))
                                    .drawBehind {
                                        val strokeWidth = 3.dp.toPx()
                                        drawLine(
                                            color = themeAccent,
                                            start = Offset(0f, 0f),
                                            end = Offset(0f, size.height),
                                            strokeWidth = strokeWidth
                                        )
                                        drawLine(
                                            color = themeDivider.copy(alpha = 0.4f),
                                            start = Offset(0f, 0f),
                                            end = Offset(size.width, 0f),
                                            strokeWidth = 0.5.dp.toPx()
                                        )
                                        drawLine(
                                            color = themeDivider.copy(alpha = 0.4f),
                                            start = Offset(0f, size.height),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = 0.5.dp.toPx()
                                        )
                                    }
                                    .padding(
                                        start = if (isPresentationMode) 24.dp else 16.dp,
                                        end = if (isPresentationMode) 16.dp else 12.dp,
                                        top = if (isPresentationMode) 16.dp else 12.dp,
                                        bottom = if (isPresentationMode) 16.dp else 12.dp
                                    )
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "36-MONTH FORECASTED ROI",
                                        fontSize = if (isPresentationMode) 13.sp else 8.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1fx ROI", (potentialSavings.toFloat() / (activeCompany.costAfterAi.coerceAtLeast(1000))) * 3.8f),
                                        fontSize = if (isPresentationMode) 32.sp else 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = themeAccent,
                                        fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.SansSerif
                                    )
                                    Text(
                                        text = "Significant margin enhancement",
                                        fontSize = if (isPresentationMode) 13.sp else 9.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Action Plan
            item {
                ReportSectionContainer(
                    title = if (isPresentationMode) "04 // 90-DAY ACTION PLAN" else "05 // ACTION PLAN",
                    selectedFirm = selectedFirm,
                    themeAccent = themeAccent,
                    isPresentationMode = isPresentationMode
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(if (isPresentationMode) 24.dp else 20.dp)) {
                        Text(
                            text = "A phased 90-day implementation plan to transition ${activeCompany.name} from legacy state to digital pioneer:",
                            fontSize = if (isPresentationMode) 18.sp else 13.sp,
                            color = TextSecondary,
                            lineHeight = if (isPresentationMode) 28.sp else 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Column(
                            modifier = Modifier.padding(start = if (isPresentationMode) 12.dp else 8.dp),
                            verticalArrangement = Arrangement.spacedBy(if (isPresentationMode) 32.dp else 24.dp)
                        ) {
                            ReportTimelineItem(
                                phase = "PHASE 1 (DAYS 1-30)",
                                title = "Architectural Foundation & Sandbox Deployment",
                                description = "Deprecate legacy monolithic dependencies. Establish cloud orchestration gateways, securely deploy sandbox LLM models, and initiate in-house technical team training.",
                                selectedFirm = selectedFirm,
                                themeAccent = themeAccent,
                                isPresentationMode = isPresentationMode
                            )
                            ReportTimelineItem(
                                phase = "PHASE 2 (DAYS 31-60)",
                                title = "Production Integration & API Interfacing",
                                description = "Integrate custom AI engines with database endpoints. Spin up localized pipeline triggers and deploy automated client support agents to test channels.",
                                selectedFirm = selectedFirm,
                                themeAccent = themeAccent,
                                isPresentationMode = isPresentationMode
                            )
                            ReportTimelineItem(
                                phase = "PHASE 3 (DAYS 61-90)",
                                title = "Scale, Calibration & Predictive Optimization",
                                description = "Conduct high-capacity latency and load tests. Roll out the full system in live channels, optimize pipeline query caching, and activate continuous model calibration.",
                                selectedFirm = selectedFirm,
                                themeAccent = themeAccent,
                                isPresentationMode = isPresentationMode
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportSectionContainer(
    title: String,
    selectedFirm: FirmStyle,
    themeAccent: Color,
    isPresentationMode: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isPresentationMode) 12.dp else 4.dp),
        verticalArrangement = Arrangement.spacedBy(if (isPresentationMode) 24.dp else 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = if (isPresentationMode) 18.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                color = themeAccent,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(0.5.dp)
                    .background(themeAccent.copy(alpha = 0.25f))
            )
        }
        
        // Redesigned: Zero rounded corners, sleek bottom borders, or clean solid sheet lines
        // depending on selected firm. Feels printable and bespoke.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Crisp McKinsey slate divider lines
                    drawLine(
                        color = themeAccent.copy(alpha = 0.08f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
                .padding(
                    top = if (isPresentationMode) 20.dp else 12.dp,
                    bottom = if (isPresentationMode) 20.dp else 12.dp,
                    start = if (isPresentationMode) 8.dp else 4.dp,
                    end = if (isPresentationMode) 8.dp else 4.dp
                )
        ) {
            content()
        }
    }
}

@Composable
fun ReportBadge(
    label: String,
    value: String,
    accentColor: Color,
    accentMuted: Color,
    selectedFirm: FirmStyle = FirmStyle.MCKINSEY
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.02f))
            .border(
                BorderStroke(0.5.dp, accentColor.copy(alpha = 0.35f)), 
                shape = if (selectedFirm == FirmStyle.MCKINSEY) RoundedCornerShape(0.dp) else RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontSize = 9.sp,
            color = accentColor,
            fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun FinancialRow(
    label: String,
    value: String,
    valueColor: Color,
    isHeader: Boolean,
    isPresentationMode: Boolean = false,
    selectedFirm: FirmStyle = FirmStyle.MCKINSEY
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isPresentationMode) 14.dp else 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isPresentationMode) {
                if (isHeader) 20.sp else 18.sp
            } else {
                if (isHeader) 13.sp else 12.sp
            },
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Medium,
            color = if (isHeader) Color.White else TextSecondary,
            fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.SansSerif
        )
        Text(
            text = value,
            fontSize = if (isPresentationMode) {
                if (isHeader) 22.sp else 20.sp
            } else {
                if (isHeader) 14.sp else 12.sp
            },
            fontWeight = FontWeight.Bold,
            color = valueColor,
            fontFamily = if (selectedFirm == FirmStyle.BLOOMBERG) FontFamily.Monospace else FontFamily.SansSerif
        )
    }
}

@Composable
fun ReportTimelineItem(
    phase: String,
    title: String,
    description: String,
    selectedFirm: FirmStyle,
    themeAccent: Color,
    isPresentationMode: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (isPresentationMode) 24.dp else 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(if (isPresentationMode) 30.dp else 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isPresentationMode) 18.dp else 12.dp)
                    .clip(CircleShape)
                    .background(themeAccent)
            )
            Box(
                modifier = Modifier
                    .width(if (isPresentationMode) 2.dp else 1.dp)
                    .height(if (isPresentationMode) 120.dp else 90.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                themeAccent,
                                Color.White.copy(alpha = 0.08f)
                            )
                        )
                    )
            )
        }
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = phase,
                fontSize = if (isPresentationMode) 15.sp else 10.sp,
                color = themeAccent,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = title,
                fontSize = if (isPresentationMode) 20.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.SansSerif
            )
            Text(
                text = description,
                fontSize = if (isPresentationMode) 18.sp else 12.sp,
                color = TextSecondary,
                lineHeight = if (isPresentationMode) 26.sp else 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = if (selectedFirm == FirmStyle.MCKINSEY) FontFamily.Serif else FontFamily.SansSerif
            )
        }
    }
}

@Composable
fun TechnologyEvolutionTimelineSection(companyA: Company, scoreA: Int) {
    var selectedStepIndex by remember { mutableStateOf(0) }
    
    val steps = listOf(
        TimelineStep(
            label = "Current State",
            title = "COGNITIVE BASELINE",
            scoreTarget = scoreA,
            focus = "Foundational Telemetry & Redundancy Mitigation",
            improvement = "Establishing exact API telemetry loggers, identifying legacy technical debt, and securing operational boundaries."
        ),
        TimelineStep(
            label = "6 Months",
            title = "ALGORITHMIC DEPLOYMENT",
            scoreTarget = (scoreA + 12).coerceAtMost(95),
            focus = "High-Entropy Compression & Cost-Aware Routing",
            improvement = "Realizing up to 70% reduction in Gemini API token cost overruns through loss-less compression pipelines."
        ),
        TimelineStep(
            label = "1 Year",
            title = "AGENTIC WORKFLOWS",
            scoreTarget = (scoreA + 25).coerceAtMost(98),
            focus = "Multi-Agent Platforms & Transactional Automation",
            improvement = "Eliminating administrative friction and raising team throughput by automating routine code/reporting tasks."
        ),
        TimelineStep(
            label = "3 Years",
            title = "COGNITIVE SYSTEM",
            scoreTarget = (scoreA + 40).coerceAtMost(100),
            focus = "Autonomous Self-Learning Enterprise Architectures",
            improvement = "Achieving peak operational efficiency, self-healing cognitive systems, and full localized data compliance."
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 12.dp)
            .testTag("tech_timeline_card"),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(32.dp)
        ) {
            // Header
            Text(
                text = "TECHNOLOGY EVOLUTION ROADMAP",
                fontSize = 10.sp,
                color = BlueAccent,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Technology Evolution Timeline",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "A strategic roadmap projecting technological maturity improvements and automated cost containment metrics over a 36-month horizon.",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Horizontal Track with circles
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                val animatedProgressWidth by animateFloatAsState(
                    targetValue = selectedStepIndex / 3f,
                    animationSpec = tween(350, easing = FastOutSlowInEasing),
                    label = "timeline_progress_bar"
                )
                
                val localBlueAccent = BlueAccent
                val localEmeraldAccent = EmeraldAccent
                
                // Background connection line and active line drawn via Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .align(Alignment.TopCenter)
                ) {
                    val y = 12.dp.toPx()
                    val x0 = 0.5f / 4f * size.width
                    val x3 = 3.5f / 4f * size.width
                    
                    // Draw inactive connection track
                    drawLine(
                        color = Color(0x18FFFFFF),
                        start = Offset(x0, y),
                        end = Offset(x3, y),
                        strokeWidth = 2.dp.toPx()
                    )
                    
                    // Draw active connection track with animated progress
                    val activeEnd = x0 + (animatedProgressWidth * (x3 - x0))
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(localBlueAccent, localEmeraldAccent)
                        ),
                        start = Offset(x0, y),
                        end = Offset(activeEnd, y),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                
                // Nodes and labels row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    steps.forEachIndexed { index, step ->
                        val isSelected = selectedStepIndex == index
                        val isCompleted = index <= selectedStepIndex
                        
                        val circleSize by animateDpAsState(
                            targetValue = if (isSelected) 18.dp else 12.dp,
                            animationSpec = tween(300),
                            label = "circle_size"
                        )
                        
                        val circleColor by animateColorAsState(
                            targetValue = when {
                                isSelected -> BlueAccent
                                isCompleted -> EmeraldAccent
                                else -> Color(0x3DFFFFFF)
                            },
                            animationSpec = tween(300),
                            label = "circle_color"
                        )
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedStepIndex = index
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(circleSize)
                                        .clip(CircleShape)
                                        .background(circleColor)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = step.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Detail box with AnimatedContent
            AnimatedContent(
                targetState = selectedStepIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    } using SizeTransform(clip = false)
                },
                label = "timeline_detail_animation"
            ) { targetIndex ->
                val currentStep = steps[targetIndex]
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(12.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentStep.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueAccent,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        
                        Surface(
                            color = EmeraldAccent.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f)),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "Maturity Target: ${currentStep.scoreTarget}/100",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = EmeraldAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    // Progress Indicator Bar
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "EXPECTED MATURITY INDEX",
                                fontSize = 8.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        val animatedScoreProgress by animateFloatAsState(
                            targetValue = currentStep.scoreTarget / 100f,
                            animationSpec = tween(350, easing = FastOutSlowInEasing),
                            label = "timeline_score_bar"
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0x0CFFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedScoreProgress)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(BlueAccent, EmeraldAccent)
                                        )
                                    )
                            )
                        }
                    }
                    
                    // Strategic Focus Point
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "PRIMARY TECHNOLOGY FOCUS",
                            fontSize = 8.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentStep.focus,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    // Expected Improvements
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "EXPECTED MATURITY IMPROVEMENTS",
                            fontSize = 8.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentStep.improvement,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

data class TimelineStep(
    val label: String,
    val title: String,
    val scoreTarget: Int,
    val focus: String,
    val improvement: String
)
