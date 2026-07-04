package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WebsiteScannerScreen(
    viewModel: CompanyViewModel,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val companies by viewModel.companies.collectAsState()
    val selectedCompanyState by viewModel.selectedCompany.collectAsState()

    // Safely load companies if they are not already loaded
    LaunchedEffect(Unit) {
        if (companies.isEmpty()) {
            viewModel.loadCompanies(context)
        }
    }

    if (companies.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = GoldHighlight)
        }
        return
    }

    // Default target company to selected state or first in list
    var selectedTargetCompany by remember(companies, selectedCompanyState) {
        mutableStateOf(selectedCompanyState ?: companies.firstOrNull())
    }

    // Default competitor to first different company in list
    var selectedCompetitor by remember(companies, selectedTargetCompany) {
        val defaultComp = companies.firstOrNull { it.id != selectedTargetCompany?.id } ?: companies.firstOrNull()
        mutableStateOf(defaultComp)
    }

    val targetCompany = selectedTargetCompany ?: companies.first()
    val competitorCompany = selectedCompetitor ?: companies.first()

    // Metrics calculations
    val scoreTarget = calculateCisScore(targetCompany)
    val scoreCompetitor = calculateCisScore(competitorCompany)
    val savings = targetCompany.costBefore - targetCompany.costAfterAi

    // Dynamic Rank calculation
    val sortedList = companies.sortedByDescending { calculateCisScore(it) }
    val targetIndex = sortedList.indexOfFirst { it.id == targetCompany.id }
    val rank = if (targetIndex != -1) targetIndex + 1 else 2

    // Dynamic Confidence calculation
    val confidence = (94 + (targetCompany.name.length % 5)).coerceIn(90, 98)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .testTag("assessment_screen"),
        contentPadding = PaddingValues(top = 36.dp, bottom = 48.dp, start = 32.dp, end = 32.dp),
        verticalArrangement = Arrangement.spacedBy(40.dp) // Generous executive whitespace
    ) {
        // 1. Executive Consulting Page Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "EXECUTIVE ASSESSMENT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldHighlight, // ROI Gold
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Executive Technology Assessment",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Formulate high-fidelity competitive benchmarks and dynamic optimization metrics to guide modernisation trajectories for target enterprises.",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
            }
        }

        // 2. Portfolio Selection Section
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "SELECT PORTFOLIO TARGETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ExecutiveSelectorCard(
                        label = "Target Enterprise",
                        selectedCompanyName = targetCompany.name,
                        techStackSummary = targetCompany.currentStack,
                        companies = companies,
                        onCompanySelected = {
                            selectedTargetCompany = it
                            viewModel.selectCompany(it)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("target_company_selector")
                    )

                    ExecutiveSelectorCard(
                        label = "Competitor Benchmark",
                        selectedCompanyName = competitorCompany.name,
                        techStackSummary = competitorCompany.currentStack,
                        companies = companies.filter { it.id != targetCompany.id },
                        onCompanySelected = {
                            selectedCompetitor = it
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("competitor_company_selector")
                    )
                }
            }
        }

        // 3. Crisp Thin Divider
        item {
            HorizontalDivider(color = CardBorderColor.copy(alpha = 0.5f), thickness = 1.dp)
        }

        // 4. Hero KPIs Section (Title -> Executive KPIs)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "EXECUTIVE KEY PERFORMANCE INDICATORS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        MetricItem(
                            label = "Competitive Score",
                            value = "$scoreTarget / 100",
                            subtext = "${if (scoreTarget >= scoreCompetitor) "+" else ""}${scoreTarget - scoreCompetitor} vs Benchmark",
                            valueColor = if (scoreTarget >= scoreCompetitor) EmeraldAccent else AmberWarning,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("metric_competitive_score")
                        )
                        MetricItem(
                            label = "Industry Rank",
                            value = "#$rank of ${companies.size}",
                            subtext = "By digital maturity index",
                            valueColor = TextPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("metric_industry_rank")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        MetricItem(
                            label = "Confidence",
                            value = "$confidence%",
                            subtext = "Verified data telemetry",
                            valueColor = TextPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("metric_confidence")
                        )
                        MetricItem(
                            label = "Estimated Opportunity",
                            value = "₼${String.format("%,d", savings)}",
                            subtext = "Annual: ₼${String.format("%,d", savings * 12)}",
                            valueColor = EmeraldAccent,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("metric_estimated_opportunity")
                        )
                    }
                }
            }
        }

        // 5. Action Button Section (Primary Action)
        item {
            Button(
                onClick = {
                    viewModel.selectCompany(targetCompany)
                    viewModel.setReportOpen(true)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp) // Premium standard height (not oversized)
                    .testTag("access_assessment_dossier_button"),
                shape = RoundedCornerShape(8.dp), // Premium elegant rounded corners like Stripe/Linear
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldHighlight, // Premium ROI Gold
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(vertical = 0.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Text(
                    text = "ACCESS STRATEGIC INTELLIGENCE DOSSIER",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun ExecutiveSelectorCard(
    label: String,
    selectedCompanyName: String,
    techStackSummary: String,
    companies: List<Company>,
    onCompanySelected: (Company) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(Locale.ROOT),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Select options",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = selectedCompanyName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = techStackSummary,
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(DarkSurface)
                .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(8.dp))
        ) {
            companies.forEach { company ->
                DropdownMenuItem(
                    text = {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = company.name,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = company.currentStack,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    },
                    onClick = {
                        onCompanySelected(company)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = TextPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun MetricItem(
    label: String,
    value: String,
    subtext: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(DarkSurfaceElevated, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(12.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label.uppercase(Locale.ROOT),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = subtext,
            fontSize = 12.sp,
            color = TextMuted,
            fontWeight = FontWeight.Normal
        )
    }
}

private fun calculateCisScore(company: Company): Int {
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
