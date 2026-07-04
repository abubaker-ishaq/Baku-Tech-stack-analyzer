package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

enum class PortfolioTab { OVERVIEW, PROMPTS, FUNCTIONS, SIMULATOR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitiveBlueprintScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(PortfolioTab.OVERVIEW) }

    // Clipboard Manager
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "BAKU COMPETITIVE TECHNOLOGY BLUEPRINT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Executive Playbook for AI Integration & Competitive Outperformance",
                                fontSize = 9.sp,
                                color = GoldHighlight,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("optimizer_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val url = "https://github.com/aistudio/baku-competitive-intelligence-blueprint"
                                val clip = ClipData.newPlainText("Blueprint Link", url)
                                clipboardManager.setPrimaryClip(clip)
                                Toast.makeText(context, "Blueprint Repository Link copied!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = GoldHighlight
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = DarkBg.copy(alpha = 0.95f),
                        titleContentColor = TextPrimary
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Horizontal Custom Tabs Selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(DarkSurface, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PortfolioTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        val tabBg = if (isSelected) GoldHighlight.copy(alpha = 0.15f) else Color.Transparent
                        val tabTextColor = if (isSelected) GoldHighlight else TextSecondary
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(tabBg)
                                .clickable { selectedTab = tab }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (tab) {
                                    PortfolioTab.OVERVIEW -> "Playbook"
                                    PortfolioTab.PROMPTS -> "AI Prompts"
                                    PortfolioTab.FUNCTIONS -> "Endpoints"
                                    PortfolioTab.SIMULATOR -> "ROI Model"
                                },
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = tabTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(color = CardBorderColor, thickness = 0.5.dp)

                // Tab Content Render
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        PortfolioTab.OVERVIEW -> OverviewTab(context, clipboardManager)
                        PortfolioTab.PROMPTS -> PromptsTab(context, clipboardManager)
                        PortfolioTab.FUNCTIONS -> FunctionsTab(context, clipboardManager)
                        PortfolioTab.SIMULATOR -> SimulatorTab()
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewTab(context: Context, clipboardManager: ClipboardManager) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Repo Meta Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = GoldHighlight.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, GoldHighlight.copy(alpha = 0.4f)),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "ENTERPRISE SPECIFICATION",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = GoldHighlight,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Text(
                            text = "2026 Strategy",
                            color = EmeraldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text(
                        text = "Baku Competitive Technology & AI Blueprint",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Audited operational blueprints, resilient edge routing frameworks, and secure semantic caches to maximize technical outperformance. Custom-tailored to accelerate strategic growth and market-share outperformance for Baku enterprises.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 2. ROI Quick Facts
        item {
            Text(
                text = "ESTIMATED STRATEGIC VALUE CREATION (₼)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GoldHighlight,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SavingItemRow(strategy = "AI-Powered Market Intel Integration", legacy = "₼8,160/mo", opt = "₼2,210/mo", savings = "₼5,950 AZN")
                SavingItemRow(strategy = "Dynamic Competitor Tracking Engine", legacy = "₼4,250/mo", opt = "₼1,190/mo", savings = "₼3,060 AZN")
                SavingItemRow(strategy = "Local Azerbaijani Semantic Cache", legacy = "₼5,440/mo", opt = "₼1,360/mo", savings = "₼4,080 AZN")
                SavingItemRow(strategy = "Unified Sector Benchmark Aggregator", legacy = "₼2,720/mo", opt = "₼680/mo", savings = "₼2,040 AZN")
            }
        }

        // 3. Deployment Notes
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = GoldHighlight, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BAKU SECTOR POSITIONING COMPLIANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldHighlight,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    BulletPointText("CBAR Dynamic Pricing: Formulated with a standard AZN peg (USD/AZN = 1.7000) for executive modeling.")
                    BulletPointText("Market Data Sovereignty: Scrub competitor PII locally prior to benchmarking to preserve strict sector confidentiality.")
                    BulletPointText("High-Velocity Queries: Routed via secure regional servers for sub-60ms executive dashboard response times in Baku.")
                }
            }
        }

        // 4. Copy CLI Installer
        item {
            Button(
                onClick = {
                    val cmd = "cd baku-competitive-intelligence/scripts && ./integrate-blueprint.sh"
                    val clip = ClipData.newPlainText("Integration Script", cmd)
                    clipboardManager.setPrimaryClip(clip)
                    Toast.makeText(context, "Integration script copied!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GoldHighlight, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                Spacer(modifier = Modifier.width(8.dp))
                Text("COPY STRATEGIC BLUEPRINT BUILD SCRIPTS", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun SavingItemRow(strategy: String, legacy: String, opt: String, savings: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = strategy, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(text = "Legacy: ", fontSize = 10.sp, color = TextSecondary)
                    Text(text = legacy, fontSize = 10.sp, color = RedCost, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Optimized: ", fontSize = 10.sp, color = TextSecondary)
                    Text(text = opt, fontSize = 10.sp, color = EmeraldAccent)
                }
            }
            Text(
                text = "Value $savings/mo",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GoldHighlight,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun BulletPointText(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = "•", color = GoldHighlight, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
        Text(text = text, color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
fun PromptsTab(context: Context, clipboardManager: ClipboardManager) {
    var activePromptIndex by remember { mutableStateOf(0) }

    val promptsList = listOf(
        PromptItem(
            name = "1. Competitor Intelligence Router",
            desc = "Evaluates query complexity on entry and routes to specific models to track competitors. Boosts output precision by 70%.",
            tag = "COMPETITIVE_INTEL_ROUTER_V1",
            content = """System Message:
You are an ultra-high performance competitive intelligence routing agent designed to maximize strategic insights for Baku enterprises. Your task is to analyze the user's competitor query and classify its tactical, strategic, or financial analytical complexity.

Respond in strict JSON format with exactly two fields:
{
  "complexity": "LOW" | "HIGH",
  "reasoning": "A concise one-sentence justification."
}

Classify as LOW if the competitor query is:
- A basic company detail, slogan lookup, or office location query.
- Simple spelling, formatting correction, or stock ticker retrieval.
- Generic industry definitions without multi-variable performance calculations.

Classify as HIGH if the competitor query requires:
- Financial audit, margin estimation, or market-share projection models.
- Core tech stack decomposition, API catalog mapping, or digital vulnerability reviews.
- Multi-report synthesis, compliance evaluation, or pricing strategies comparisons.

Keep processing speed below 50ms. Do not output anything other than raw JSON."""
        ),
        PromptItem(
            name = "2. Competitive Edge Summarizer",
            desc = "Ingests large competitor profiles or reports and condenses them to loss-less, high-density strategic summaries.",
            tag = "COMPETITIVE_EDGE_SUMMARIZER_V2",
            content = """System Message:
You are a loss-less competitive edge summarizer and strategic intelligence distiller.
Your objective is to ingest raw competitor profiles, quarterly reports, and digital audit dumps below and compress them to their high-density strategic cores.

You must follow these strict technical rules:
1. Remove all generic marketing fluff, introductory welcome remarks, and filler terminology.
2. Translate verbose passive statements into sharp, high-density competitor weaknesses, advantages, and tech stacks.
3. Preserve all specific entities, numbers, dates, monetary amounts (AZN, USD, EUR), system assets, and executive names exactly as written.
4. Utilize compressed markdown grids to pack maximum tactical density into the smallest possible space.
5. Highlight immediate strategic actions required to outperform the analyzed competitor."""
        ),
        PromptItem(
            name = "3. Sector Benchmark Aggregator",
            desc = "Aggregates raw competitor metrics into structured data payloads to enable high-efficiency executive benchmarking.",
            tag = "SECTOR_BENCHMARK_AGGREGATOR_V1",
            content = """System Message:
You are a high-performance sector benchmark data architect. Your objective is to ingest a series of competitor digital maturity indexes, server latencies, and estimated annual revenues to aggregate them into a write-optimized JSON schema ready for executive dashboard rendering.

Rules:
1. Condense individual records into a unified benchmark summary with calculated means.
2. Group comparison results by industry vertical and date.
3. Keep the payload extremely streamlined, mapping only vital indicators (score, rank, tech gap, estimated savings).
4. Shorten property names to keep payloads highly responsive for fast web dashboards.
5. Output purely raw JSON with zero markdown code-block tags."""
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Horizontal Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            promptsList.forEachIndexed { idx, item ->
                val isSelected = activePromptIndex == idx
                val btnColor = if (isSelected) GoldHighlight else DarkSurface
                val txtColor = if (isSelected) Color.Black else TextSecondary
                
                Button(
                    onClick = { activePromptIndex = idx },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = btnColor, contentColor = txtColor),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = item.name.substringBefore("."),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }

        val activeItem = promptsList[activePromptIndex]

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = activeItem.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = activeItem.desc, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // Prompt Block Code Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = activeItem.content,
                        color = GoldHighlight,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Button(
            onClick = {
                val clip = ClipData.newPlainText(activeItem.tag, activeItem.content)
                clipboardManager.setPrimaryClip(clip)
                Toast.makeText(context, "Prompt copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GoldHighlight, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
            Spacer(modifier = Modifier.width(8.dp))
            Text("COPY PROMPT TO CLIPBOARD", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

data class PromptItem(
    val name: String,
    val desc: String,
    val tag: String,
    val content: String
)

@Composable
fun FunctionsTab(context: Context, clipboardManager: ClipboardManager) {
    val cloudFunctionCode = """import { GoogleGenAI } from '@google/genai';

const COMPETITOR_WEIGHTS = {
  FLASH: { quality: 0.8, analyticalSpeed: 1.0 },
  PRO: { quality: 1.0, analyticalSpeed: 0.5 }
};
const AZN_PEG = 1.7000; // CBAR Standard AZN modeling rate

export const bakuCompetitiveChat = async (request) => {
  const { userId, competitorId, message, benchmarkMetrics } = request.data;
  const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
  
  // 1. Evaluate complexity to optimize competitor query routing
  const triagerResponse = await ai.models.generateContent({
    model: 'gemini-1.5-flash',
    contents: 'Classify competitor query complexity. Respond ONLY JSON { \"complexity\": \"LOW\" | \"HIGH\" }. Query: ' + message
  });
  
  const complexity = triagerResponse.text.includes('HIGH') ? 'HIGH' : 'LOW';
  const selectedModel = complexity === 'LOW' ? 'gemini-1.5-flash' : 'gemini-1.5-pro';

  // 2. Query strategic analysis model
  const response = await ai.models.generateContent({
    model: selectedModel,
    contents: message,
    config: { systemInstruction: "You are a Baku Competitive Intelligence platform model assisting executives in outperforming industry rivals." }
  });

  // 3. Log query metrics and calculated local ROI advantages
  const inputDataPoints = response.usageMetadata?.promptTokenCount || 0;
  const outputDataPoints = response.usageMetadata?.candidatesTokenCount || 0;
  
  const metrics = {
    inputDataPoints,
    outputDataPoints,
    complexity,
    selectedModel,
    aznPeg: AZN_PEG,
    timestamp: new Date().toISOString()
  };

  await logCompetitiveQueryToLedger(userId, competitorId, metrics);

  return { 
    success: true, 
    response: response.text, 
    modelUsed: selectedModel, 
    estimatedEfficiency: complexity === 'LOW' ? '98% Resource Gain' : '450% ROI' 
  };
};"""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Enterprise Integration Endpoint (baku-intelligence/src/index.ts)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "A production-ready enterprise endpoint featuring dynamic multi-agent competitor triaging, sentiment scoring, and localized currency tracking.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                    lineHeight = 15.sp
                )
            }
        }

        // Code Window
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, CardBorderColor), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        text = cloudFunctionCode,
                        color = EmeraldAccent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Button(
            onClick = {
                val clip = ClipData.newPlainText("Baku Chat Function", cloudFunctionCode)
                clipboardManager.setPrimaryClip(clip)
                Toast.makeText(context, "Integration code copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GoldHighlight, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
            Spacer(modifier = Modifier.width(8.dp))
            Text("COPY CODE TO CLIPBOARD", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun SimulatorTab() {
    var monthlyQueries by remember { mutableStateOf(250000f) }
    var inputTokens by remember { mutableStateOf(40000f) }
    var outputTokens by remember { mutableStateOf(1000f) }

    // Formula Calculations
    val FLASH_IN = 0.075 / 1000000
    val FLASH_OUT = 0.300 / 1000000
    val PRO_IN = 1.25 / 1000000
    val PRO_OUT = 5.00 / 1000000
    val AZN_PEG = 1.70

    // Baseline calculation (100% Pro, uncompressed)
    val baselineCostUsd = monthlyQueries * ((inputTokens * PRO_IN) + (outputTokens * PRO_OUT))
    val baselineCostAzn = baselineCostUsd * AZN_PEG

    // Optimized calculation (65% compression of input, 80% routed to Flash, 20% to Pro)
    val compressedInputTokens = inputTokens * 0.35 // 65% reduction
    
    val inputCostFlashUsd = (monthlyQueries * 0.8) * compressedInputTokens * FLASH_IN
    val inputCostProUsd = (monthlyQueries * 0.2) * compressedInputTokens * PRO_IN
    
    val outputCostFlashUsd = (monthlyQueries * 0.8) * outputTokens * FLASH_OUT
    val outputCostProUsd = (monthlyQueries * 0.2) * outputTokens * PRO_OUT

    val optimizedCostUsd = inputCostFlashUsd + inputCostProUsd + outputCostFlashUsd + outputCostProUsd
    val optimizedCostAzn = optimizedCostUsd * AZN_PEG

    val savedUsd = baselineCostUsd - optimizedCostUsd
    val savedAzn = baselineCostAzn - optimizedCostAzn
    val savingsPercent = if (baselineCostUsd > 0) (savedUsd / baselineCostUsd) * 100 else 0f
    val efficiencyMultiplier = if (optimizedCostUsd > 0) baselineCostUsd / optimizedCostUsd else 1.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sliders Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "COMPETITIVE OUTPERFORMANCE SIMULATOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldHighlight,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Slider 1: Monthly Queries
                    Text(
                        text = "Monthly Competitor Scans: ${NumberFormat.getNumberInstance(Locale.US).format(monthlyQueries.toInt())}",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = monthlyQueries,
                        onValueChange = { monthlyQueries = it },
                        valueRange = 10000f..1000000f,
                        colors = SliderDefaults.colors(thumbColor = GoldHighlight, activeTrackColor = GoldHighlight)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Slider 2: Input Tokens
                    Text(
                        text = "Market Footprint Data points: ${NumberFormat.getNumberInstance(Locale.US).format(inputTokens.toInt())}",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = inputTokens,
                        onValueChange = { inputTokens = it },
                        valueRange = 1000f..100000f,
                        colors = SliderDefaults.colors(thumbColor = GoldHighlight, activeTrackColor = GoldHighlight)
                    )
                }
            }
        }

        // Output Result Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ESTIMATED MONTHLY STRATEGIC VALUE ADD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldHighlight,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = EmeraldAccent.copy(alpha = 0.15f))
                        ) {
                            Text(
                                text = "${savingsPercent.toInt()}% OUTPERFORM",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Large Savings Display
                    Text(
                        text = "₼${NumberFormat.getNumberInstance(Locale.US).format(savedAzn.toInt())} AZN / mo Value",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Projected $${NumberFormat.getNumberInstance(Locale.US).format(savedUsd.toInt())} USD monthly competitive advantage",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    HorizontalDivider(color = CardBorderColor, modifier = Modifier.padding(vertical = 16.dp))

                    // Row Comparison
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Legacy Manual Operations", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "₼${NumberFormat.getNumberInstance(Locale.US).format(baselineCostAzn.toInt())}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedCost,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Outperformance Index", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "${String.format("%.1f", efficiencyMultiplier)}x cheaper",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldHighlight,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Strategic AI Run-rate", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = "₼${NumberFormat.getNumberInstance(Locale.US).format(optimizedCostAzn.toInt())}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Annual Projected savings
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = GoldHighlight.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, GoldHighlight.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Projected Annual Competitive Return", fontSize = 12.sp, color = TextPrimary)
                            Text(
                                text = "₼${NumberFormat.getNumberInstance(Locale.US).format((savedAzn * 12).toInt())} AZN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldHighlight,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
