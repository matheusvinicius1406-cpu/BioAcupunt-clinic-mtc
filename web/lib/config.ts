// Base URL do backend FastAPI. Definida em NEXT_PUBLIC_API_URL.
// Usada tanto no servidor (server components / route handlers) quanto exposta
// ao cliente pelo prefixo NEXT_PUBLIC_ (embora o cliente nunca fale direto com
// o backend — todo trafego passa pelos route handlers do Next para o token
// httpOnly nunca vazar para o JS do navegador).
export const API_URL = (
  process.env.NEXT_PUBLIC_API_URL ?? "http://127.0.0.1:8000"
).replace(/\/+$/, "");
