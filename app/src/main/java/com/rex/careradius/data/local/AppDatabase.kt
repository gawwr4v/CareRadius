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
    version = 3,
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add geofenceName column to visits for preserving history after deletion
                database.execSQL("ALTER TABLE visits ADD COLUMN geofenceName TEXT NOT NULL DEFAULT 'Unknown'")
                
                // Update existing visits with geofence names from the geofences table
                database.execSQL("""
                    UPDATE visits 
                    SET geofenceName = COALESCE(
                        (SELECT name FROM geofences WHERE geofences.id = visits.geofenceId),
                        'Unknown'
                    )
                """)
                
                // Create new table with nullable geofenceId and SET NULL foreign key
                database.execSQL("""
                    CREATE TABLE visits_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        geofenceId INTEGER,
                        geofenceName TEXT NOT NULL,
                        entryTime INTEGER NOT NULL,
                        exitTime INTEGER,
                        durationMillis INTEGER,
                        FOREIGN KEY (geofenceId) REFERENCES geofences(id) ON DELETE SET NULL
                    )
                """)
                
                // Copy data from old table
                database.execSQL("""
                    INSERT INTO visits_new (id, geofenceId, geofenceName, entryTime, exitTime, durationMillis)
                    SELECT id, geofenceId, geofenceName, entryTime, exitTime, durationMillis FROM visits
                """)
                
                // Drop old table and rename new one
                database.execSQL("DROP TABLE visits")
                database.execSQL("ALTER TABLE visits_new RENAME TO visits")
                
                // Recreate index
                database.execSQL("CREATE INDEX index_visits_geofenceId ON visits(geofenceId)")
            }
        }
    }
}
