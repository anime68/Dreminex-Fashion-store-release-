package com.dreminexfashion.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dreminexfashion.store.data.CartLine
import com.dreminexfashion.store.data.StoreCategory
import com.dreminexfashion.store.data.StoreManifest
import com.dreminexfashion.store.data.StoreProduct
import com.dreminexfashion.store.network.StoreApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

 data class StoreUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val manifest: StoreManifest = StoreManifest(),
    val categories: List<StoreCategory> = emptyList(),
    val products: List<StoreProduct> = emptyList(),
    val cart: Map<Int, Int> = emptyMap(),
    val search: String = "",
    val category: String = "",
    val message: String = "",
    val selectedTab: StoreTab = StoreTab.HOME,
)

enum class StoreTab { HOME, SEARCH, ACCOUNT, FAVORITES, CART }

class StoreViewModel(private var api: StoreApi) : ViewModel() {
    private val _state = MutableStateFlow(StoreUiState())
    val state: StateFlow<StoreUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        val first = _state.value.loading
        _state.value = _state.value.copy(loading = first, refreshing = !first, message = "")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val firstManifest = api.manifest()
                val site = firstManifest.site_url.trim()
                if (site.isNotBlank() && site.trimEnd('/') != BuildConfig.STORE_URL.trimEnd('/')) {
                    api = StoreApi(site)
                }
                val manifest = if (site.isNotBlank()) api.manifest() else firstManifest
                val categories = api.categories()
                val catalog = api.catalog(search = _state.value.search, category = _state.value.category)
                _state.value = _state.value.copy(loading = false, refreshing = false, manifest = manifest, categories = categories, products = catalog.items, message = "")
            } catch (error: Exception) {
                _state.value = _state.value.copy(loading = false, refreshing = false, message = error.message ?: "Could not sync the store")
            }
        }
    }

    fun selectTab(tab: StoreTab) { _state.value = _state.value.copy(selectedTab = tab) }
    fun search(value: String) { _state.value = _state.value.copy(search = value) }
    fun applySearch() { refresh() }
    fun chooseCategory(category: String) { _state.value = _state.value.copy(category = category); refresh() }
    fun add(product: StoreProduct) { val next = (_state.value.cart[product.id] ?: 0) + 1; _state.value = _state.value.copy(cart = _state.value.cart + (product.id to next)) }
    fun remove(product: StoreProduct) { val current = _state.value.cart[product.id] ?: return; val next = current - 1; _state.value = _state.value.copy(cart = if (next > 0) _state.value.cart + (product.id to next) else _state.value.cart - product.id) }
    fun cartLines(): List<CartLine> = _state.value.products.mapNotNull { product -> _state.value.cart[product.id]?.let { CartLine(product, it) } }

    class Factory(private val siteUrl: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = StoreViewModel(StoreApi(siteUrl)) as T
    }
}
