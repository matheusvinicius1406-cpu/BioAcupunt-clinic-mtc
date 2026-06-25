import { PrismaClient } from '@prisma/client'
const prisma = new PrismaClient()

async function main() {
  await prisma.synergy.createMany({
    data: [
      { 
        title: "Microagulhamento Estético", description: "...", procedure: "microagulhamento", category: "potencializa", 
        mainPoints: ["BP6", "IG11"], rationale: "...", precautions: "...", steps: ["1. ..."] 
      },
      // ... add others
    ]
  })
}

main().catch(e => { console.error(e); process.exit(1) }).finally(async () => await prisma.$disconnect())
