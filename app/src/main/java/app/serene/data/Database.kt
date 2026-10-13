package app.serene.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

enum class PracticeKind { BREATH, MEDITATION, VISUALIZATION, TIMER, AFFIRMATIONS, VISIONS }

/** A finished (or long enough) practice. Counts toward minutes and the streak. */
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val kind: PracticeKind,
    /** Guided id, breathing pattern id, affirmation set id… empty for the plain timer. */
    val ref: String = "",
    val seconds: Int,
)

/** A check-in: how it feels right now, what went well, anything else. */
@Entity(tableName = "journal")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    /** 1 (heavy) .. 5 (great), or null when only gratitude was written. */
    val mood: Int? = null,
    /** One thing per line. */
    val gratitude: String = "",
    val note: String = "",
)

/** Wheel of balance: one score 1..10 per [app.serene.domain.Areas] entry, comma-separated. */
@Entity(tableName = "balance")
data class BalanceCheck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val scores: String,
) {
    fun values(): List<Int> = scores.split(',').mapNotNull { it.trim().toIntOrNull() }
}

@Entity(tableName = "my_affirmations")
data class MyAffirmation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Keys: "a:<setId>:<index>" stock affirmation, "m:<id>" own affirmation, "g:<guidedId>" session. */
@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey val key: String,
    val at: Long = System.currentTimeMillis(),
)

/** A card on the vision board: something the user wants to see come true. */
@Entity(tableName = "visions")
data class Vision(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** What it looks, sounds and feels like, in the user's words. */
    val details: String = "",
    /** A present-tense line to say to oneself, optional. */
    val affirmation: String = "",
    /** File name inside filesDir/visions, or empty for a gradient card. */
    val image: String = "",
    /** Index into the card gradients when there is no image. */
    val hue: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

class Snapshot(
    val sessions: List<Session>,
    val journal: List<JournalEntry>,
    val balance: List<BalanceCheck>,
    val affirmations: List<MyAffirmation>,
    val favorites: List<Favorite>,
    val visions: List<Vision>,
)

@Dao
abstract class SereneDao {
    // Sessions
    @Query("SELECT * FROM sessions ORDER BY at")
    abstract fun observeSessions(): Flow<List<Session>>

    @Query("SELECT * FROM sessions ORDER BY at")
    abstract suspend fun sessions(): List<Session>

    @Insert
    abstract suspend fun insertSession(s: Session): Long

    // Journal
    @Query("SELECT * FROM journal ORDER BY at DESC")
    abstract fun observeJournal(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal WHERE id = :id")
    abstract suspend fun entry(id: Long): JournalEntry?

    @Insert
    abstract suspend fun insertEntry(e: JournalEntry): Long

    @Update
    abstract suspend fun updateEntry(e: JournalEntry)

    @Delete
    abstract suspend fun deleteEntry(e: JournalEntry)

    // Balance
    @Query("SELECT * FROM balance ORDER BY at")
    abstract fun observeBalance(): Flow<List<BalanceCheck>>

    @Insert
    abstract suspend fun insertBalance(b: BalanceCheck): Long

    // Own affirmations
    @Query("SELECT * FROM my_affirmations ORDER BY createdAt")
    abstract fun observeMine(): Flow<List<MyAffirmation>>

    @Query("SELECT * FROM my_affirmations ORDER BY createdAt")
    abstract suspend fun mine(): List<MyAffirmation>

    @Insert
    abstract suspend fun insertMine(a: MyAffirmation): Long

    @Query("UPDATE my_affirmations SET text = :text WHERE id = :id")
    abstract suspend fun updateMine(id: Long, text: String)

    @Query("DELETE FROM my_affirmations WHERE id = :id")
    protected abstract suspend fun deleteMineRow(id: Long)

    @Transaction
    open suspend fun deleteMine(id: Long) {
        deleteMineRow(id)
        unfavorite("m:$id")
    }

    // Favourites
    @Query("SELECT * FROM favorites ORDER BY at DESC")
    abstract fun observeFavorites(): Flow<List<Favorite>>

    @Query("SELECT * FROM favorites ORDER BY at DESC")
    abstract suspend fun favorites(): List<Favorite>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun favorite(f: Favorite)

    @Query("DELETE FROM favorites WHERE `key` = :key")
    abstract suspend fun unfavorite(key: String)

    // Visions
    @Query("SELECT * FROM visions ORDER BY createdAt")
    abstract fun observeVisions(): Flow<List<Vision>>

    @Query("SELECT * FROM visions ORDER BY createdAt")
    abstract suspend fun visions(): List<Vision>

    @Query("SELECT * FROM visions WHERE id = :id")
    abstract suspend fun vision(id: Long): Vision?

    @Insert
    abstract suspend fun insertVision(v: Vision): Long

    @Update
    abstract suspend fun updateVision(v: Vision)

    @Delete
    abstract suspend fun deleteVision(v: Vision)

    // Backup
    @Query("SELECT * FROM journal ORDER BY at")
    protected abstract suspend fun journalAll(): List<JournalEntry>

    @Query("SELECT * FROM balance ORDER BY at")
    protected abstract suspend fun balanceAll(): List<BalanceCheck>

    @Transaction
    open suspend fun snapshot() = Snapshot(sessions(), journalAll(), balanceAll(), mine(), favorites(), visions())

    @Query("DELETE FROM sessions") protected abstract suspend fun clearSessions()
    @Query("DELETE FROM journal") protected abstract suspend fun clearJournal()
    @Query("DELETE FROM balance") protected abstract suspend fun clearBalance()
    @Query("DELETE FROM my_affirmations") protected abstract suspend fun clearMine()
    @Query("DELETE FROM favorites") protected abstract suspend fun clearFavorites()
    @Query("DELETE FROM visions") protected abstract suspend fun clearVisions()

    @Insert protected abstract suspend fun insertSessions(l: List<Session>)
    @Insert protected abstract suspend fun insertEntries(l: List<JournalEntry>)
    @Insert protected abstract suspend fun insertBalances(l: List<BalanceCheck>)
    @Insert protected abstract suspend fun insertMines(l: List<MyAffirmation>)
    @Insert protected abstract suspend fun insertFavorites(l: List<Favorite>)
    @Insert protected abstract suspend fun insertVisions(l: List<Vision>)

    @Transaction
    open suspend fun replaceAll(s: Snapshot) {
        clearSessions(); clearJournal(); clearBalance(); clearMine(); clearFavorites(); clearVisions()
        insertSessions(s.sessions); insertEntries(s.journal); insertBalances(s.balance)
        insertMines(s.affirmations); insertFavorites(s.favorites); insertVisions(s.visions)
    }
}

@Database(
    entities = [Session::class, JournalEntry::class, BalanceCheck::class, MyAffirmation::class, Favorite::class, Vision::class],
    version = 1,
    exportSchema = true,
)
abstract class SereneDatabase : RoomDatabase() {
    abstract fun dao(): SereneDao

    companion object {
        fun create(context: Context): SereneDatabase =
            Room.databaseBuilder(context.applicationContext, SereneDatabase::class.java, "serene.db").build()
    }
}
