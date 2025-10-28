package com.fpf.smartscan.data.fewshot

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO pro Few-Shot Samples
 *
 * Poskytuje operace pro CRUD few-shot samples (jednotlivé příklady použité pro vytvoření prototype).
 */
@Dao
interface FewShotSampleDao {

    /**
     * Získá všechny samples pro daný prototype (seřazené podle data přidání)
     *
     * @param prototypeId ID prototypu
     * @return Flow s listem samples
     */
    @Query("SELECT * FROM few_shot_samples WHERE prototypeId = :prototypeId ORDER BY addedAt ASC")
    fun getSamplesForPrototype(prototypeId: Long): Flow<List<FewShotSampleEntity>>

    /**
     * Získá všechny samples pro daný prototype (synchronní)
     *
     * @param prototypeId ID prototypu
     * @return List samples
     */
    @Query("SELECT * FROM few_shot_samples WHERE prototypeId = :prototypeId ORDER BY addedAt ASC")
    suspend fun getSamplesForPrototypeSync(prototypeId: Long): List<FewShotSampleEntity>

    /**
     * Získá sample podle ID
     *
     * @param id ID samplu
     * @return Sample nebo null pokud neexistuje
     */
    @Query("SELECT * FROM few_shot_samples WHERE id = :id")
    suspend fun getSampleById(id: Long): FewShotSampleEntity?

    /**
     * Získá počet samplu pro daný prototype
     *
     * @param prototypeId ID prototypu
     * @return Počet samples
     */
    @Query("SELECT COUNT(*) FROM few_shot_samples WHERE prototypeId = :prototypeId")
    suspend fun getSampleCount(prototypeId: Long): Int

    /**
     * Vloží nový sample
     *
     * @param sample Sample k vložení
     * @return ID vloženého samplu
     */
    @Insert
    suspend fun insertSample(sample: FewShotSampleEntity): Long

    /**
     * Vloží více samplu najednou
     *
     * @param samples List samplu k vložení
     * @return List ID vložených samplu
     */
    @Insert
    suspend fun insertSamples(samples: List<FewShotSampleEntity>): List<Long>

    /**
     * Aktualizuje existující sample
     *
     * @param sample Sample k update
     */
    @Update
    suspend fun updateSample(sample: FewShotSampleEntity)

    /**
     * Smaže sample
     *
     * @param sample Sample ke smazání
     */
    @Delete
    suspend fun deleteSample(sample: FewShotSampleEntity)

    /**
     * Smaže sample podle ID
     *
     * @param id ID samplu ke smazání
     */
    @Query("DELETE FROM few_shot_samples WHERE id = :id")
    suspend fun deleteSampleById(id: Long)

    /**
     * Smaže všechny samples pro daný prototype
     * (Cascade delete to udělá automaticky při smazání prototype, ale může se hodit samostatně)
     *
     * @param prototypeId ID prototypu
     */
    @Query("DELETE FROM few_shot_samples WHERE prototypeId = :prototypeId")
    suspend fun deleteAllSamplesForPrototype(prototypeId: Long)

    /**
     * Získá poslední N samplu (napříč všemi prototypes)
     * Používá se např. pro "Recent samples" widget
     *
     * @param limit Maximální počet samplu k vrácení
     * @return List samples
     */
    @Query("SELECT * FROM few_shot_samples ORDER BY addedAt DESC LIMIT :limit")
    suspend fun getRecentSamples(limit: Int): List<FewShotSampleEntity>
}
