from fastapi import APIRouter, Depends, HTTPException
from app.core.supabase_client import supabase
from app.models.schemas import User

router = APIRouter()

@router.post("/register")
async def register(email: str, password: str):
    return supabase.auth.sign_up({"email": email, "password": password})

@router.post("/login")
async def login(email: str, password: str):
    return supabase.auth.sign_in_with_password({"email": email, "password": password})

@router.post("/refresh")
async def refresh(refresh_token: str):
    return supabase.auth.refresh_session(refresh_token)
