package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TrustedHostEntity::class,
        ControllerAuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SalimDatabase : RoomDatabase() {
    abstract fun trustedHostDao(): TrustedHostDao
    abstract fun auditLogDao(): ControllerAuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: SalimDatabase? = null

        fun getDatabase(context: Context): SalimDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SalimDatabase::class.java,
                    "salim_controller_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
