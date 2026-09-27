package ir.atom313.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CacheDao {
    @Query("SELECT * FROM api_cache WHERE `key` = :key")
    suspend fun get(key: String): CacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: CacheEntity)

    @Query("DELETE FROM api_cache WHERE personal = 1")
    suspend fun clearPersonal()

    @Query("DELETE FROM api_cache WHERE updatedAt < :before")
    suspend fun prune(before: Long)
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY addedAt ASC")
    fun observe(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items ORDER BY addedAt ASC")
    suspend fun all(): List<CartItemEntity>

    @Query("SELECT * FROM cart_items WHERE productId = :id")
    suspend fun get(id: Long): CartItemEntity?

    @Upsert
    suspend fun upsert(item: CartItemEntity)

    @Query("UPDATE cart_items SET qty = :qty WHERE productId = :id")
    suspend fun setQty(id: Long, qty: Int)

    @Query("DELETE FROM cart_items WHERE productId = :id")
    suspend fun remove(id: Long)

    @Query("DELETE FROM cart_items")
    suspend fun clear()
}
