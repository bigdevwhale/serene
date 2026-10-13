package app.serene.content

import app.serene.data.MyAffirmation
import app.serene.data.Vision
import java.time.LocalDate
import java.util.Locale

/** An affirmation with the key it is stored under in favourites. */
data class Affirmation(val key: String, val text: String, val setId: String)

/** Everything the user reads or hears, in one language. */
class Library private constructor(
    val ru: Boolean,
    val meditations: List<Guided>,
    val visualizations: List<Guided>,
    val sets: List<AffirmationSet>,
) {
    val guided: List<Guided> get() = meditations + visualizations

    fun guided(id: String): Guided? = guided.firstOrNull { it.id == id }

    fun set(id: String): AffirmationSet? = sets.firstOrNull { it.id == id }

    fun affirmations(set: AffirmationSet): List<Affirmation> =
        set.items.mapIndexed { i, t -> Affirmation("a:${set.id}:$i", t, set.id) }

    /** Resolves a favourite key to its text in this language; null when it no longer exists. */
    fun affirmation(key: String, mine: List<MyAffirmation>): Affirmation? {
        val parts = key.split(':')
        return when (parts.firstOrNull()) {
            "a" -> parts.getOrNull(2)?.toIntOrNull()?.let { i ->
                set(parts[1])?.items?.getOrNull(i)?.let { Affirmation(key, it, parts[1]) }
            }
            "m" -> parts.getOrNull(1)?.toLongOrNull()?.let { id ->
                mine.firstOrNull { it.id == id }?.let { Affirmation(key, it.text, MINE) }
            }
            else -> null
        }
    }

    /**
     * Same affirmation all day, a new one tomorrow. Draws from the chosen focus sets, the
     * user's own lines and favourites, so the daily card feels personal.
     */
    fun daily(day: LocalDate, focus: Set<String>, favorites: List<String>, mine: List<MyAffirmation>): Affirmation {
        val pool = mix(focus, favorites, mine)
        return pool[Math.floorMod(day.toEpochDay() * 7919, pool.size.toLong()).toInt()]
    }

    /** The "my mix" playlist: focus sets, then favourites and own lines, without repeats. */
    fun mix(focus: Set<String>, favorites: List<String>, mine: List<MyAffirmation>): List<Affirmation> {
        val chosen = sets.filter { focus.isEmpty() || it.id in focus }.ifEmpty { sets }
        val base = chosen.flatMap(::affirmations)
        val extra = favorites.mapNotNull { affirmation(it, mine) } + mine.map { Affirmation("m:${it.id}", it.text, MINE) }
        return (extra + base).distinctBy { it.key }
    }

    /** A session made from the user's own vision board. */
    fun visionSession(visions: List<Vision>): Guided {
        val steps = buildList {
            if (ru) {
                add(s("Устройтесь удобно и мягко закройте глаза.", 3))
                add(s("Сделайте медленный вдох… и долгий спокойный выдох.", 5))
                add(s("Ещё раз. Вдох… и выдох. Пусть тело становится мягче.", 6))
                add(s("Сейчас мы побудем в том, что для вас важно. Так, словно это уже случилось.", 4))
            } else {
                add(s("Settle into a comfortable position and gently close your eyes.", 3))
                add(s("Take a slow breath in… and a long, easy breath out.", 5))
                add(s("Once more. Breathe in… and out. Let your body soften.", 6))
                add(s("Now let's spend a few moments with what matters to you, as if it has already come true.", 4))
            }
            visions.forEachIndexed { i, v ->
                if (ru) {
                    add(Step(if (i == 0) "Представьте: ${v.title}." else "А теперь представьте: ${v.title}.", 5, i))
                    if (v.details.isNotBlank()) add(Step(v.details.trim(), 10, i))
                    add(Step("Посмотрите вокруг. Какие цвета, какой свет? Кто рядом с вами?", 14, i))
                    add(Step("Прислушайтесь. Какие звуки вы слышите в этот момент?", 10, i))
                    add(Step("Почувствуйте, каково это — жить это прямо сейчас. Где в теле живёт эта радость? Позвольте ей расти.", 16, i))
                    if (v.affirmation.isNotBlank()) add(Step("Скажите себе: ${v.affirmation.trim()}", 8, i))
                } else {
                    add(Step(if (i == 0) "Picture this: ${v.title}." else "Now picture this: ${v.title}.", 5, i))
                    if (v.details.isNotBlank()) add(Step(v.details.trim(), 10, i))
                    add(Step("Look around. What colours do you see, what light? Who is there with you?", 14, i))
                    add(Step("Listen. What sounds are there in this moment?", 10, i))
                    add(Step("Feel what it is like to live this, right now. Where in your body is the joy? Let it grow.", 16, i))
                    if (v.affirmation.isNotBlank()) add(Step("Say to yourself: ${v.affirmation.trim()}", 8, i))
                }
            }
            if (ru) {
                add(s("Сделайте глубокий вдох и сохраните это чувство. Оно уже ваше.", 5))
                add(s("Подумайте об одном маленьком шаге, который вы можете сделать сегодня.", 10))
                add(s("Пошевелите пальцами. Когда будете готовы, откройте глаза.", 3))
            } else {
                add(s("Take a deep breath and keep this feeling. It's already yours.", 5))
                add(s("Think of one small step you could take today.", 10))
                add(s("Wiggle your fingers. When you're ready, open your eyes.", 3))
            }
        }
        return Guided(
            VISIONS_ID, GuidedKind.VISUALIZATION,
            if (ru) "Моя доска визуализаций" else "My vision board", "",
            Scene.DAWN, Ambience.DRONE, steps,
        )
    }

    companion object {
        const val MINE = "mine"
        const val VISIONS_ID = "visions"

        private val en by lazy { Library(false, meditationsEn, visualizationsEn, affirmationsEn) }
        private val ru by lazy { Library(true, meditationsRu, visualizationsRu, affirmationsRu) }

        fun of(locale: Locale): Library = if (locale.language == "ru") ru else en
    }
}

/** Rough spoken length: slow text-to-speech plus the silence after each line. */
fun Step.estimateSeconds(ru: Boolean): Float {
    val words = text.split(' ', '\n').count { it.isNotBlank() }
    return words / (if (ru) 2.0f else 2.3f) + pause
}

fun Guided.estimateMinutes(ru: Boolean): Int =
    maxOf(1, Math.round(steps.sumOf { it.estimateSeconds(ru).toDouble() } / 60.0).toInt())
