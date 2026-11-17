package com.example.data.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null
)

@JsonClass(generateAdapter = true)
data class ForecastResponse(
    @Json(name = "current_weather")
    val currentWeather: CurrentWeatherDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeatherDto(
    val temperature: Double,
    val windspeed: Double,
    val weathercode: Int,
    @Json(name = "is_day")
    val isDay: Int = 1
)

interface OpenMeteoGeocodingApi {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 1,
        @Query("language") language: String = "pt"
    ): GeocodingResponse
}

interface OpenMeteoForecastApi {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true
    ): ForecastResponse
}

object OpenMeteoClient {
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val geocodingApi: OpenMeteoGeocodingApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoGeocodingApi::class.java)
    }

    val forecastApi: OpenMeteoForecastApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoForecastApi::class.java)
    }

    fun weatherCodeToLabelPt(code: Int): String = when (code) {
        0 -> "Céu limpo"
        1 -> "Predominantemente limpo"
        2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Neblina"
        51, 53, 55 -> "Garoa leve"
        61, 63, 65 -> "Chuva"
        66, 67 -> "Chuva congelante"
        71, 73, 75, 77 -> "Neve"
        80, 81, 82 -> "Pancadas de chuva"
        85, 86 -> "Neve intensa"
        95, 96, 99 -> "Trovoada"
        else -> "Condições variáveis"
    }
}
