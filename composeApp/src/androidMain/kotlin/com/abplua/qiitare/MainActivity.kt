package com.abplua.qiitare

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.abplua.qiitare.data.repositories.AuthRepository
import com.abplua.qiitare.data.repositories.QiitaRepository
import com.abplua.qiitare.ui.App
import com.abplua.qiitare.ui.screens.TimelineScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.publicvalue.multiplatform.oidc.appsupport.AndroidCodeAuthFlowFactory

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val codeAuthFlowFactory = AndroidCodeAuthFlowFactory()
    private lateinit var authRepository: AuthRepository
    private lateinit var qiitaRepository: QiitaRepository
    private lateinit var qiitaTokenPreferences: QiitaTokenPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        codeAuthFlowFactory.registerActivity(this)
        qiitaTokenPreferences = QiitaTokenPreferences(applicationContext)
        authRepository = AuthRepository(codeAuthFlowFactory = codeAuthFlowFactory)
        qiitaRepository = QiitaRepository()

        val accessToken = qiitaTokenPreferences.getAccessToken()
        if (accessToken.isNullOrBlank()) {
            observeAuthentication()
        } else {
            logItemTitles()
        }

        setContent {
            TimelineScreen()
        }
    }

    private fun observeAuthentication() {
        lifecycleScope.launch {
            authRepository.authenticateAsync().collect { state ->
                if (state is AuthRepository.QiitaAuthState.Authenticated) {
                    qiitaTokenPreferences.saveAccessToken(state.accessToken)
                    logItemTitles()
                }
            }
        }
    }

    private fun logItemTitles() {
        lifecycleScope.launch {
            runCatching {
                qiitaRepository.getItems()
            }.onSuccess { items ->
                items.forEach { item ->
                    Log.d(TAG, item.title)
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to get Qiita items.", error)
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
