import { GoogleGenerativeAI } from "@google/generative-ai";

class GeminiService {
  private genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY || "");
  private model = this.genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

  async generateChatResponse(prompt: string, systemInstruction: string, history: any[] = [], responseMimeType?: string) {
    const chat = this.model.startChat({
      history: history.map(h => ({
        role: h.role === 'assistant' ? 'model' : 'user',
        parts: [{ text: h.content }]
      })),
      generationConfig: {
        maxOutputTokens: 1000,
        responseMimeType: responseMimeType || "text/plain",
      },
      systemInstruction: {
        parts: [{ text: systemInstruction }]
      } as any
    });

    const result = await chat.sendMessage(prompt);
    return result.response.text();
  }
}

export default new GeminiService();
