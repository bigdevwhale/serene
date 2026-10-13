package app.serene.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Plain JSON backup: everything stays on the device unless the user exports it.
 * Vision photos are not included; their cards come back with a gradient instead.
 */
object Backup {
    private const val VERSION = 1

    fun encode(s: Snapshot): String = JSONObject()
        .put("app", "serene")
        .put("version", VERSION)
        .put("sessions", JSONArray(s.sessions.map {
            JSONObject().put("id", it.id).put("at", it.at).put("kind", it.kind.name).put("ref", it.ref).put("seconds", it.seconds)
        }))
        .put("journal", JSONArray(s.journal.map {
            JSONObject().put("id", it.id).put("at", it.at).put("mood", it.mood ?: JSONObject.NULL)
                .put("gratitude", it.gratitude).put("note", it.note)
        }))
        .put("balance", JSONArray(s.balance.map { JSONObject().put("id", it.id).put("at", it.at).put("scores", it.scores) }))
        .put("affirmations", JSONArray(s.affirmations.map { JSONObject().put("id", it.id).put("text", it.text).put("createdAt", it.createdAt) }))
        .put("favorites", JSONArray(s.favorites.map { JSONObject().put("key", it.key).put("at", it.at) }))
        .put("visions", JSONArray(s.visions.map {
            JSONObject().put("id", it.id).put("title", it.title).put("details", it.details).put("affirmation", it.affirmation)
                .put("hue", it.hue).put("createdAt", it.createdAt)
        }))
        .toString(2)

    /** Throws [IllegalArgumentException] when the text is not a Serene backup. */
    fun decode(text: String): Snapshot {
        val root = runCatching { JSONObject(text) }.getOrNull()
        require(root != null && root.optString("app") == "serene") { "Not a Serene backup" }
        return Snapshot(
            sessions = root.list("sessions") {
                Session(getLong("id"), getLong("at"), PracticeKind.valueOf(getString("kind")), optString("ref"), getInt("seconds"))
            },
            journal = root.list("journal") {
                JournalEntry(getLong("id"), getLong("at"), if (isNull("mood")) null else getInt("mood"), optString("gratitude"), optString("note"))
            },
            balance = root.list("balance") { BalanceCheck(getLong("id"), getLong("at"), getString("scores")) },
            affirmations = root.list("affirmations") { MyAffirmation(getLong("id"), getString("text"), getLong("createdAt")) },
            favorites = root.list("favorites") { Favorite(getString("key"), getLong("at")) },
            visions = root.list("visions") {
                Vision(getLong("id"), getString("title"), optString("details"), optString("affirmation"), "", optInt("hue"), getLong("createdAt"))
            },
        )
    }

    private fun <T> JSONObject.list(key: String, read: JSONObject.() -> T): List<T> {
        val arr = optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).map { arr.getJSONObject(it).read() }
    }
}
