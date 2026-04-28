package com.abplua.qiitare.data.repositories

import com.abplua.qiitare.data.models.Item
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class QiitaRepository(
    private val httpClient: HttpClient = defaultHttpClient(),
    private val baseUrl: String = BASE_URL,
) {

    suspend fun getItems(page: Int = 1, perPage: Int = 20, query: String? = null): List<Item> {
        require(page in 1..100) { "page must be between 1 and 100." }
        require(perPage in 1..100) { "perPage must be between 1 and 100." }

        val response = httpClient.get("$baseUrl/items") {
            accept(ContentType.Application.Json)
            parameter("page", page)
            parameter("per_page", perPage)
            if (!query.isNullOrBlank()) {
                parameter("query", query)
            }
        }

        if (response.status.value !in 200..299) {
            val body = runCatching { response.body<String>() }.getOrDefault("")
            throw IllegalStateException(
                "Qiita items request failed: ${response.status.value} ${response.status.description}. $body"
                    .trim()
            )
        }

        return response.body<List<Item>>()
    }

    companion object {
        private const val BASE_URL = "https://qiita.com/api/v2"

        private fun defaultHttpClient(): HttpClient {
            return HttpClient {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        }
    }
}
