package app.serene.data

import android.content.Context
import java.util.Locale

/** Made-up practice history for README screenshots; only takes effect with `-Pdemo` (app.serene.demo). */
object DemoSeed {
    private const val DAY = 86_400_000L
    private const val HOUR = 3_600_000L

    suspend fun seedIfEmpty(context: Context, db: SereneDatabase, settings: SettingsStore) {
        val dao = db.dao()
        if (dao.sessions().isNotEmpty()) return
        val ru = Locale.getDefault().language == "ru"
        settings.update { it.copy(onboarded = true, name = if (ru) "Аня" else "Anna", focus = setOf("calm", "self_love", "gratitude")) }
        val now = System.currentTimeMillis()
        val pattern = listOf(
            PracticeKind.MEDITATION to "breath_awareness", PracticeKind.BREATH to "calm", PracticeKind.VISUALIZATION to "ocean_shore",
            PracticeKind.MEDITATION to "body_scan", PracticeKind.TIMER to "", PracticeKind.VISUALIZATION to "safe_place",
            PracticeKind.AFFIRMATIONS to "calm", PracticeKind.MEDITATION to "gratitude",
        )
        // Three weeks of practice with two missed days, and a fresh streak of nine.
        for (d in 1..21) {
            if (d == 10 || d == 15) continue
            val (kind, ref) = pattern[d % pattern.size]
            dao.insertSession(Session(at = now - d * DAY - 2 * HOUR, kind = kind, ref = ref, seconds = 300 + (d * 97) % 600))
            if (d % 3 == 0) dao.insertSession(Session(at = now - d * DAY + 9 * HOUR, kind = PracticeKind.BREATH, ref = "box", seconds = 180))
        }
        dao.insertSession(Session(at = now - 2 * HOUR, kind = PracticeKind.MEDITATION, ref = "morning_intention", seconds = 300))

        val moods = listOf(4, 3, 4, 5, 3, 2, 3, 4, 4, 5, 4, 3, 4, 5)
        val gratitude = if (ru) listOf("Тёплое утро и кофе на балконе", "Долгий разговор с мамой", "Прогулка в парке после дождя", "Коллега помог с отчётом")
        else listOf("Warm morning coffee on the balcony", "A long call with my mum", "A walk in the park after the rain", "A colleague helped me finish the report")
        moods.forEachIndexed { i, m ->
            dao.insertEntry(
                JournalEntry(
                    at = now - i * DAY - 3 * HOUR, mood = m,
                    gratitude = if (i % 2 == 0) gratitude.drop(i / 2 % 3).take(2).joinToString("\n") else "",
                    note = if (i == 0) (if (ru) "Спокойный день. Хорошо выспалась." else "A calm day. Slept well.") else "",
                )
            )
        }
        dao.insertBalance(BalanceCheck(at = now - 14 * DAY, scores = "5,4,7,6,3,5"))
        dao.insertBalance(BalanceCheck(at = now - DAY, scores = "7,6,8,6,6,7"))

        listOf("a:calm:0", "a:self_love:2", "a:gratitude:1", "g:ocean_shore", "g:body_scan").forEach { dao.favorite(Favorite(it)) }
        dao.insertMine(MyAffirmation(text = if (ru) "Я двигаюсь в своём темпе, и это правильно." else "I move at my own pace, and that is right."))

        val visions = if (ru) listOf(
            Triple("Дом у моря", "Светлая кухня, открытое окно, слышно волны и чаек.", "Мой дом наполнен светом и покоем."),
            Triple("Пробежать полумарафон", "Финишная линия, друзья машут, ноги лёгкие.", "Моё тело сильное и выносливое."),
            Triple("Своя студия керамики", "Запах глины, тёплый свет, ученики за гончарными кругами.", "Я создаю то, что люблю."),
        ) else listOf(
            Triple("A house by the sea", "A bright kitchen, the window open, waves and gulls outside.", "My home is full of light and peace."),
            Triple("Run a half marathon", "The finish line, friends cheering, my legs feel light.", "My body is strong and steady."),
            Triple("My own pottery studio", "The smell of clay, warm light, students at the wheels.", "I create what I love."),
        )
        visions.forEachIndexed { i, (t, d, a) -> dao.insertVision(Vision(title = t, details = d, affirmation = a, hue = i, createdAt = now - (30 - i) * DAY)) }
    }
}
