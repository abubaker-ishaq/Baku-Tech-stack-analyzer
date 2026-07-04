package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import java.io.File

object ReportExporter {

    fun generateExcelReportText(company: Company): String {
        val sb = StringBuilder()
        sb.append("Baku Tech Stack AI Analyzer - Executive Report\n")
        sb.append("Company Name,${company.name}\n")
        sb.append("Website Stack,${company.currentStack}\n")
        
        val score = TechStackScanner.calculateReadinessScore(company)
        val category = TechStackScanner.getReadinessCategory(score)
        val debt = TechStackScanner.getTechnicalDebtScore(company)
        
        sb.append("Digital Maturity Score,${score}/100 (${category})\n")
        sb.append("Technical Debt Index,${debt}/100\n")
        sb.append("Total IT Cost Before,${company.costBefore} AZN\n")
        sb.append("Cost After AI Adoption,${company.costAfterAi} AZN\n")
        sb.append("Estimated Savings %,${(company.savingPercent * 100).toInt()}%\n")
        sb.append("Estimated Savings AZN,${company.costBefore - company.costAfterAi} AZN\n")
        sb.append("AI Solution Proposed,${company.aiSolution.replace(",", ";")}\n")
        sb.append("Improvement Opportunity Identified,${company.weakness.replace(",", ";")}\n")
        
        sb.append("\nMigration Timeline:\n")
        for (step in company.migrationPlan) {
            sb.append("${step.days},${step.title.replace(",", ";")},${step.detail.replace(",", ";")}\n")
        }
        return sb.toString()
    }

    fun exportExcelReport(context: Context, company: Company): Uri? {
        val text = generateExcelReportText(company)
        val cacheFile = File(context.cacheDir, "CTO_Report_${company.name.replace(" ", "_")}.csv")
        try {
            cacheFile.writeText(text)
            
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "CTO_Report_${company.name.replace(" ", "_")}.csv")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/comma-separated-values")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
            }
            
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(text.toByteArray())
                }
                return uri
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun shareReport(context: Context, company: Company) {
        val text = """
            BAKU TECH STACK AI ANALYZER REPORT
            -----------------------------------------
            Company: ${company.name}
            Detected Stack: ${company.currentStack}
            Digital Maturity: ${TechStackScanner.calculateReadinessScore(company)}/100
            
            Estimated Savings: ${(company.savingPercent * 100).toInt()}% (${company.costBefore - company.costAfterAi} AZN saved!)
            Technical Debt Index: ${TechStackScanner.getTechnicalDebtScore(company)}/100
            
            AI Solution Proposed:
            ${company.aiSolution}
            
            Analyzed via Baku Tech Stack AI Analyzer.
        """.trimIndent()
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AI CTO Report for ${company.name}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share CTO Report"))
    }
}
