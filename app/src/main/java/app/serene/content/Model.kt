package app.serene.content

enum class GuidedKind { MEDITATION, VISUALIZATION }

/** The colours and drifting shapes behind a session. */
enum class Scene { DAWN, OCEAN, FOREST, NIGHT, MOUNTAIN, MEADOW, LIGHT, SPACE }

/** Background sound, synthesised on the device (see app.serene.audio.Soundscape). */
enum class Ambience { NONE, RAIN, OCEAN, STREAM, WIND, FOREST, FIRE, DRONE, BROWN }

/**
 * One spoken line of a guided session, then [pause] seconds of quiet. [tag] marks which vision
 * a line belongs to; [audio] is the user's own recording (a file name in filesDir/audio) to play
 * instead of speaking [text].
 */
class Step(val text: String, val pause: Int = 6, val tag: Int = -1, val audio: String = "")

fun s(text: String, pause: Int = 6) = Step(text, pause)

class Guided(
    /** Stable across languages: history and favourites refer to it. */
    val id: String,
    val kind: GuidedKind,
    val title: String,
    val subtitle: String,
    val scene: Scene,
    val ambience: Ambience,
    val steps: List<Step>,
)

/** A themed list of affirmations. Lists are aligned across languages: item i is the same thought. */
class AffirmationSet(val id: String, val title: String, val items: List<String>)
