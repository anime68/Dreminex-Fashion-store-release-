package com.dreminexfashion.store.network

import com.dreminexfashion.store.data.CatalogResponse
import com.dreminexfashion.store.data.StoreCategory
import com.dreminexfashion.store.data.StoreManifest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class StoreApi(siteUrl: String, private val client: OkHttpClient = OkHttpClient()) {
    private val root = siteUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun manifest(): StoreManifest = request("$root/wp-json/dreminex/v1/store-app/manifest".toHttpUrl(), StoreManifest.serializer())
    fun categories(): List<StoreCategory> = request("$root/wp-json/dreminex/v1/store-app/categories".toHttpUrl(), ListSerializer(StoreCategory.serializer()))
    fun catalog(page: Int = 1, search: String = "", category: String = ""): CatalogResponse {
        val url = "$root/wp-json/dreminex/v1/store-app/catalog".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString()).addQueryParameter("per_page", "24")
            .apply { if (search.isNotBlank()) addQueryParameter("search", search); if (category.isNotBlank()) addQueryParameter("category", category) }
            .build()
        return request(url, CatalogResponse.serializer())
    }

    private fun <T> request(url: okhttp3.HttpUrl, serializer: kotlinx.serialization.KSerializer<T>): T {
        val response = client.newCall(Request.Builder().url(url).header("Accept", "application/json").get().build()).execute()
        response.use {
            if (!it.isSuccessful) throw IOException("Store API returned HTTP ${it.code}")
            return json.decodeFromString(serializer, it.body?.string().orEmpty())
        }
    }
}
