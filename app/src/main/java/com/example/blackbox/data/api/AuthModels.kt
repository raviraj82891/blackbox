package com.example.blackbox.data.api

data class AuthRequest(
    val email: String,
    val passwordHash: String
)

data class AuthResponse(
    val token: String,
    val userId: String,
    val message: String? = null
)

data class EmergencyContactDto(
    val id: String? = null,
    val name: String,
    val phone: String,
    val email: String,
    val relationship: String
)
