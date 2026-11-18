package com.novapulse.launcher.interfaces

import androidx.room.*
import com.novapulse.launcher.models.HiddenIcon

@Dao
interface HiddenIconsDao {
    @Query("SELECT * FROM hidden_icons")
    fun getHiddenIcons(): List<HiddenIcon>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(hiddenIcon: HiddenIcon): Long

    @Delete
    fun removeHiddenIcons(icons: List<HiddenIcon>)
}
