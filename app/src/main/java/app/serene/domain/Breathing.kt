package app.serene.domain

enum class PhaseType { INHALE, TOP_UP, HOLD, EXHALE, REST }

class Phase(val type: PhaseType, val seconds: Float)

/** A breathing rhythm, repeated until the chosen time is up. Titles live in strings (breath_<id>). */
class Pattern(val id: String, val phases: List<Phase>) {
    val cycle: Float get() = phases.sumOf { it.seconds.toDouble() }.toFloat()

    /** Whole cycles that fit into [minutes]; never fewer than three, so even a short session settles. */
    fun cycles(minutes: Int): Int = maxOf(3, (minutes * 60 / cycle).toInt())

    /** "4 · 7 · 8" */
    fun rhythm(): String = phases.joinToString(" · ") { p ->
        if (p.seconds % 1f == 0f) p.seconds.toInt().toString() else p.seconds.toString()
    }

    companion object {
        private fun p(vararg phases: Pair<PhaseType, Float>) = phases.map { Phase(it.first, it.second) }

        val all = listOf(
            // Long exhales nudge the body toward rest.
            Pattern("calm", p(PhaseType.INHALE to 4f, PhaseType.EXHALE to 6f)),
            Pattern("box", p(PhaseType.INHALE to 4f, PhaseType.HOLD to 4f, PhaseType.EXHALE to 4f, PhaseType.REST to 4f)),
            Pattern("relax", p(PhaseType.INHALE to 4f, PhaseType.HOLD to 7f, PhaseType.EXHALE to 8f)),
            Pattern("coherent", p(PhaseType.INHALE to 5.5f, PhaseType.EXHALE to 5.5f)),
            // Two inhales through the nose, one long sigh out.
            Pattern("sigh", p(PhaseType.INHALE to 2.5f, PhaseType.TOP_UP to 1f, PhaseType.EXHALE to 6f)),
            Pattern("balance", p(PhaseType.INHALE to 4f, PhaseType.EXHALE to 4f)),
        )

        fun byId(id: String) = all.firstOrNull { it.id == id } ?: all.first()
    }
}
