package com.mchi.proyecto.api

import com.mchi.proyecto.api.models.HealthTipResponse
import retrofit2.http.GET

interface ApiService {
    @GET("advice")
    suspend fun getHealthTip(): HealthTipResponse
}
