package com.example.openglow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.openglow.data.entity.PersonalizationRuleEntity

@Dao
interface PersonalizationRuleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRule(rule: PersonalizationRuleEntity): Long

    @Update
    suspend fun updateRule(rule: PersonalizationRuleEntity)

    @Query("SELECT * FROM personalization_rules ORDER BY updatedAt DESC")
    suspend fun getAllRules(): List<PersonalizationRuleEntity>

    @Query("SELECT * FROM personalization_rules WHERE ruleType = :ruleType AND pattern = :pattern LIMIT 1")
    suspend fun getRule(ruleType: String, pattern: String): PersonalizationRuleEntity?
}
