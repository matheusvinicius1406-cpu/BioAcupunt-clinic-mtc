"""Quick script to test the Neon database connection with the production env vars."""
import sys
sys.path.insert(0, '.')

import asyncio
from app.db.session import engine
from app.core.config import get_settings

async def main():
    settings = get_settings()
    print(f"Connecting to: {settings.database_url[:50]}...")
    
    try:
        async with engine.connect() as conn:
            print("✓ DB connected!")
            result = await conn.execute(await conn.text("SELECT 1"))
            print(f"✓ Query result: {result.scalar()}")
        print("✓ All checks passed!")
    except Exception as e:
        print(f"✗ Connection failed: {type(e).__name__}: {e}")
    finally:
        await engine.dispose()

asyncio.run(main())
