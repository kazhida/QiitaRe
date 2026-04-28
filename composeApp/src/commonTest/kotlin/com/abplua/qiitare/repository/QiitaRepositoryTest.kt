package com.abplua.qiitare.repository

import com.abplua.qiitare.data.repositories.QiitaRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QiitaRepositoryTest {

    @Test
    fun getItems_requestsItemsWithPagingAndQuery() = runTest {
        val repository = QiitaRepository(
            httpClient = testHttpClient(
                MockEngine { request ->
                    assertEquals("https://qiita.com/api/v2/items?page=2&per_page=50&query=qiita+user%3AQiita", request.url.toString())
                    assertEquals("GET", request.method.value)
                    respond(
                        content = itemListJson,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }
            )
        )

        val items = repository.getItems(page = 2, perPage = 50, query = "qiita user:Qiita")

        assertEquals(1, items.size)
        assertEquals("Example title", items.first().title)
        assertEquals("Ruby", items.first().tags.first().name)
        assertEquals(listOf("0.0.1"), items.first().tags.first().versions)
    }

    @Test
    fun getItems_omitsBlankQuery() = runTest {
        val repository = QiitaRepository(
            httpClient = testHttpClient(
                MockEngine { request ->
                    assertEquals("https://qiita.com/api/v2/items?page=1&per_page=20", request.url.toString())
                    respond(
                        content = "[]",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }
            )
        )

        assertEquals(emptyList(), repository.getItems(query = " "))
    }

    @Test
    fun getItems_throwsWhenQiitaReturnsError() = runTest {
        val repository = QiitaRepository(
            httpClient = testHttpClient(
                MockEngine {
                    respond(
                        content = """{"message":"Rate limit exceeded","type":"rate_limit_exceeded"}""",
                        status = HttpStatusCode.TooManyRequests,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }
            )
        )

        val error = assertFailsWith<IllegalStateException> {
            repository.getItems()
        }

        assertEquals(
            """Qiita items request failed: 429 Too Many Requests. {"message":"Rate limit exceeded","type":"rate_limit_exceeded"}""",
            error.message,
        )
    }

    @Test
    fun getItems_validatesPagingRange() = runTest {
        val repository = QiitaRepository(
            httpClient = testHttpClient(MockEngine {
                error("Invalid paging should not call HttpClient")
            })
        )

        assertFailsWith<IllegalArgumentException> {
            repository.getItems(page = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            repository.getItems(perPage = 101)
        }
    }

    private fun testHttpClient(engine: MockEngine): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    private val itemListJson = """
        [
          {
            "rendered_body": "<h1>Example</h1>",
            "body": "# Example",
            "coediting": false,
            "comments_count": 100,
            "created_at": "2000-01-01T00:00:00+00:00",
            "id": "c686397e4a0f4f11683d",
            "likes_count": 100,
            "private": false,
            "reactions_count": 100,
            "stocks_count": 100,
            "tags": [
              {
                "name": "Ruby",
                "versions": ["0.0.1"]
              }
            ],
            "title": "Example title",
            "updated_at": "2000-01-01T00:00:00+00:00",
            "url": "https://qiita.com/Qiita/items/c686397e4a0f4f11683d",
            "user": {
              "description": "Hello, world.",
              "facebook_id": "qiita",
              "followees_count": 100,
              "followers_count": 200,
              "github_login_name": "qiitan",
              "id": "qiita",
              "items_count": 300,
              "linkedin_id": "qiita",
              "location": "Tokyo, Japan",
              "name": "Qiita キータ",
              "organization": "Qiita Inc.",
              "permanent_id": 1,
              "profile_image_url": "https://example.com/image.png",
              "team_only": false,
              "twitter_screen_name": "qiita",
              "website_url": "https://qiita.com"
            },
            "page_views_count": null,
            "organization_url_name": null,
            "slide": false
          }
        ]
    """.trimIndent()
}
