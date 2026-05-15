package com.fakka.cash

data class Product(
    val id: String,
    val price: Double,
    val category: Category,
    val label: String
)

enum class Category { OLD, NEW, MARED }

object Products {
    private val newIds = setOf(
        "Fakka_6_NewUnite",
        "Fakka_10_NewUnite",
        "Fakka_15_NewUnite",
        "Fakka_19.5_NewUnite",
        "Fakka_26_Unite"
    )

    private val fakkaIds = listOf(
        "Fakka_2.5_Unite", "Fakka_4.25_Unite", "Fakka_5_Unite", "Fakka_6_NewUnite",
        "Fakka_7_Unite", "Fakka_9_Unite", "Fakka_10_Unite", "Fakka_10_NewUnite",
        "Fakka_10.5_Unite", "Fakka_11.5_Unite", "Fakka_12_Unite", "Fakka_12.5_Unite",
        "Fakka_13_Unite", "Fakka_13.5_Unite", "Fakka_15_Unite", "Fakka_15_NewUnite",
        "Fakka_15.5_Unite", "Fakka_16.5_Unite", "Fakka_17.5_Unite", "Fakka_19.5_NewUnite",
        "Fakka_20_Unite", "Fakka_26_Unite"
    )

    private val maredMap = mapOf(
        "Mared_10_Minuts" to "مارد 10 دقايق",
        "Mared_10_Flexs" to "مارد 10 فليكس",
        "Mared_10_Social" to "مارد 10 سوشيال"
    )

    val fakka: List<Product> = fakkaIds.map { id ->
        val price = Regex("Fakka_([\\d.]+)_").find(id)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        Product(
            id = id,
            price = price,
            category = if (newIds.contains(id)) Category.NEW else Category.OLD,
            label = "كارت ${if (price % 1.0 == 0.0) price.toInt().toString() else price.toString()} جنيه"
        )
    }

    val mared: List<Product> = maredMap.map { (id, label) ->
        Product(id = id, price = 10.0, category = Category.MARED, label = label)
    }

    val newCards = fakka.filter { it.category == Category.NEW }
    val oldCards = fakka.filter { it.category == Category.OLD }
    val all = fakka + mared
}
