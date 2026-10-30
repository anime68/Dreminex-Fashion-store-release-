package com.dreminexfashion.store.data

import java.text.NumberFormat
import java.util.Locale

object PriceFormatting {
    fun amount(raw: String): Double? = raw.trim().replace(",", "").toDoubleOrNull()

    fun display(product: StoreProduct): String {
        val value = amount(product.sale_price).takeIf { product.on_sale } ?: amount(product.price) ?: amount(product.regular_price)
        if (value == null) return product.price.ifBlank { product.regular_price }
        return NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2; minimumFractionDigits = 0 }.format(value) + " " + product.currency
    }

    fun regularCrossed(product: StoreProduct): String? {
        val regular = amount(product.regular_price) ?: return null
        val current = amount(product.sale_price).takeIf { product.on_sale } ?: amount(product.price) ?: return null
        return regular.takeIf { it > current }?.let { NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2 }.format(it) + " " + product.currency }
    }
}
