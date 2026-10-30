package com.dreminexfashion.store.data

import kotlinx.serialization.Serializable

@Serializable
data class StoreManifest(
    val site_id: String = "",
    val site_url: String = "",
    val site_name: String = "Dreminex Fashion",
    val branding: Branding = Branding(),
    val features: Features = Features(),
    val store_app: StoreRelease = StoreRelease(),
    val server_time: String = "",
)

@Serializable
data class Branding(
    val name: String = "Dreminex Fashion",
    val short_name: String = "Dreminex",
    val logo_url: String = "",
    val theme_color: String = "#08B6D3",
    val background_color: String = "#FFFFFF",
)

@Serializable
data class Features(
    val catalog: Boolean = true,
    val offers: Boolean = true,
    val bundles: Boolean = true,
    val push: Boolean = false,
    val claim_once: Boolean = true,
)

@Serializable
data class StoreRelease(
    val version: String = "",
    val download_url: String = "",
    val sha256: String = "",
    val release_notes_url: String = "",
    val published_at: String = "",
)

@Serializable
data class CatalogResponse(
    val items: List<StoreProduct> = emptyList(),
    val page: Int = 1,
    val per_page: Int = 24,
    val total: Int = 0,
    val pages: Int = 0,
    val modified_at: String = "",
)

@Serializable
data class StoreProduct(
    val id: Int = 0,
    val name: String = "",
    val slug: String = "",
    val permalink: String = "",
    val description: String = "",
    val images: List<String> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val type: String = "simple",
    val sku: String = "",
    val currency: String = "BDT",
    val price: String = "",
    val regular_price: String = "",
    val sale_price: String = "",
    val on_sale: Boolean = false,
    val stock_status: String = "instock",
    val in_stock: Boolean = true,
    val average_rating: String = "0",
    val review_count: Int = 0,
    val modified: String = "",
)

@Serializable
data class ProductCategory(
    val id: Int = 0,
    val name: String = "",
    val slug: String = "",
)

@Serializable
data class StoreCategory(
    val id: Int = 0,
    val name: String = "",
    val slug: String = "",
    val count: Int = 0,
    val image: String = "",
)

data class CartLine(val product: StoreProduct, val quantity: Int)
