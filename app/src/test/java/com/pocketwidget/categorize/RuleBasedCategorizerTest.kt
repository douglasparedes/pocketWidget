package com.pocketwidget.categorize

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuleBasedCategorizerTest {

    private val categorizer = RuleBasedCategorizer()

    @Test
    fun matchesTransportKeywords() = runBlocking {
        assertEquals("transport", categorizer.suggest("Uber ride home", 1200))
        assertEquals("transport", categorizer.suggest("Metro top-up", 275))
    }

    @Test
    fun matchesFoodAndCoffeeKeywords() = runBlocking {
        assertEquals("coffee", categorizer.suggest("Starbucks flat white", 380))
        assertEquals("food", categorizer.suggest("Ramen with Anna", 1850))
    }

    @Test
    fun isCaseInsensitive() = runBlocking {
        assertEquals("groceries", categorizer.suggest("LIDL weekly shop", 4200))
    }

    @Test
    fun returnsNullWhenUnsure() = runBlocking {
        assertNull(categorizer.suggest("", 100))
        assertNull(categorizer.suggest("asdf qwer", 100))
    }
}
