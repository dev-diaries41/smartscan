package com.fpf.smartscan.data.tags

/**
 * Přednastavené tagy pro rychlý start
 *
 * Obsahuje doporučené tagy s optimalizovanými popisy pro CLIP model
 */
object PresetTags {
    val RECOMMENDED = listOf(
        TagPreset(
            name = "Rekonstrukce domu",
            description = "fotografie stavebních prací, renovace a rekonstrukce interiéru nebo exteriéru domu, viditelné nástroje, materiály, rozbitá místa, nedokončená stavba",
            threshold = 0.35f,
            color = 0xFFFF9800.toInt() // Oranžová
        ),
        TagPreset(
            name = "Děti",
            description = "fotografie dětí, batolat, školáků při hře, učení nebo jiných aktivitách, dětské portréty a rodinné fotky s dětmi",
            threshold = 0.40f,
            color = 0xFF4CAF50.toInt() // Zelená
        ),
        TagPreset(
            name = "Screenshoty",
            description = "screenshot mobilní aplikace, snímek obrazovky telefonu nebo počítače s viditelným uživatelským rozhraním, tlačítky, menu, textem a notifikacemi",
            threshold = 0.38f,
            color = 0xFF607D8B.toInt() // Šedá
        ),
        TagPreset(
            name = "Selfie",
            description = "selfie fotografie pořízená z ruky s natočenou kamerou k sobě, autoportrét, zrcadlové selfie, typický selfie úhel",
            threshold = 0.42f,
            color = 0xFF9C27B0.toInt() // Fialová
        ),
        TagPreset(
            name = "Explicitní obsah",
            description = "fotografie nahého těla, intimních částí, erotického nebo pornografického obsahu, sexuálně explicitní materiál",
            threshold = 0.45f,
            color = 0xFFF44336.toInt() // Červená
        ),
        TagPreset(
            name = "Dokumenty",
            description = "naskenovaný dokument, text na papíře, účtenka, faktura, smlouva, formulář, oficiální dokument",
            threshold = 0.38f,
            color = 0xFF2196F3.toInt() // Modrá
        ),
        TagPreset(
            name = "Jídlo",
            description = "fotografie jídla na talíři, v restauraci nebo při vaření, snídaně, oběd, večeře, dezert, nápoje",
            threshold = 0.40f,
            color = 0xFFFF5722.toInt() // Tmavě oranžová
        ),
        TagPreset(
            name = "Příroda",
            description = "krajina s přírodou, stromy, lesy, hory, řeky, jezera, západ slunce, obloha, příroda bez lidí",
            threshold = 0.35f,
            color = 0xFF8BC34A.toInt() // Světle zelená
        ),
        TagPreset(
            name = "Domácí mazlíčci",
            description = "fotografie domácích zvířat jako psi, kočky, králíci, ptáci, domácí mazlíčci",
            threshold = 0.40f,
            color = 0xFF795548.toInt() // Hnědá
        ),
        TagPreset(
            name = "Auta a doprava",
            description = "fotografie automobilů, motocyklů, vozidel, dopravních prostředků, auta na silnici nebo zaparkovaná",
            threshold = 0.35f,
            color = 0xFF000000.toInt() // Černá
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
