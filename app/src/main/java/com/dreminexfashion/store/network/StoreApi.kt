package com.dreminexfashion.store.network

import com.dreminexfashion.store.data.CatalogResponse
import com.dreminexfashion.store.data.StoreCategory
import com.dreminexfashion.store.data.StoreManifest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.math.BigDecimal

class StoreApi(siteUrl: String, private val client: OkHttpClient = OkHttpClient()) {
    private val root = siteUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun manifest(): StoreManifest = try {
        request("$root/wp-json/dreminex/v1/store-app/manifest".toHttpUrl(), StoreManifest.serializer())
    } catch (_: IOException) {
        StoreManifest(site_url = root, site_name = root.removePrefix("https://").removePrefix("http://"))
    }

    fun categories(): List<StoreCategory> = try {
        request("$root/wp-json/dreminex/v1/store-app/categories".toHttpUrl(), ListSerializer(StoreCategory.serializer()))
    } catch (_: IOException) {
        standardCategories()
    }
    fun catalog(page: Int = 1, search: String = "", category: String = ""): CatalogResponse {
        val url = "$root/wp-json/dreminex/v1/store-app/catalog".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString()).addQueryParameter("per_page", "24")
            .apply { if (search.isNotBlank()) addQueryParameter("search", search); if (category.isNotBlank()) addQueryParameter("category", category) }
            .build()
        return try {
            request(url, CatalogResponse.serializer())
        } catch (_: IOException) {
            standardCatalog(page, search, category)
        }
    }

    private fun standardCategories(): List<StoreCategory> {
        val url = "$root/wp-json/wc/store/v1/products/categories".toHttpUrl().newBuilder()
            .addQueryParameter("per_page", "100").build()
        return json.parseToJsonElement(requestBody(url)).jsonArray.map { value ->
            val item = value.jsonObject
            StoreCategory(
                id = item.int("id"), name = item.string("name"), slug = item.string("slug"),
                count = item.int("count"), image = item.jsonObjectOrNull("image")?.string("src") ?: "",
            )
        }
    }

    private fun standardCatalog(page: Int, search: String, category: String): CatalogResponse {
        val builder = "$root/wp-json/wc/store/v1/products".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString()).addQueryParameter("per_page", "24")
        if (search.isNotBlank()) builder.addQueryParameter("search", search)
        if (category.isNotBlank()) builder.addQueryParameter("category", category)
        val items = json.parseToJsonElement(requestBody(builder.build())).jsonArray.map { value ->
            val item = value.jsonObject
            val prices = item.jsonObjectOrNull("prices")
            val categories = item.arrayOrEmpty("categories").map { categoryItem ->
                val categoryObject = categoryItem.jsonObject
                com.dreminexfashion.store.data.ProductCategory(
                    id = categoryObject.int("id"), name = categoryObject.string("name"), slug = categoryObject.string("slug"),
                )
            }
            val currency = prices?.string("currency_code") ?: "BDT"
            val minorUnit = prices?.int("currency_minor_unit") ?: 2
            com.dreminexfashion.store.data.StoreProduct(
                id = item.int("id"), name = item.string("name"), slug = item.string("slug"), permalink = item.string("permalink"),
                description = item.string("short_description").ifBlank { item.string("description") },
                images = item.arrayOrEmpty("images").mapNotNull { it.jsonObject.stringOrNull("src") },
                categories = categories, type = item.string("type"), sku = item.string("sku"), currency = currency,
                price = prices?.minorPrice("price", minorUnit) ?: "", regular_price = prices?.minorPrice("regular_price", minorUnit) ?: "",
                sale_price = prices?.minorPrice("sale_price", minorUnit) ?: "", on_sale = item.boolean("on_sale"),
                stock_status = item.string("low_stock_remaining").ifBlank { if (item.boolean("is_in_stock")) "instock" else "outofstock" },
                in_stock = item.boolean("is_in_stock"), average_rating = item.string("average_rating"), review_count = item.int("review_count"),
                modified = item.string("date_modified_gmt"),
            )
        }
        return CatalogResponse(items = items, page = page, per_page = 24, total = items.size, pages = page)
    }

    private fun <T> request(url: okhttp3.HttpUrl, serializer: kotlinx.serialization.KSerializer<T>): T {
        return json.decodeFromString(serializer, requestBody(url))
    }

    private fun requestBody(url: okhttp3.HttpUrl): String {
        val response = client.newCall(Request.Builder().url(url).header("Accept", "application/json").get().build()).execute()
        response.use {
            if (!it.isSuccessful) throw IOException("Store API returned HTTP ${it.code}")
            return it.body?.string().orEmpty()
        }
    }
}

private fun JsonObject.string(key: String): String = this[key]?.jsonPrimitive?.content.orEmpty()
private fun JsonObject.stringOrNull(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.int(key: String): Int = this[key]?.jsonPrimitive?.intOrNull ?: 0
private fun JsonObject.boolean(key: String): Boolean = this[key]?.jsonPrimitive?.booleanOrNull ?: false
private fun JsonObject.jsonObjectOrNull(key: String): JsonObject? = this[key]?.jsonObject
private fun JsonObject.arrayOrEmpty(key: String) = this[key]?.jsonArray.orEmpty()
private fun JsonObject.minorPrice(key: String, minorUnit: Int): String {
    val raw = string(key)
    if (raw.isBlank()) return ""
    return try { BigDecimal(raw).movePointLeft(minorUnit).stripTrailingZeros().toPlainString() } catch (_: NumberFormatException) { "" }
}
