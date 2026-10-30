package com.dreminexfashion.store.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceFormattingTest {
    @Test fun salePriceUsesSaleAndCrossesRegular() {
        val product = StoreProduct(price = "679", regular_price = "999", sale_price = "679", on_sale = true, currency = "BDT")
        assertEquals("679 BDT", PriceFormatting.display(product))
        assertEquals("999 BDT", PriceFormatting.regularCrossed(product))
    }

    @Test fun regularPriceDoesNotCreateCrossedValue() {
        val product = StoreProduct(price = "999", regular_price = "999", currency = "BDT")
        assertEquals("999 BDT", PriceFormatting.display(product))
        assertNull(PriceFormatting.regularCrossed(product))
    }
}
