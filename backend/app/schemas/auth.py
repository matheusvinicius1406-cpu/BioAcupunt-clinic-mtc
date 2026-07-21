from pydantic import BaseModel, EmailStr, Field


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class RegisterRequest(BaseModel):
    """Self-signup for a solo practitioner: creates her clinic + admin user in
    one call and logs her straight in. Password floor is enforced here so a weak
    secret never reaches the hasher."""

    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    full_name: str = Field(min_length=1, max_length=200)
    clinic_name: str | None = Field(default=None, max_length=200)


class RefreshRequest(BaseModel):
    refresh_token: str


class TokenPairResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class UserResponse(BaseModel):
    id: int
    clinic_id: int
    email: str
    full_name: str
    role: str

    model_config = {"from_attributes": True}
