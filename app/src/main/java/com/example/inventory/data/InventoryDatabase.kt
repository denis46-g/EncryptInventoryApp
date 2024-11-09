package com.example.inventory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.commonsware.cwac.saferoom.SQLCipherUtils.encrypt
import com.commonsware.cwac.saferoom.SQLCipherUtils.getDatabaseState
import com.example.inventory.KeyStoreHelper
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

/**
 * Database class with a singleton Instance object.
 */
@Database(entities = [Item::class], version = 1, exportSchema = false)
abstract class InventoryDatabase: RoomDatabase() {

    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        private var Instance: InventoryDatabase? = null

        fun getDatabase(context: Context): InventoryDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                var key = KeyStoreHelper.getKey()
                if(key == null){
                    KeyStoreHelper.createKey()
                    key = KeyStoreHelper.getKey()
                }
                val stringKey = key.toString()

                val passphrase: ByteArray = SQLiteDatabase.getBytes(stringKey.toCharArray())

                val state = getDatabaseState(context, "item_database_cipher")

                if (state.toString() == "UNENCRYPTED"){
                    encrypt(context, "item_database_cipher", passphrase)
                }

                val factory = SupportFactory(passphrase)

                Room.databaseBuilder(context, InventoryDatabase::class.java, "item_database_cipher")
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}