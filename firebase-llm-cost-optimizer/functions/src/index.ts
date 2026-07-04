import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { GoogleGenAI } from '@google/genai';

admin.initializeApp();
const db = admin.firestore();

// Initialize the Google Gen AI Client
// GoogleGenAI automatically utilizes the GEMINI_API_KEY environment variable.
const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

// Official 2026 pricing for Gemini 1.5/2.0 API Models (USD per Million tokens)
const MODEL_PRICING = {
  FLASH: { input: 0.075 / 1000000, output: 0.30 / 1000000 },
  PRO: { input: 1.25 / 1000000, output: 5.00 / 1000000 }
};

const AZN_PEG = 1.7000; // 1 USD = 1.7000 AZN (Central Bank of Azerbaijan)

interface ChatRequest {
  userId: string;
  message: string;
  conversationId: string;
}

/**
 * Cloud Function: costOptimizedChat
 * Evaluates request complexity, routes dynamically to save tokens,
 * and logs billing metrics directly in Firestore using cost equations.
 */
export const costOptimizedChat = functions.https.onCall(async (request, context) => {
  // Ensure the request is validated
  const data = request.data as ChatRequest;
  if (!data || !data.message || !data.userId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Required fields: userId, message, conversationId'
    );
  }

  const { userId, message, conversationId } = data;

  try {
    // -------------------------------------------------------------
    // PHASE 1: COST-AWARE ROUTING
    // -------------------------------------------------------------
    // Classify user inquiry using Gemini Flash to minimize triaging overhead.
    const triagerModel = 'gemini-1.5-flash';
    const triagerPrompt = `
You are an ultra-high performance cognitive triaging agent designed to minimize API token costs.
Analyze this user inquiry and classify its technical, analytical, or logical complexity.

Respond in strict JSON format with exactly two fields:
{
  "complexity": "LOW" | "HIGH",
  "reasoning": "A concise one-sentence justification."
}

Classify as LOW if the request is:
- A simple greeting, polite gesture, or trivial lookup.
- Basic factual question, generic FAQ retrieval, or spelling/grammar correction.

Classify as HIGH if the request requires:
- Financial audit, mathematical logic, or multi-variable formulas.
- Code generation, systems debugging, or database design.
- Synthesis of extensive reports or deep compliance audits.

User inquiry: "${message.replace(/"/g, '\\"')}"
    `;

    const triagerResponse = await ai.models.generateContent({
      model: triagerModel,
      contents: triagerPrompt,
      config: {
        responseMimeType: 'application/json'
      }
    });

    const classificationText = triagerResponse.text;
    let complexity: 'LOW' | 'HIGH' = 'LOW';
    let reasoning = 'Defaulting to low cost due to parsing failure.';

    if (classificationText) {
      try {
        const parsed = JSON.parse(classificationText.trim());
        complexity = parsed.complexity === 'HIGH' ? 'HIGH' : 'LOW';
        reasoning = parsed.reasoning || '';
      } catch (e) {
        console.error('Failed to parse complexity response JSON:', e);
      }
    }

    // Determine destination model based on complexity evaluation
    // LOW complexity maps to Flash (90%+ cheaper), HIGH complexity maps to Pro
    const selectedModel = complexity === 'LOW' ? 'gemini-1.5-flash' : 'gemini-1.5-pro';

    console.info(`[Router] Classified as ${complexity}. Routing to: ${selectedModel}. Reason: ${reasoning}`);

    // -------------------------------------------------------------
    // PHASE 2: SYSTEM EXECUTION
    // -------------------------------------------------------------
    // Execute actual LLM prompt using the selected model
    const systemInstruction = `
You are an expert enterprise business adviser assisting Baku banks, telecoms, and oil sectors.
Provide highly structured, technically rigorous feedback. 
Reference Azerbaijan's economic regulations, personal data laws, and Manat (AZN) currency where applicable.
    `;

    const modelResponse = await ai.models.generateContent({
      model: selectedModel,
      contents: message,
      config: {
        systemInstruction: systemInstruction,
        temperature: 0.2
      }
    });

    const completionText = modelResponse.text || 'Empty response generated.';

    // -------------------------------------------------------------
    // PHASE 3: TELEMETRY & COST ACCOUNTING (USD & AZN)
    // -------------------------------------------------------------
    // Extract token usage metrics from metadata response
    const inputTokens = modelResponse.usageMetadata?.promptTokenCount || 0;
    const outputTokens = modelResponse.usageMetadata?.candidatesTokenCount || 0;
    const totalTokens = inputTokens + outputTokens;

    // Calculate actual cost metrics
    const priceProfile = selectedModel.includes('pro') ? MODEL_PRICING.PRO : MODEL_PRICING.FLASH;
    const costUsd = (inputTokens * priceProfile.input) + (outputTokens * priceProfile.output);
    const costAzn = costUsd * AZN_PEG;

    // Log the event securely in Cloud Firestore for transparency
    const logData = {
      userId,
      conversationId,
      message,
      response: completionText,
      selectedModel,
      complexity,
      reasoning,
      metrics: {
        inputTokens,
        outputTokens,
        totalTokens,
        costUsd,
        costAzn
      },
      timestamp: admin.firestore.FieldValue.serverTimestamp()
    };

    await db.collection('llm_cost_logs').add(logData);

    return {
      success: true,
      response: completionText,
      modelUsed: selectedModel,
      complexityClass: complexity,
      tokenMetrics: {
        totalTokens,
        inputTokens,
        outputTokens
      },
      estimatedCosts: {
        usd: costUsd,
        azn: costAzn
      }
    };

  } catch (error: any) {
    console.error('Error executing cost-optimized chat function:', error);
    throw new functions.https.HttpsError(
      'internal',
      error.message || 'An unexpected error occurred during model routing.'
    );
  }
});
