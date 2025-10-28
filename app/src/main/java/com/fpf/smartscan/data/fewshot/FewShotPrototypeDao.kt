package com.fpf.smartscan.data.fewshot

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO pro Few-Shot Prototypes
 *
 * Poskytuje operace pro CRUD few-shot prototypes (průměrné embeddingy).
 */
@Dao
interface FewShotPrototypeDao {

    /**
     * Získá všechny prototypes seřazené podle posledního updatu (nejnovější první)
     *
     * @return Flow s listem prototypes
     */
    @Query("SELECT * FROM few_shot_prototypes ORDER BY updatedAt DESC")
    fun getAllPrototypes(): Flow<List<FewShotPrototypeEntity>>

    /**
     * Získá všechny prototypes seřazené podle jména
     *
     * @return Flow s listem prototypes
     */
    @Query("SELECT * FROM few_shot_prototypes ORDER BY name ASC")
    fun getAllPrototypesByName(): Flow<List<FewShotPrototypeEntity>>

    /**
     * Získá všechny prototypes pro danou kategorii
     *
     * @param category Kategorie ("person", "object", "scene", "style")
     * @return Flow s listem prototypes
     */
    @Query("SELECT * FROM few_shot_prototypes WHERE category = :category ORDER BY updatedAt DESC")
    fun getPrototypesByCategory(category: String): Flow<List<FewShotPrototypeEntity>>

    /**
     * Získá prototype podle ID
     *
     * @param id ID prototypu
     * @return Prototype nebo null pokud neexistuje
     */
    @Query("SELECT * FROM few_shot_prototypes WHERE id = :id")
    suspend fun getPrototypeById(id: Long): FewShotPrototypeEntity?

    /**
     * Získá prototype podle jména
     *
     * @param name Název prototypu
     * @return Prototype nebo null pokud neexistuje
     */
    @Query("SELECT * FROM few_shot_prototypes WHERE name = :name LIMIT 1")
    suspend fun getPrototypeByName(name: String): FewShotPrototypeEntity?

    /**
     * Vyhledá prototypes podle query v názvu nebo popisu
     *
     * @param query Search query (wildcard search)
     * @return List prototypes které matchují query
     */
    @Query("""
        SELECT * FROM few_shot_prototypes
        WHERE name LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    suspend fun searchPrototypes(query: String): List<FewShotPrototypeEntity>

    /**
     * Vloží nový prototype
     *
     * @param prototype Prototype k vložení
     * @return ID vloženého prototypu
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrototype(prototype: FewShotPrototypeEntity): Long

    /**
     * Aktualizuje existující prototype
     *
     * @param prototype Prototype k update
     */
    @Update
    suspend fun updatePrototype(prototype: FewShotPrototypeEntity)

    /**
     * Smaže prototype
     * Cascade delete automaticky smaže všechny samples
     *
     * @param prototype Prototype ke smazání
     */
    @Delete
    suspend fun deletePrototype(prototype: FewShotPrototypeEntity)

    /**
     * Smaže prototype podle ID
     * Cascade delete automaticky smaže všechny samples
     *
     * @param id ID prototypu ke smazání
     */
    @Query("DELETE FROM few_shot_prototypes WHERE id = :id")
    suspend fun deletePrototypeById(id: Long)

    /**
     * Získá počet všech prototypes
     *
     * @return Počet prototypes
     */
    @Query("SELECT COUNT(*) FROM few_shot_prototypes")
    suspend fun getPrototypeCount(): Int

    /**
     * Získá počet prototypes pro danou kategorii
     *
     * @param category Kategorie
     * @return Počet prototypes v kategorii
     */
    @Query("SELECT COUNT(*) FROM few_shot_prototypes WHERE category = :category")
    suspend fun getPrototypeCountByCategory(category: String): Int
}
