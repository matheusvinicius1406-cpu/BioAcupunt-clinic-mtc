import { Request, Response } from "express";
import fs from "fs";
import path from "path";

const UPLOADS_DIR = path.join(process.cwd(), "uploads");
const FILES_DIR = path.join(UPLOADS_DIR, "files");
const METADATA_FILE = path.join(UPLOADS_DIR, "metadata.json");
const HISTORY_FILE = path.join(UPLOADS_DIR, "download_history.json");

// Minimal valid PDF byte structures for seeded clinical literature
const SEEDED_PDFS = [
  {
    fileName: "Guia Pratico BioAcupunt - Protocolo Clinico Prescritivo de Emergencia.pdf",
    title: "Guia Prático BioAcupunt - Protocolo Clínico Prescritivo de Emergência",
    category: "Protocolos Clínicos",
    size: 512,
    uploadedBy: "Dra. Camila Silva (Sistema)",
    content: `%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Kids [3 0 R] /Count 1 >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>
endobj
4 0 obj
<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
endobj
5 0 obj
<< /Length 120 >>
stream
BT
/F1 16 Tf
50 700 Td
(GUIAS CLINICOS BIOACUPUNT: ACUPUNTURA DE EMERGENCIA) Tj
/F1 12 Tf
0 -40 Td
(Diagnosticos e aplicacoes de pontos sistemicos e auriculares para condutas agudas.) Tj
0 -20 Td
(Pontos chave: IG4, F3, BP6, VG20, R3. Elaborado por Dra. Camila Silva.) Tj
ET
endstream
endobj
xref
0 6
0000000000 65535 f 
0000000009 00000 n 
0000000058 00000 n 
0000000114 00000 n 
0000000234 00000 n 
0000000305 00000 n 
trailer
<< /Size 6 /Root 1 0 R >>
startxref
480
%%EOF`
  },
  {
    fileName: "Tratado Classico de Acupuntura - Fundamentos e Canais Principais.pdf",
    title: "Tratado Clássico de Acupuntura - Fundamentos e Canais Principais",
    category: "Livros",
    size: 590,
    uploadedBy: "Dra. Camila Silva (Sistema)",
    content: `%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Kids [3 0 R] /Count 1 >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>
endobj
4 0 obj
<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
endobj
5 0 obj
<< /Length 150 >>
stream
BT
/F1 16 Tf
50 700 Td
(TRATADO CLASSICO: FUNDAMENTOS ENERGETICOS DA ACUPUNTURA) Tj
/F1 12 Tf
0 -40 Td
(Fisiologia dos Canais Principais, ramificacoes colaterais e a teoria Zang Fu.) Tj
0 -20 Td
(Uma revisao profunda elaborada para estudo continuo e raciocinio clinico.) Tj
0 -20 Td
(Origem: Biblioteca Particular Especializada Dra. Camila Silva.) Tj
ET
endstream
endobj
xref
0 6
0000000000 65535 f 
0000000009 00000 n 
0000000058 00000 n 
0000000114 00000 n 
0000000234 00000 n 
0000000305 00000 n 
trailer
<< /Size 6 /Root 1 0 R >>
startxref
510
%%EOF`
  },
  {
    fileName: "Analise Cientifica de Pontos Extras e Estudos Clinicos Modernos.pdf",
    title: "Análise Científica de Pontos Extras e Estudos Clínicos Modernos",
    category: "Artigos Científicos",
    size: 610,
    uploadedBy: "Dra. Camila Silva (Sistema)",
    content: `%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Kids [3 0 R] /Count 1 >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>
endobj
4 0 obj
<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
endobj
5 0 obj
<< /Length 160 >>
stream
BT
/F1 16 Tf
50 700 Td
(EVIDENCIAS MODERNAS: ESTUDOS CLINICOS EM ACUPUNTURA) Tj
/F1 12 Tf
0 -40 Td
(Pesquisas e ensaios clinicos randomizados mapeando respostas biologicas.) Tj
0 -20 Td
(Mecanismos do eixo HPA, histaminas, modulacao central e alivio neurologico.) Tj
0 -20 Td
(Biblioteca inteligente integrada a plataforma de CDSS BioAcupunt.) Tj
ET
endstream
endobj
xref
0 6
0000000000 65535 f 
0000000009 00000 n 
0000000058 00000 n 
0000000114 00000 n 
0000000234 00000 n 
0000000305 00000 n 
trailer
<< /Size 6 /Root 1 0 R >>
startxref
530
%%EOF`
  }
];

// Ensure required files & directory structure exist
function initStorage() {
  if (!fs.existsSync(UPLOADS_DIR)) {
    fs.mkdirSync(UPLOADS_DIR, { recursive: true });
  }
  if (!fs.existsSync(FILES_DIR)) {
    fs.mkdirSync(FILES_DIR, { recursive: true });
  }

  // Verify and write seeded clinical PDFs if not present
  let metadataList: any[] = [];
  if (fs.existsSync(METADATA_FILE)) {
    try {
      metadataList = JSON.parse(fs.readFileSync(METADATA_FILE, "utf-8"));
    } catch {
      metadataList = [];
    }
  }

  let metadataUpdated = false;

  for (const s of SEEDED_PDFS) {
    const filePath = path.join(FILES_DIR, s.fileName);
    if (!fs.existsSync(filePath)) {
      fs.writeFileSync(filePath, s.content, "utf-8");
    }

    const existsInMeta = metadataList.some((m) => m.fileName === s.fileName);
    if (!existsInMeta) {
      metadataList.push({
        id: `seeded-${s.fileName.replace(/\s+/g, "-").toLowerCase()}`,
        fileName: s.fileName,
        title: s.title,
        category: s.category,
        size: s.size,
        uploadedBy: s.uploadedBy,
        createdAt: new Date().toISOString(),
      });
      metadataUpdated = true;
    }
  }

  if (metadataUpdated || !fs.existsSync(METADATA_FILE)) {
    fs.writeFileSync(METADATA_FILE, JSON.stringify(metadataList, null, 2), "utf-8");
  }

  if (!fs.existsSync(HISTORY_FILE)) {
    fs.writeFileSync(HISTORY_FILE, JSON.stringify([], null, 2), "utf-8");
  }
}

export const PDFController = {
  // Lists all documents uploaded plus system default PDFs
  list: async (req: Request, res: Response) => {
    try {
      initStorage();
      const metadataList = JSON.parse(fs.readFileSync(METADATA_FILE, "utf-8"));
      res.json(metadataList);
    } catch (err: any) {
      res.status(500).json({ error: "Erro ao listar documentos PDF: " + err.message });
    }
  },

  // Uploads a new PDF document in Base64
  upload: async (req: Request, res: Response) => {
    try {
      initStorage();
      const { title, fileName, base64Data, category } = req.body;

      if (!fileName || !title || !base64Data) {
        return res.status(400).json({ error: "Parâmetros fileName, title e base64Data são obrigatórios." });
      }

      // Safe filename verification to prevent directory traversal
      const safeName = path.basename(fileName).replace(/[^a-zA-Z0-9.\-\s]/g, "_");
      if (!safeName.endsWith(".pdf")) {
        return res.status(400).json({ error: "Somente arquivos no formato .pdf são permitidos." });
      }

      const buffer = Buffer.from(base64Data, "base64");
      const targetPath = path.join(FILES_DIR, safeName);

      fs.writeFileSync(targetPath, buffer);

      // Save registry entry
      const metadataList = JSON.parse(fs.readFileSync(METADATA_FILE, "utf-8"));
      const newFileId = `pdf-${Math.random().toString(36).substring(2, 9)}`;

      const newEntry = {
        id: newFileId,
        fileName: safeName,
        title: title,
        category: category || "Outros Documentos",
        size: buffer.length,
        uploadedBy: "Dra. Camila Silva (Pessoal)",
        createdAt: new Date().toISOString(),
      };

      // Filter out existing one if overwriting
      const cleanList = metadataList.filter((m: any) => m.fileName !== safeName);
      cleanList.push(newEntry);

      fs.writeFileSync(METADATA_FILE, JSON.stringify(cleanList, null, 2), "utf-8");
      res.status(201).json(newEntry);
    } catch (err: any) {
      res.status(500).json({ error: "Erro ao fazer upload do PDF: " + err.message });
    }
  },

  // Downloads / serves attachment with full headers preserving original name
  download: async (req: Request, res: Response) => {
    try {
      initStorage();
      const { filename } = req.params;
      const safeName = path.basename(filename);
      const targetPath = path.join(FILES_DIR, safeName);

      if (!fs.existsSync(targetPath)) {
        return res.status(404).json({ error: "Arquivo PDF solicitado não existe fisicamente no servidor." });
      }

      // Log download to history
      const history = JSON.parse(fs.readFileSync(HISTORY_FILE, "utf-8"));
      history.push({
        id: `dl-${Math.random().toString(36).substring(2, 9)}`,
        fileName: safeName,
        timestamp: new Date().toISOString(),
        user: "Dra. Camila Silva",
        ip: req.ip || "127.0.0.1",
      });
      fs.writeFileSync(HISTORY_FILE, JSON.stringify(history, null, 2), "utf-8");

      res.setHeader("Content-Disposition", `attachment; filename="${safeName}"`);
      res.setHeader("Content-Type", "application/pdf");
      
      const stream = fs.createReadStream(targetPath);
      stream.pipe(res);
    } catch (err: any) {
      res.status(500).json({ error: "Erro ao efetuar download do arquivo: " + err.message });
    }
  },

  // Serves PDF natively inside the inline browser preview tab
  view: async (req: Request, res: Response) => {
    try {
      initStorage();
      const { filename } = req.params;
      const safeName = path.basename(filename);
      const targetPath = path.join(FILES_DIR, safeName);

      if (!fs.existsSync(targetPath)) {
        return res.status(404).json({ error: "Arquivo PDF solicitado não existe." });
      }

      res.setHeader("Content-Type", "application/pdf");
      res.setHeader("Content-Disposition", `inline; filename="${safeName}"`);

      const stream = fs.createReadStream(targetPath);
      stream.pipe(res);
    } catch (err: any) {
      res.status(500).json({ error: "Erro ao abrir visualização: " + err.message });
    }
  },

  // Retrives PDF download audit logs history
  getLogs: async (req: Request, res: Response) => {
    try {
      initStorage();
      const history = JSON.parse(fs.readFileSync(HISTORY_FILE, "utf-8"));
      res.json(history.slice().reverse()); // Newest first
    } catch (err: any) {
      res.status(500).json({ error: "Erro ao resgatar histórico de downloads: " + err.message });
    }
  }
};
