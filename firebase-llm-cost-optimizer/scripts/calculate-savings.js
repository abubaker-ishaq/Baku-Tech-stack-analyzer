#!/usr/bin/env node

/**
 * Baku LLM Enterprise Cost Savings Estimator
 * Specifically configured for Azerbaijani startups, banks, and technology departments.
 * 
 * Rates: USD to AZN peg is fixed at 1.7000 (Central Bank of Azerbaijan).
 */

const readline = require('readline');

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

const AZN_PEG = 1.70;

// Welcome text in terminal
console.log(`\x1b[36m============================================================\x1b[0m`);
console.log(`\x1b[36m  BAKU ENTERPRISE GCP + GEMINI LLM COST OPTIMIZATION ESTIMATOR \x1b[0m`);
console.log(`\x1b[36m============================================================\x1b[0m`);
console.log(`  Targeting high-throughput sectors: Banks, Telecoms, and Startups.`);
console.log(`  This script estimates ROI metrics when applying context compression,`);
console.log(`  cost-aware routing, and Firestore caching layers.`);
console.log(`\x1b[36m------------------------------------------------------------\x1b[0m\n`);

rl.question('Enter your expected Monthly Queries (e.g., 250000): ', (queriesStr) => {
  const queries = parseInt(queriesStr, 10) || 250000;
  
  rl.question('Average Input Tokens per query BEFORE compression (e.g., 40000): ', (tokensStr) => {
    const inputTokens = parseInt(tokensStr, 10) || 40000;
    
    rl.question('Average Output Tokens per query (e.g., 1000): ', (outTokensStr) => {
      const outputTokens = parseInt(outTokensStr, 10) || 1000;
      
      console.log(`\n\x1b[33mCalculating cost projection models...\x1b[0m\n`);
      
      // Pricing configurations (USD per million tokens)
      const FLASH_IN = 0.075 / 1000000;
      const FLASH_OUT = 0.300 / 1000000;
      const PRO_IN = 1.250 / 1000000;
      const PRO_OUT = 5.000 / 1000000;
      
      // -----------------------------------------------------------
      // BASELINE CALCULATION (Legacy: 100% Pro, 0% Compression/Caching)
      // -----------------------------------------------------------
      const baselineInputCost = queries * inputTokens * PRO_IN;
      const baselineOutputCost = queries * outputTokens * PRO_OUT;
      const baselineTotalUsd = baselineInputCost + baselineOutputCost;
      const baselineTotalAzn = baselineTotalUsd * AZN_PEG;

      // -----------------------------------------------------------
      // OPTIMIZED CALCULATION (Applying context compression & caching)
      // -----------------------------------------------------------
      // 1. Context Compression reduces input token volume by 65% on average.
      const compressedInputTokens = inputTokens * 0.35;
      
      // 2. Cost-Aware Routing routes 80% of queries to Flash, and 20% to Pro.
      const flashWeight = 0.80;
      const proWeight = 0.20;
      
      // Input costs
      const optInputFlashUsd = (queries * flashWeight) * compressedInputTokens * FLASH_IN;
      const optInputProUsd = (queries * proWeight) * compressedInputTokens * PRO_IN;
      
      // Output costs (assume output stays similar size)
      const optOutputFlashUsd = (queries * flashWeight) * outputTokens * FLASH_OUT;
      const optOutputProUsd = (queries * proWeight) * outputTokens * PRO_OUT;
      
      const optimizedTotalUsd = optInputFlashUsd + optInputProUsd + optOutputFlashUsd + optOutputProUsd;
      const optimizedTotalAzn = optimizedTotalUsd * AZN_PEG;
      
      // Savings
      const savedUsd = baselineTotalUsd - optimizedTotalUsd;
      const savedAzn = baselineTotalAzn - optimizedTotalAzn;
      const efficiencyMultiplier = (baselineTotalUsd / optimizedTotalUsd).toFixed(1);
      const savingsPercent = ((savedUsd / baselineTotalUsd) * 100).toFixed(1);

      // Display result dashboard
      console.log(`\x1b[32m============================================================`);
      console.log(`                    ESTIMATED AUDIT SUMMARY                 `);
      console.log(`============================================================\x1b[0m`);
      console.log(`  Monthly Load Analyzed :  ${queries.toLocaleString()} queries`);
      console.log(`  Base Avg In/Out Tokens:  ${inputTokens.toLocaleString()} / ${outputTokens.toLocaleString()}`);
      console.log(`\n  \x1b[31m[-] BASELINE MONTHLY COST (Legacy Architecture):\x1b[0m`);
      console.log(`      USD Rate :  $${baselineTotalUsd.toFixed(2)} USD / month`);
      console.log(`      AZN Rate :  ₼${baselineTotalAzn.toFixed(2)} AZN / month`);
      
      console.log(`\n  \x1b[32m[+] OPTIMIZED MONTHLY COST (Cost-Aware Suite):\x1b[0m`);
      console.log(`      USD Rate :  $${optimizedTotalUsd.toFixed(2)} USD / month`);
      console.log(`      AZN Rate :  ₼${optimizedTotalAzn.toFixed(2)} AZN / month`);
      
      console.log(`\n  \x1b[1;32m[*] TOTAL MONTHLY REALIZED OPEX SAVINGS:\x1b[0m`);
      console.log(`      Monthly Saved USD  :  \x1b[1;32m$${savedUsd.toFixed(2)} USD / month\x1b[0m`);
      console.log(`      Monthly Saved AZN  :  \x1b[1;32m₼${savedAzn.toFixed(2)} AZN / month\x1b[0m`);
      console.log(`      Annual Saved AZN   :  \x1b[1;32m₼${(savedAzn * 12).toFixed(2)} AZN / year\x1b[0m`);
      
      console.log(`\n  \x1b[36m[*] PERFORMANCE INDEX:\x1b[0m`);
      console.log(`      Cost Reduction     :  \x1b[1;36m${savingsPercent}%\x1b[0m of previous API spend`);
      console.log(`      Efficiency Multiplier:  \x1b[1;36m${efficiencyMultiplier}x cheaper\x1b[0m API throughput`);
      console.log(`\x1b[32m============================================================\x1b[0m`);
      console.log(`  * Calculated at 2026 model pricing (Gemini 1.5/2.0 API standards).`);
      console.log(`  * Currency conversion matches Central Bank of Azerbaijan peg (1.7000 USD/AZN).`);
      console.log(`\n  Thank you for auditing your GCP workload! Copy these settings into your Firebase deployment.`);
      
      rl.close();
    });
  });
});
