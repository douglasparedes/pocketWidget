package com.pocketwidget.categorize

/**
 * Optional local categorization aid. The app deliberately ships no AI and no network:
 * only deterministic keyword rules are implemented.
 */
interface Categorizer {
    /** Returns a category id from `DefaultCategories`, or null when unsure. */
    suspend fun suggest(note: String, amountMinor: Long): String?
}

class RuleBasedCategorizer : Categorizer {

    override suspend fun suggest(note: String, amountMinor: Long): String? {
        val text = note.lowercase()
        if (text.isBlank()) return null
        return RULES.firstOrNull { (keyword, _) -> text.contains(keyword) }?.second
    }

    private companion object {
        /** Order matters: the first matching keyword wins. */
        val RULES: List<Pair<String, String>> = listOf(
            "coffee" to "coffee",
            "espresso" to "coffee",
            "latte" to "coffee",
            "cafe" to "coffee",
            "café" to "coffee",
            "starbucks" to "coffee",
            "bakery" to "food",
            "lunch" to "food",
            "dinner" to "food",
            "breakfast" to "food",
            "restaurant" to "food",
            "pizza" to "food",
            "burger" to "food",
            "ramen" to "food",
            "sushi" to "food",
            "takeaway" to "food",
            "grocer" to "groceries",
            "supermarket" to "groceries",
            "market" to "groceries",
            "aldi" to "groceries",
            "lidl" to "groceries",
            "tesco" to "groceries",
            "uber" to "transport",
            "bolt" to "transport",
            "taxi" to "transport",
            "metro" to "transport",
            "subway" to "transport",
            "bus" to "transport",
            "tram" to "transport",
            "train" to "transport",
            "fuel" to "transport",
            "petrol" to "transport",
            "parking" to "transport",
            "rent" to "home",
            "electric" to "bills",
            "water bill" to "bills",
            "internet" to "bills",
            "phone bill" to "bills",
            "insurance" to "bills",
            "gym" to "health",
            "pharmacy" to "health",
            "doctor" to "health",
            "dentist" to "health",
            "cinema" to "fun",
            "concert" to "fun",
            "game" to "fun",
            "spotify" to "fun",
            "netflix" to "fun",
            "museum" to "fun",
            "amazon" to "shopping",
            "clothes" to "shopping",
            "shoes" to "shopping",
            "book" to "shopping",
        )
    }
}
