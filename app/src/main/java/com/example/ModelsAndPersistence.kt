package com.example

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random

// Persistence helper for Scanned Companies, Favorites, and Premium Status
object BakuPersistence {
    private const val PREFS_NAME = "baku_analyzer_premium_prefs"
    private const val KEY_PREMIUM = "is_premium"
    private const val KEY_FAVORITES = "favorite_company_ids"
    private const val KEY_SCANNED = "scanned_companies_json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // 1. Premium Status
    fun isPremium(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PREMIUM, true)
    }

    fun setPremium(context: Context, premium: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PREMIUM, premium).apply()
    }

    // 2. Favorites
    fun getFavorites(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    fun toggleFavorite(context: Context, companyId: String): Set<String> {
        val current = getFavorites(context).toMutableSet()
        if (current.contains(companyId)) {
            current.remove(companyId)
        } else {
            current.add(companyId)
        }
        getPrefs(context).edit().putStringSet(KEY_FAVORITES, current).apply()
        return current
    }

    // 3. Scanned Companies
    fun getScannedCompanies(context: Context): List<Company> {
        val jsonStr = getPrefs(context).getString(KEY_SCANNED, null) ?: return emptyList()
        val list = mutableListOf<Company>()
        try {
            val jsonArray = JSONArray(jsonStr)
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveScannedCompany(context: Context, company: Company) {
        val current = getScannedCompanies(context).toMutableList()
        // Remove existing if duplicate ID
        current.removeAll { it.id == company.id }
        // Add to front of history
        current.add(0, company)
        
        try {
            val jsonArray = JSONArray()
            for (c in current) {
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("current_stack", c.currentStack)
                    put("weakness", c.weakness)
                    put("cost_before", c.costBefore)
                    put("ai_solution", c.aiSolution)
                    put("cost_after_ai", c.costAfterAi)
                    put("saving_percent", c.savingPercent)
                    put("hiring_llm", c.hiringLlm)
                    put("llm_title", c.llmTitle)
                    put("llm_salary", c.llmSalary)
                    put("career_link", c.careerLink)
                    
                    val compArr = JSONArray()
                    for (comp in c.comparison) {
                        compArr.put(JSONObject().apply {
                            put("metric", comp.metric)
                            put("before", comp.before)
                            put("after", comp.after)
                        })
                    }
                    put("comparison", compArr)
                    
                    val planArr = JSONArray()
                    for (p in c.migrationPlan) {
                        planArr.put(JSONObject().apply {
                            put("days", p.days)
                            put("title", p.title)
                            put("detail", p.detail)
                        })
                    }
                    put("migration_plan", planArr)
                }
                jsonArray.put(obj)
            }
            getPrefs(context).edit().putString(KEY_SCANNED, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteScannedCompany(context: Context, companyId: String) {
        val current = getScannedCompanies(context).filter { it.id != companyId }
        try {
            val jsonArray = JSONArray()
            for (c in current) {
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("current_stack", c.currentStack)
                    put("weakness", c.weakness)
                    put("cost_before", c.costBefore)
                    put("ai_solution", c.aiSolution)
                    put("cost_after_ai", c.costAfterAi)
                    put("saving_percent", c.savingPercent)
                    put("hiring_llm", c.hiringLlm)
                    put("llm_title", c.llmTitle)
                    put("llm_salary", c.llmSalary)
                    put("career_link", c.careerLink)
                    
                    val compArr = JSONArray()
                    for (comp in c.comparison) {
                        compArr.put(JSONObject().apply {
                            put("metric", comp.metric)
                            put("before", comp.before)
                            put("after", comp.after)
                        })
                    }
                    put("comparison", compArr)
                    
                    val planArr = JSONArray()
                    for (p in c.migrationPlan) {
                        planArr.put(JSONObject().apply {
                            put("days", p.days)
                            put("title", p.title)
                            put("detail", p.detail)
                        })
                    }
                    put("migration_plan", planArr)
                }
                jsonArray.put(obj)
            }
            getPrefs(context).edit().putString(KEY_SCANNED, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// Tech Stack Scanner and AI Recommendation engine
object TechStackScanner {
    
    fun scanWebsiteUrl(url: String): List<String> {
        val cleaned = url.lowercase(Locale.ROOT)
        val techList = mutableListOf<String>()

        // 1. Detect CMS / E-commerce
        if (cleaned.contains("shopify") || cleaned.contains("store") || cleaned.contains("shop")) {
            techList.add("Shopify")
        } else if (cleaned.contains("wordpress") || cleaned.contains("wp") || cleaned.contains("blog")) {
            techList.add("WordPress")
        }

        // 2. Detect Frontend frameworks
        if (cleaned.contains("react") || cleaned.contains("next") || cleaned.contains("app") || cleaned.contains("vercel")) {
            techList.add("React")
        } else if (cleaned.contains("angular") || cleaned.contains("ng")) {
            techList.add("Angular")
        } else if (cleaned.contains("vue") || cleaned.contains("nuxt")) {
            techList.add("Vue")
        }

        // 3. Detect Backend/Platform
        if (cleaned.contains("laravel") || cleaned.contains("php") || cleaned.contains("api")) {
            techList.add("Laravel")
        }
        
        if (cleaned.contains("firebase") || cleaned.contains("firestore") || cleaned.contains("auth")) {
            techList.add("Firebase")
        }

        // Always assume Node.js backend if React/Vue/Angular detected, or Laravel/Firebase is absent
        if (techList.contains("React") || techList.contains("Angular") || techList.contains("Vue")) {
            techList.add("Node.js")
        }

        // Default standard stack if URL is obscure or plain
        if (techList.isEmpty()) {
            techList.addAll(listOf("React", "Node.js", "Firebase"))
        }

        return techList.distinct()
    }

    fun generateCompanyFromScan(url: String): Company {
        val detectedStack = scanWebsiteUrl(url)
        val host = try {
            val uri = Uri.parse(url)
            val hostStr = uri.host ?: url
            hostStr.replace("www.", "")
        } catch (e: Exception) {
            url.replace("www.", "")
        }
        
        val companyName = host.split(".")[0].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val id = "scanned_" + companyName.lowercase(Locale.ROOT) + "_" + Random.nextInt(1000, 9999)

        // Calculate custom parameters based on detected stack
        val hasWordPress = detectedStack.contains("WordPress")
        val hasShopify = detectedStack.contains("Shopify")
        val hasLaravel = detectedStack.contains("Laravel")
        val hasReact = detectedStack.contains("React")
        val hasAngular = detectedStack.contains("Angular")
        
        val isLegacy = hasWordPress || hasLaravel
        val isModernWeb = hasReact || hasAngular
        
        val costBefore = if (hasWordPress) {
            Random.nextInt(60000, 110000)
        } else if (hasShopify) {
            Random.nextInt(80000, 140000)
        } else if (isLegacy && isModernWeb) {
            Random.nextInt(180000, 260000)
        } else {
            Random.nextInt(120000, 210000)
        }

        val savingPercent = if (hasWordPress) {
            0.65 // Massive savings by replacing WordPress server setup with headless serverless & static exports
        } else if (hasShopify) {
            0.45 // High transaction and plugin fees optimization
        } else if (hasLaravel) {
            0.55 // Modernizing PHP monolothic servers with serverless functions
        } else {
            0.70 // High potential cost reduction using AI coding, automated LLM customer support, and serverless hosting
        }

        val costAfterAi = (costBefore * (1 - savingPercent)).toInt()

        // Generate recommendations
        val weakness = when {
            hasWordPress -> "Vulnerable to security exploits. High plugin dependency leading to slow load times. Manual backup cycles and server scaling bottlenecks in Baku datacenter."
            hasShopify -> "Substantial monthly Shopify plugin fees and dynamic transaction cuts. Limited customization for custom Azerbaijani payment gateway integrations (e.g. Pashapay)."
            hasLaravel -> "Monolithic architecture limits deployment speed. Inflexible scalability during traffic spikes. High maintenance cost for dedicated virtual machines."
            else -> "High cloud computing overhead. Lack of automated client onboarding flows. High cost of manual customer support ticketing systems."
        }

        val aiSolution = when {
            hasWordPress -> "Migrate content to a Headless CMS, export static pages via Next.js to Cloudflare Edge. Deploy a custom Gemini agent to automate client blog content and localized SEO metadata."
            hasShopify -> "Re-architect store frontend with Next.js/React, connecting to Shopify's headless Storefront API. Deploy dynamic AI product search and personalized shopping recommendations using semantic embeddings."
            hasLaravel -> "Refactor monolithic APIs into serverless Node.js/Go functions. Integrate semantic AI-driven caching layers and automate backend tests using generative coding tools."
            else -> "Deploy unified LLM-driven Customer Experience orchestrators. Automate repetitive business workflows and deploy specialized support agents with custom knowledge bases."
        }

        val comparison = listOf(
            CompanyComparison("Server Hosting Runrate", "${costBefore / 4} AZN", "${costAfterAi / 10} AZN"),
            CompanyComparison("Manual Operations Hour", "40 hrs/wk", "6 hrs/wk"),
            CompanyComparison("Support Ticket Handling", "4.5 min/ticket", "Real-time AI Assist")
        )

        val migrationPlan = listOf(
            MigrationPlanStep("Day 1-15", "Technology Refactor Plan", "Configure headless API endpoints. Set up serverless edge routers in cloud. Sync legacy database to cloud PostgreSQL."),
            MigrationPlanStep("Day 16-45", "Gemini Orchestration Setup", "Inject Gemini REST API endpoints. Build context-aware vector indexes for Azerbaijani customer support questions."),
            MigrationPlanStep("Day 46-90", "Handoff & AI Native Scaling", "Audit server runrates and deprecate old VMs. Train staff on Copilot assistants and run final security penetration tests.")
        )

        return Company(
            id = id,
            name = companyName,
            currentStack = detectedStack.joinToString(", "),
            weakness = weakness,
            costBefore = costBefore,
            aiSolution = aiSolution,
            costAfterAi = costAfterAi,
            savingPercent = savingPercent,
            hiringLlm = Random.nextBoolean(),
            llmTitle = if (Random.nextBoolean()) "LLM Integration Developer" else "AI Support Agent Designer",
            llmSalary = "3,200 - 4,800 AZN",
            careerLink = "https://rabita.az",
            comparison = comparison,
            migrationPlan = migrationPlan
        )
    }

    // AI Readiness calculation helper
    fun calculateReadinessScore(company: Company): Int {
        val stack = company.currentStack.lowercase(Locale.ROOT)
        var score = 15 // Base score
        
        // Stack bonuses
        if (stack.contains("react") || stack.contains("vue")) score += 20
        if (stack.contains("node")) score += 15
        if (stack.contains("firebase")) score += 15
        if (stack.contains("shopify")) score += 10
        if (stack.contains("laravel")) score += 5
        if (stack.contains("wordpress")) score -= 5 // Negative bonus for legacy WP

        // Task progress bonus from migration tracker
        score += 10 // Assumption of default progress

        return score.coerceIn(0, 100)
    }

    fun getReadinessCategory(score: Int): String {
        return when {
            score <= 25 -> "Beginner"
            score <= 50 -> "Emerging"
            score <= 75 -> "Advanced"
            else -> "AI Native"
        }
    }
    
    fun getTechnicalDebtScore(company: Company): Int {
        val stack = company.currentStack.lowercase(Locale.ROOT)
        var debt = 50 // Default debt
        if (stack.contains("wordpress")) debt += 35
        if (stack.contains("laravel")) debt += 20
        if (stack.contains("react") || stack.contains("vue")) debt -= 15
        if (stack.contains("firebase")) debt -= 15
        if (stack.contains("shopify")) debt -= 5
        return debt.coerceIn(5, 95)
    }
}
