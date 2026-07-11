import pytest

from app.core.security import (
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_document,
    hash_password,
    verify_password,
)

pytestmark = pytest.mark.unit


def test_password_hash_roundtrip():
    hashed = hash_password("MinhaSenh@123")
    assert verify_password("MinhaSenh@123", hashed)
    assert not verify_password("SenhaErrada", hashed)


def test_password_hash_is_not_plaintext():
    hashed = hash_password("MinhaSenh@123")
    assert hashed != "MinhaSenh@123"


def test_document_hash_is_deterministic():
    assert hash_document("123.456.789-00") == hash_document("123.456.789-00")


def test_document_hash_ignores_formatting():
    assert hash_document("123.456.789-00") == hash_document("12345678900")


def test_document_hash_never_reveals_raw_document():
    hashed = hash_document("123.456.789-00")
    assert "123" not in hashed and "456" not in hashed


def test_document_hash_differs_for_different_documents():
    assert hash_document("11111111111") != hash_document("22222222222")


def test_access_token_roundtrip():
    token = create_access_token(subject="42", clinic_id=7, role="admin")
    payload = decode_token(token)
    assert payload["sub"] == "42"
    assert payload["clinic_id"] == 7
    assert payload["role"] == "admin"
    assert payload["type"] == "access"


def test_refresh_token_roundtrip():
    token = create_refresh_token(subject="42", session_id="abc-123")
    payload = decode_token(token)
    assert payload["sub"] == "42"
    assert payload["sid"] == "abc-123"
    assert payload["type"] == "refresh"


def test_decode_token_rejects_garbage():
    with pytest.raises(ValueError):
        decode_token("not-a-real-token")
