package com.abplua.qiitare.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import org.publicvalue.multiplatform.oidc.flows.AuthCodeResult
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class QiitaRepositoryTest {

    @Test
    fun oauthAuthorize_returnsQiitaAuthorizeUrl() {
        val repository = QiitaRepository(
            httpClient = testHttpClient(MockEngine {
                error("oauthAuthorize should not call HttpClient")
            }),
            authorizationCodeProvider = { _, _ ->
                error("oauthAuthorize should not request authorization code")
            }
        )

        assertEquals(
            "https://qiita.com/api/v2/oauth/authorize?client_id=4dddfc67c0f429370a32390e072f684a3a0616c6&scope=read_qiita&state=",
            repository.oauthAuthorize()
        )
    }

    @Test
    fun authenticateAsync_returnsAccessTokenAfterOidcReturnsCode() = runTest {
        val repository = QiitaRepository(
            httpClient = testHttpClient(
                MockEngine { request ->
                    assertEquals("https://qiita.com/api/v2/access_tokens", request.url.toString())
                    assertEquals("POST", request.method.value)
                    respond(
                        content = """{"token":"issued-token"}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                    )
                }
            ),
            scope = this,
            authorizationCodeProvider = { _, request ->
                AuthCodeResult(code = "issued-code", state = request.state)
            },
        )

        val flow = repository.authenticateAsync()
        advanceUntilIdle()

        assertEquals(
            QiitaRepository.QiitaAuthState.Authenticated("issued-token"),
            awaitTerminalState(flow)
        )
    }

    @Test
    fun authenticateAsync_returnsFailedWhenOidcReturnsNoCode() = runTest {
        val repository = QiitaRepository(
            httpClient = testHttpClient(MockEngine {
                error("token exchange should not be called without code")
            }),
            scope = this,
            authorizationCodeProvider = { _, request ->
                AuthCodeResult(code = null, state = request.state)
            },
        )

        val flow = repository.authenticateAsync()
        advanceUntilIdle()

        assertEquals(
            QiitaRepository.QiitaAuthState.Failed("Qiita redirect did not contain code."),
            awaitTerminalState(flow)
        )
    }

    @Test
    fun authenticateAsync_returnsFailedWhenStateDoesNotMatch() = runTest {
        val repository = QiitaRepository(
            httpClient = testHttpClient(MockEngine {
                error("token exchange should not be called when state mismatches")
            }),
            scope = this,
            authorizationCodeProvider = { _, _ ->
                AuthCodeResult(code = "issued-code", state = "mismatched-state")
            },
        )

        val flow = repository.authenticateAsync()
        advanceUntilIdle()

        assertEquals(
            QiitaRepository.QiitaAuthState.Failed("Qiita redirect state mismatch."),
            awaitTerminalState(flow)
        )
    }

    @Test
    fun authenticateAsync_returnsFailedWhenQiitaRejectsAccessTokenRequest() = runTest {
        val repository = QiitaRepository(
            httpClient = testHttpClient(
                MockEngine {
                    respond(
                        content = """{"message":"Unauthorized"}""",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString())
                    )
                }
            ),
            scope = this,
            authorizationCodeProvider = { _, request ->
                AuthCodeResult(code = "invalid-code", state = request.state)
            },
        )

        val flow = repository.authenticateAsync()
        advanceUntilIdle()

        assertEquals(
            QiitaRepository.QiitaAuthState.Failed(
                "Qiita authentication failed: 401 Unauthorized. {\"message\":\"Unauthorized\"}"
            ),
            awaitTerminalState(flow)
        )
    }

    private fun testHttpClient(engine: MockEngine): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    private suspend fun awaitTerminalState(flow: StateFlow<QiitaRepository.QiitaAuthState>): QiitaRepository.QiitaAuthState {
        withTimeout(5_000) {
            while (true) {
                when (flow.value) {
                    is QiitaRepository.QiitaAuthState.Authenticated,
                    is QiitaRepository.QiitaAuthState.Failed -> return@withTimeout
                    else -> yield()
                }
            }
        }
        return flow.value
    }
}
