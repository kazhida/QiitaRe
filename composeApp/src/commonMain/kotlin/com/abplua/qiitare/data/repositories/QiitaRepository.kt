package com.abplua.qiitare.data.repositories

import com.abplua.qiitare.data.models.AuthenticatedUser
import com.abplua.qiitare.data.models.FollowingTag
import com.abplua.qiitare.data.models.Article
import com.abplua.qiitare.data.models.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class QiitaRepository(
    private val httpClient: HttpClient = defaultHttpClient(),
    private val baseUrl: String = BASE_URL,
) {

    suspend fun getItems(page: Int = 1, perPage: Int = 20, query: String? = null): List<Article> {
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

        return response.body<List<Article>>()
    }

    suspend fun getAuthenticatedUser(accessToken: String): AuthenticatedUser {
        require(accessToken.isNotBlank()) { "accessToken must not be blank." }

        val response = httpClient.get("$baseUrl/authenticated_user") {
            accept(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }

        if (response.status.value !in 200..299) {
            val body = runCatching { response.body<String>() }.getOrDefault("")
            if (response.status == HttpStatusCode.Unauthorized) {
                throw InvalidAccessTokenException(body)
            }
            throw IllegalStateException(
                "Qiita authenticated user request failed: ${response.status.value} ${response.status.description}. $body"
                    .trim()
            )
        }

        return response.body<AuthenticatedUser>()
    }

    suspend fun getFollowees(userId: String, page: Int = 1, perPage: Int = 100): List<User> {
        require(userId.isNotBlank()) { "userId must not be blank." }
        require(page in 1..100) { "page must be between 1 and 100." }
        require(perPage in 1..100) { "perPage must be between 1 and 100." }

        val response = httpClient.get("$baseUrl/users/$userId/followees") {
            accept(ContentType.Application.Json)
            parameter("page", page)
            parameter("per_page", perPage)
        }

        if (response.status.value !in 200..299) {
            val body = runCatching { response.body<String>() }.getOrDefault("")
            throw IllegalStateException(
                "Qiita followees request failed: ${response.status.value} ${response.status.description}. $body"
                    .trim()
            )
        }

        return response.body<List<User>>()
    }

    suspend fun getFollowingTags(userId: String, page: Int = 1, perPage: Int = 100): List<FollowingTag> {
        require(userId.isNotBlank()) { "userId must not be blank." }
        require(page in 1..100) { "page must be between 1 and 100." }
        require(perPage in 1..100) { "perPage must be between 1 and 100." }

        val response = httpClient.get("$baseUrl/users/$userId/following_tags") {
            accept(ContentType.Application.Json)
            parameter("page", page)
            parameter("per_page", perPage)
        }

        if (response.status.value !in 200..299) {
            val body = runCatching { response.body<String>() }.getOrDefault("")
            throw IllegalStateException(
                "Qiita following tags request failed: ${response.status.value} ${response.status.description}. $body"
                    .trim()
            )
        }

        return response.body<List<FollowingTag>>()
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

    class InvalidAccessTokenException(responseBody: String) : IllegalStateException(
        "Qiita access token is invalid. $responseBody".trim()
    )
}
