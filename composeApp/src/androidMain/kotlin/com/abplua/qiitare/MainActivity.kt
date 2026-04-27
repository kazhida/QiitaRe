package com.abplua.qiitare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.abplua.qiitare.repository.QiitaRepository
import kotlinx.coroutines.launch
import org.publicvalue.multiplatform.oidc.appsupport.AndroidCodeAuthFlowFactory

class MainActivity : ComponentActivity() {
    private val codeAuthFlowFactory = AndroidCodeAuthFlowFactory()
    private lateinit var qiitaRepository: QiitaRepository
    private lateinit var qiitaTokenPreferences: QiitaTokenPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        codeAuthFlowFactory.registerActivity(this)
        qiitaRepository = QiitaRepository(codeAuthFlowFactory = codeAuthFlowFactory)
        qiitaTokenPreferences = QiitaTokenPreferences(applicationContext)
        observeAuthentication()

        setContent {
            App()
        }
    }

    private fun observeAuthentication() {
        lifecycleScope.launch {
            qiitaRepository.authenticateAsync().collect { state ->
                if (state is QiitaRepository.QiitaAuthState.Authenticated) {
                    qiitaTokenPreferences.saveAccessToken(state.accessToken)
                }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
