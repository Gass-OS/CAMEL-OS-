package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.EuropassCv
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "europass_cvs")
data class CvEntity(
    @PrimaryKey val id: String,
    val title: String,
    val fullName: String,
    val templateId: String,
    val payloadJson: String,
    val updatedAt: Long
)

@Dao
interface CvDao {
    @Query("SELECT * FROM europass_cvs ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CvEntity>>

    @Query("SELECT * FROM europass_cvs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CvEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CvEntity)

    @Query("DELETE FROM europass_cvs WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Database(entities = [CvEntity::class], version = 1, exportSchema = false)
abstract class EuropassDatabase : RoomDatabase() {
    abstract fun cvDao(): CvDao

    companion object {
        @Volatile
        private var INSTANCE: EuropassDatabase? = null

        fun getInstance(context: Context): EuropassDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EuropassDatabase::class.java,
                    "europass_cv_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class CvRepository(private val dao: CvDao) {
    val allCvs: Flow<List<EuropassCv>> = dao.observeAll().map { entities ->
        entities.mapNotNull { entity ->
            runCatching { CvJsonCodec.fromJson(entity.payloadJson) }.getOrNull()
        }
    }

    suspend fun saveCv(cv: EuropassCv): EuropassCv {
        val stamped = cv.copy(updatedAt = System.currentTimeMillis())
        val entity = CvEntity(
            id = stamped.id,
            title = stamped.title.ifBlank { "${stamped.fullName} CV" },
            fullName = stamped.fullName,
            templateId = stamped.templateId,
            payloadJson = CvJsonCodec.toJson(stamped),
            updatedAt = stamped.updatedAt
        )
        dao.upsert(entity)
        return stamped
    }

    suspend fun deleteCv(id: String) {
        dao.deleteById(id)
    }

    suspend fun getCv(id: String): EuropassCv? {
        return dao.getById(id)?.let { runCatching { CvJsonCodec.fromJson(it.payloadJson) }.getOrNull() }
    }
}
