import type { Metadata, Viewport } from "next";

import "./globals.css";

export const metadata: Metadata = {
  title: "BioAcupunt — Painel",
  description:
    "Painel web complementar do BioAcupunt: agenda, pacientes, financeiro e relatórios. Somente gestão — sem prontuário clínico.",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="pt-BR">
      <body>{children}</body>
    </html>
  );
}
