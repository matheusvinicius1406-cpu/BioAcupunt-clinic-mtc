#!/usr/bin/env python3
"""
Gera um comando SQL para reverter a aprovação de um item na biblioteca.

Uso:
  1. Descubra o ID do item que foi aprovado acidentalmente:
     python scripts/unapprove_item.py --list-db
     (mostra todos os itens aprovados no banco)

  2. Rejeite o item:
     python scripts/unapprove_item.py --id pcdt_fibrose_cistica_0005

  3. Ou gere o SQL manualmente:
     python scripts/unapprove_item.py --id pcdt_fibrose_cistica_0005 --sql-only

Pré-requisitos: adb conectado ao device com a build debug.
"""

import subprocess
import json
import sys
import re

DB_PATH = "/data/data/com.bioacupunt/databases/bioacupunt_db"


def run_adb_sql(sql: str):
    """Roda um comando SQL via adb shell."""
    cmd = f"adb shell 'sqlite3 \"{DB_PATH}\" \"{sql}\"'"
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Erro: {result.stderr}")
        return None
    return result.stdout.strip()


def list_approved():
    """Lista todos os itens aprovados no banco."""
    sql = """
    SELECT id, type, substr(title, 1, 60) as title_short,
           substr(metadata, 1, 80) as meta_preview
    FROM biblioteca_nodes
    WHERE json_extract(metadata, '$.status') = 'APPROVED'
    ORDER BY id;
    """
    print("Itens aprovados no banco:")
    print("-" * 80)
    rows = run_adb_sql(sql)
    if rows:
        print(rows)
    else:
        print("Nenhum item aprovado encontrado ou não foi possível conectar.")


def reject_item(item_id: str, sql_only: bool = False):
    """Rejeita (desaprova) um item específico."""
    now_ts = "datetime('now')"
    sql = f"""
    UPDATE biblioteca_nodes
    SET metadata = json_set(metadata,
            '$.status', 'REJECTED',
            '$.reviewedAt', {now_ts})
    WHERE id = '{item_id}'
      AND json_extract(metadata, '$.status') = 'APPROVED';
    """
    if sql_only:
        print("SQL para rejeitar o item:")
        print(sql)
        return

    print(f"Rejeitando item: {item_id}...")
    result = run_adb_sql(sql)
    if result is not None:
        # Checa se alguma linha foi afetada
        count = run_adb_sql("SELECT changes();")
        print(f"Linhas afetadas: {count}")
        if count and count != "0":
            print(f"✓ Item '{item_id}' rejeitado com sucesso!")
        else:
            print(f"Item '{item_id}' não encontrado ou já não estava aprovado.")
    else:
        print("Erro ao conectar ao banco via adb.")
        print()
        print("SQL manual (rode no seu terminal com adb):")
        print(sql)


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return

    if "--list-db" in sys.argv:
        list_approved()
    elif "--id" in sys.argv:
        idx = sys.argv.index("--id")
        if idx + 1 < len(sys.argv):
            item_id = sys.argv[idx + 1]
            sql_only = "--sql-only" in sys.argv
            reject_item(item_id, sql_only)
        else:
            print("Erro: informe o ID após --id")
    else:
        print("Use --list-db para listar itens aprovados ou --id <id> para rejeitar.")


if __name__ == "__main__":
    main()
