package com.abplua.qiitare.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assume.assumeTrue
import org.publicvalue.multiplatform.oidc.flows.AuthCodeResult
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class QiitaRepositoryLiveTest {

    @Test
    fun authenticateAsync_exchangesActualAuthorizationCode() = runTest {
        val authorizationCode = System.getenv("QIITA_AUTH_CODE")
        assumeTrue(
            "Set QIITA_AUTH_CODE to a fresh code returned from qiitare://oauth/callback",
            !authorizationCode.isNullOrBlank()
        )

        val repository = QiitaRepository(
            scope = this,
            authorizationCodeProvider = { _, request ->
                AuthCodeResult(code = authorizationCode, state = request.state)
            },
        )

        val flow = repository.authenticateAsync()

        val terminalState = awaitTerminalState(flow)
        assertTrue(
            terminalState is QiitaRepository.QiitaAuthState.Authenticated,
            "Expected Authenticated but was $terminalState"
        )
    }

    private suspend fun awaitTerminalState(flow: StateFlow<QiitaRepository.QiitaAuthState>): QiitaRepository.QiitaAuthState {
        withTimeout(10_000) {
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
