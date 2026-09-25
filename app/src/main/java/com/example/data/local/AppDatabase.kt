package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class,
        CategoryEntity::class,
        SupplierEntity::class,
        OrderEntity::class,
        CartItemEntity::class,
        WishlistItemEntity::class,
        ProductResearchEntity::class,
        MarketingCampaignEntity::class,
        AutomationRuleEntity::class,
        ActivityLogEntity::class,
        NotificationEntity::class,
        ProductReviewEntity::class,
        CouponEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun commerceDao(): CommerceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_commerce_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
