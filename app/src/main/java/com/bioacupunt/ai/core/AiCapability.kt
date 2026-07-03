package com.bioacupunt.ai.core

enum class AiCapability(val value: String) {
    Chat("chat"),
    Vision("vision"),
    OCR("ocr"),
    ImageGeneration("image_generation"),
    DocumentAnalysis("document_analysis"),
    PdfProcessing("pdf_processing"),
    Embeddings("embeddings"),
    FunctionCalling("function_calling"),
    Streaming("streaming"),
    Reasoning("reasoning"),
    LongContext("long_context"),
    Translation("translation"),
    Summarization("summarization"),
    ToolCalling("tool_calling"),
    Rag("rag"),
    StructuredOutput("structured_output")
}
