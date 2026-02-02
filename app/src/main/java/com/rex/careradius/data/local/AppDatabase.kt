package com.rex.careradius.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rex.careradius.data.local.dao.GeofenceDao
import com.rex.careradius.data.local.dao.VisitDao
import com.rex.careradius.data.local.entity.GeofenceEntity
import com.rex.careradius.data.local.entity.VisitEntity

/**
 * Room Database for CareRadius app
 * Contains Geofences and Visits tables
 */
@Database(
    entities = [GeofenceEntity::class, VisitEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun geofenceDao(): GeofenceDao
    abstract fun visitDao(): VisitDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "careradius_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add icon column with default emoji
                database.execSQL("ALTER TABLE geofences ADD COLUMN icon TEXT NOT NULL DEFAULT '📍'")
            }
        }
    }
}
