package com.mchi.proyecto.api.models

data class HealthTipResponse(
    val slip: Slip
)

data class Slip(
    val id: Int,
    val advice: String
)
