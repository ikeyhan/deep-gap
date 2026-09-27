package ir.atom313.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CacheEntity::class, CartItemEntity::class], version = 1, exportSchema = true)
abstract class AtomDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
    abstract fun cartDao(): CartDao
}
