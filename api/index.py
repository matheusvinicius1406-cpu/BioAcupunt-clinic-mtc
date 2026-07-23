"""
Vercel Serverless Function — BioAcupunt FastAPI backend.

A Vercel espera que cada arquivo dentro de `api/` exponha um `handler`
(HTTP handler) que recebe requests HTTP e retorna responses.

Para FastAPI (ASGI), o `handler` é o proprio app ASGI encapsulado.
O `@asgi` decorator do Mangum faz a ponte entre o formato serverless
da Vercel (AWS Lambda format) e o protocolo ASGI do FastAPI.

A importação do `backend.app.main` funciona porque a Vercel adiciona
automaticamente a raiz do projeto ao sys.path.
"""

import sys
import os

# Adiciona o diretório backend/ ao path para importar o app FastAPI
_backend_path = os.path.join(os.path.dirname(__file__), '..', 'backend')
if _backend_path not in sys.path:
    sys.path.insert(0, _backend_path)

# =============================================================================
# Importação do app FastAPI
# =============================================================================
# O `backend.app.main` expõe `app: FastAPI` instanciado no módulo principal.
# Como o lifespan inclui bootstrap_admin_if_empty, a primeira requisição
# pode ser um pouco mais lenta (cold start + bootstrap).
from app.main import app as _fastapi_app

# =============================================================================
# Handler para Vercel (via Mangum — adaptador ASGI → AWS Lambda)
# =============================================================================
# Vercel executa Python num runtime compatível com AWS Lambda.
# O Mangum traduz o evento Lambda em chamadas ASGI para o FastAPI.
#
# NOTA: lifespan="off" porque o lifespan do FastAPI roda `bootstrap_admin_if_empty`
# (cria usuário admin se não existir). Em serverless, o lifespan roda a cada
# cold start — o bootstrap falharia na segunda execução porque o admin já existe.
# Em vez disso, o bootstrap é executado sob demanda na primeira requisição
# (ver `_ensure_bootstrap` abaixo).
#
# Alternativa: se a Vercel suportar ASGI nativamente no futuro,
#   podemos exportar diretamente `app` como handler.
try:
    from mangum import Mangum
    handler = Mangum(_fastapi_app, lifespan="off")
except ImportError:
    # Fallback: se Mangum não estiver instalado, expõe o app diretamente.
    # Alguns runtimes Vercel mais recentes suportam ASGI nativamente.
    handler = _fastapi_app


# =============================================================================
# Bootstrap sob demanda (compensa o lifespan="off")
# =============================================================================
# O FastAPI app tem um lifespan que executa bootstrap_admin_if_empty().
# Com lifespan="off" no Mangum, precisamos garantir que o bootstrap rode
# pelo menos uma vez. Usamos um closure que intercepta a primeira chamada.

_bootstrapped = False
_bootstrap_lock = __import__("asyncio").Lock()


async def _ensure_bootstrap():
    global _bootstrapped
    if _bootstrapped:
        return
    async with _bootstrap_lock:
        if _bootstrapped:  # double-check: evita corrida em cold start
            return
        try:
            from app.services.bootstrap import bootstrap_admin_if_empty
            from app.db.session import SessionLocal
            async with SessionLocal() as session:
                await bootstrap_admin_if_empty(session)
            _bootstrapped = True
        except Exception as e:
            # Se falhar (ex: banco ainda não disponível), tenta de novo na próxima
            _bootstrapped = False
            import logging
            logging.warning("Bootstrap admin failed (will retry): %s", e)


# =============================================================================
# Middleware de bootstrap: intercepta toda requisição para garantir bootstrap
# =============================================================================
from starlette.middleware.base import BaseHTTPMiddleware


class _BootstrapMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        await _ensure_bootstrap()
        return await call_next(request)


_fastapi_app.add_middleware(_BootstrapMiddleware)

# =============================================================================
# Import explícito para evitar que ferramentas de lint removam o import
# =============================================================================
__all__ = ["handler"]
