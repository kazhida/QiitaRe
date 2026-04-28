package com.abplua.qiitare

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.abplua.qiitare.data.models.Item
import com.abplua.qiitare.data.repositories.AuthRepository
import com.abplua.qiitare.data.repositories.QiitaRepository
import com.abplua.qiitare.ui.App
import com.abplua.qiitare.ui.screens.TimelineScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.publicvalue.multiplatform.oidc.appsupport.AndroidCodeAuthFlowFactory

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val codeAuthFlowFactory = AndroidCodeAuthFlowFactory()
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    private val items = _items.asStateFlow()
    private var itemQuery: String? = null
    private var nextItemPage = 1
    private var isLoadingItems = false
    private var isItemLastPage = false
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
            loadItems(accessToken)
        }

        setContent {
            TimelineScreen(
                itemFlow = items,
                onLoadMore = ::loadNextItemPage,
            )
        }
    }

    private fun observeAuthentication() {
        lifecycleScope.launch {
            authRepository.authenticateAsync().collect { state ->
                if (state is AuthRepository.QiitaAuthState.Authenticated) {
                    qiitaTokenPreferences.saveAccessToken(state.accessToken)
                    loadItems(state.accessToken)
                }
            }
        }
    }

    private fun loadItems(accessToken: String) {
        lifecycleScope.launch {
            runCatching {
                val authenticatedUser = qiitaRepository.getAuthenticatedUser(accessToken)
                val followeeIds = getAllFolloweeIds(authenticatedUser.id)
                val followingTagIds = getAllFollowingTagIds(authenticatedUser.id)
                buildItemQuery(followeeIds = followeeIds, followingTagIds = followingTagIds)
            }.onSuccess { query ->
                itemQuery = query
                nextItemPage = 1
                isItemLastPage = query == null
                _items.value = emptyList()
                loadNextItemPage()
            }.onFailure { error ->
                Log.e(TAG, "Failed to get Qiita items.", error)
            }
        }
    }

    private fun loadNextItemPage() {
        val query = itemQuery ?: return
        if (isLoadingItems || isItemLastPage) return

        lifecycleScope.launch {
            isLoadingItems = true
            runCatching {
                qiitaRepository.getItems(
                    page = nextItemPage,
                    perPage = ITEMS_PER_PAGE,
                    query = query,
                )
            }.onSuccess { newItems ->
                if (newItems.isEmpty()) {
                    isItemLastPage = true
                } else {
                    _items.value = _items.value + newItems
                    nextItemPage += 1
                    if (newItems.size < ITEMS_PER_PAGE) {
                        isItemLastPage = true
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to get Qiita items.", error)
            }
            isLoadingItems = false
        }
    }

    private suspend fun getAllFolloweeIds(userId: String): List<String> {
        val followeeIds = mutableListOf<String>()
        for (page in 1..MAX_QIITA_PAGE) {
            val followees = qiitaRepository.getFollowees(userId = userId, page = page)
            if (followees.isEmpty()) {
                break
            }
            followeeIds += followees.map { it.id }
        }
        return followeeIds
    }

    private suspend fun getAllFollowingTagIds(userId: String): List<String> {
        val followingTagIds = mutableListOf<String>()
        for (page in 1..MAX_QIITA_PAGE) {
            val followingTags = qiitaRepository.getFollowingTags(userId = userId, page = page)
            if (followingTags.isEmpty()) {
                break
            }
            followingTagIds += followingTags.map { it.id }
        }
        return followingTagIds
    }

    private fun buildItemQuery(followeeIds: List<String>, followingTagIds: List<String>): String? {
        return listOfNotNull(
            followeeIds.takeIf { it.isNotEmpty() }?.joinToString(
                separator = ",",
                prefix = "user:",
            ),
            followingTagIds.takeIf { it.isNotEmpty() }?.joinToString(
                separator = ",",
                prefix = "tag:",
            ),
        ).joinToString(" ").ifBlank { null }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val MAX_QIITA_PAGE = 100
        private const val ITEMS_PER_PAGE = 20
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
