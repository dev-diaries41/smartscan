package com.fpf.smartscan.data.tags

/**
 * Přednastavené tagy pro rychlý start
 *
 * Obsahuje doporučené tagy s optimalizovanými popisy pro CLIP model
 */
object PresetTags {
    val RECOMMENDED = listOf(
        // Rekonstrukce - upravený popis
        TagPreset(
            name = "Rekonstrukce",
            description = "house renovation photos, construction work in progress, floor repairs, wall repairs, excavation work, plumbing installation, electrical wiring installation, unfinished construction, building materials, construction tools on site",
            threshold = 0.20f,
            color = 0xFFFF9800.toInt() // Oranžová
        ),

        // Děti
        TagPreset(
            name = "Děti",
            description = "children in photos, kids playing, toddlers, babies, school children, children portraits, family photos with children, kids activities",
            threshold = 0.20f,
            color = 0xFF4CAF50.toInt() // Zelená
        ),

        // Art - kreslené obrázky a obrazy
        TagPreset(
            name = "Art",
            description = "paintings, drawn pictures, artwork, illustrations, artistic drawings, canvas art, digital art, hand-drawn images, sketches, artistic compositions",
            threshold = 0.20f,
            color = 0xFF673AB7.toInt() // Deep Purple
        ),

        // Selfie
        TagPreset(
            name = "Selfie",
            description = "selfie photo taken with front camera, self-portrait, mirror selfie, typical selfie angle, person taking photo of themselves",
            threshold = 0.20f,
            color = 0xFFFF6F00.toInt() // Tmavě oranžová
        ),

        // Screenshoty
        TagPreset(
            name = "Screenshots",
            description = "phone screenshot, computer screen capture, mobile app interface, visible UI elements, buttons, menus, text notifications, screen recording",
            threshold = 0.20f,
            color = 0xFF607D8B.toInt() // Šedá
        ),

        // Dokumenty
        TagPreset(
            name = "Dokumenty",
            description = "scanned documents, text on paper, receipts, invoices, contracts, forms, official documents, papers with text",
            threshold = 0.20f,
            color = 0xFF2196F3.toInt() // Modrá
        ),

        // Jídlo
        TagPreset(
            name = "Jídlo",
            description = "food on plate, restaurant food, cooking, breakfast, lunch, dinner, desserts, beverages, drinks, meals",
            threshold = 0.20f,
            color = 0xFFFF5722.toInt() // Deep Orange
        ),

        // Příroda
        TagPreset(
            name = "Příroda",
            description = "natural landscape, trees, forests, mountains, rivers, lakes, sunset, sky, nature without people, wilderness",
            threshold = 0.20f,
            color = 0xFF8BC34A.toInt() // Světle zelená
        ),

        // Domácí mazlíčci
        TagPreset(
            name = "Domácí mazlíčci",
            description = "pets, dogs, cats, rabbits, birds, domestic animals, pet photos, animals at home",
            threshold = 0.20f,
            color = 0xFF795548.toInt() // Hnědá
        ),

        // Auta a doprava
        TagPreset(
            name = "Auta",
            description = "cars, motorcycles, vehicles, automobiles, transportation, cars on road, parked cars, car exteriors and interiors",
            threshold = 0.20f,
            color = 0xFF424242.toInt() // Tmavě šedá
        ),

        // Cestování
        TagPreset(
            name = "Cestování",
            description = "travel photos, vacation pictures, tourist attractions, airports, hotels, foreign cities, landmarks, tourism",
            threshold = 0.20f,
            color = 0xFF00BCD4.toInt() // Cyan
        ),

        // Oslavy a události
        TagPreset(
            name = "Oslavy",
            description = "celebrations, parties, birthdays, weddings, festive events, gatherings, special occasions, party decorations",
            threshold = 0.20f,
            color = 0xFFFFEB3B.toInt() // Žlutá
        ),

        // Sport a fitness
        TagPreset(
            name = "Sport",
            description = "sports activities, gym workout, fitness, running, cycling, sports events, athletic activities, exercise",
            threshold = 0.20f,
            color = 0xFF009688.toInt() // Teal
        ),

        // Memes a humor
        TagPreset(
            name = "Memes",
            description = "internet memes, funny pictures, humorous images, viral memes, meme templates, comedy images, joke pictures",
            threshold = 0.20f,
            color = 0xFFCDDC39.toInt() // Lime
        )
    )
}

/**
 * Data class pro preset tag
 *
 * @param name Název tagu
 * @param description Popis pro CLIP embedding
 * @param threshold Doporučený práh (0.0-1.0)
 * @param color Barva v hex formátu (ARGB)
 */
data class TagPreset(
    val name: String,
    val description: String,
    val threshold: Float = 0.30f,
    val color: Int = 0xFF2196F3.toInt()
)
