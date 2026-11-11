package com.pocketwidget.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    /** ARGB color used for the category accent. */
    val color: Long,
    val sortOrder: Int,
    val archived: Boolean = false,
)

object DefaultCategories {

    const val FALLBACK_ID = "other"

    val ALL: List<Category> = listOf(
        Category("food", "Food", "🍜", 0xFFE76F51, 0),
        Category("coffee", "Coffee", "☕", 0xFFB08968, 1),
        Category("groceries", "Groceries", "🧺", 0xFF7A9E5A, 2),
        Category("transport", "Transport", "🚇", 0xFF2A9D8F, 3),
        Category("home", "Home", "🏡", 0xFF8E7DBE, 4),
        Category("fun", "Fun", "🎟️", 0xFFE9C46A, 5),
        Category("health", "Health", "💊", 0xFF4C9F70, 6),
        Category("shopping", "Shopping", "🛍️", 0xFFD1495B, 7),
        Category("bills", "Bills", "🧾", 0xFF5A6ACF, 8),
        Category(FALLBACK_ID, "Other", "🫧", 0xFF9A8C98, 9),
    )

    val EMOJI_CHOICES: List<String> = listOf(
        "🍜", "☕", "🧺", "🚇", "🏡", "🎟️", "💊", "🛍️", "🧾", "🫧",
        "🚲", "🎧", "📚", "🐾", "🎁", "✈️", "🏋️", "🍺", "🍰", "🧴",
    )

    val COLOR_CHOICES: List<Long> = listOf(
        0xFFE76F51, 0xFF2A9D8F, 0xFF7A9E5A, 0xFF8E7DBE, 0xFFE9C46A,
        0xFFD1495B, 0xFF5A6ACF, 0xFFB08968, 0xFF9A8C98,
    )
}
