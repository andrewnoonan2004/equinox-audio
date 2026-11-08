package com.equinox.audioeq.data.autoeq

import android.content.Context
import com.equinox.audioeq.model.AutoEqProfile
import com.equinox.audioeq.model.FilterType
import com.equinox.audioeq.model.ParametricFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AutoEqRepository(context: Context) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private var cache: List<AutoEqProfile>? = null

    suspend fun profiles(): List<AutoEqProfile> = mutex.withLock {
        cache ?: withContext(Dispatchers.IO) { loadDatabase() }.also { cache = it }
    }

    suspend fun findById(id: String): AutoEqProfile? = profiles().firstOrNull { it.id == id }

    suspend fun search(query: String): List<AutoEqProfile> {
        val all = profiles()
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return all
        return all.filter { profile ->
            val haystack = "${profile.brand} ${profile.model}".lowercase()
            terms.all { it in haystack }
        }
    }

    private fun loadDatabase(): List<AutoEqProfile> {
        val json = appContext.assets.open(DATABASE_ASSET).bufferedReader().use { it.readText() }
        val profiles = JSONObject(json).getJSONArray("profiles")
        return List(profiles.length()) { i ->
            val obj = profiles.getJSONObject(i)
            val geq = obj.getJSONObject("graphicEq")
            val filters = obj.getJSONArray("filters")
            AutoEqProfile(
                id = obj.getString("id"),
                brand = obj.getString("brand"),
                model = obj.getString("model"),
                source = obj.getString("source"),
                target = obj.getString("target"),
                preampDb = obj.getDouble("preampDb").toFloat(),
                graphicEqBands = geq.keys().asSequence().associate { it.toInt() to geq.getDouble(it).toFloat() },
                parametricFilters = List(filters.length()) { j ->
                    val f = filters.getJSONObject(j)
                    ParametricFilter(
                        type = FilterType.valueOf(f.getString("type")),
                        frequencyHz = f.getDouble("frequencyHz").toFloat(),
                        gainDb = f.getDouble("gainDb").toFloat(),
                        qFactor = f.getDouble("qFactor").toFloat(),
                    )
                },
            )
        }.sortedWith(compareBy({ it.brand.lowercase() }, { it.model.lowercase() }))
    }

    companion object {
        private const val DATABASE_ASSET = "autoeq_database.json"
        const val IMPORTED_ID_PREFIX = "import:"

        private val PREAMP = Regex("""^\s*Preamp:\s*(-?\d+(?:\.\d+)?)\s*dB""", RegexOption.IGNORE_CASE)
        private val GRAPHIC_EQ = Regex("""^\s*GraphicEQ:\s*(.+)$""", RegexOption.IGNORE_CASE)
        private val FILTER = Regex(
            """^\s*Filter\s*\d*:\s*ON\s+(PK|PEQ|LSC|LS|HSC|HS|LPQ|LP|HPQ|HP)\s+Fc\s+(\d+(?:\.\d+)?)\s*Hz""" +
                """(?:\s+Gain\s+(-?\d+(?:\.\d+)?)\s*dB)?(?:\s+Q\s+(\d+(?:\.\d+)?))?""",
            RegexOption.IGNORE_CASE,
        )

        /**
         * Parses an AutoEq / Equalizer APO style text export. Supports `Preamp:`, a single
         * `GraphicEQ: f g; f g; ...` line, and `Filter N: ON PK|LSC|HSC Fc .. Hz Gain .. dB Q ..` lines.
         * Returns null when the text contains neither graphic nor parametric data.
         */
        fun parseProfileText(name: String, text: String): AutoEqProfile? {
            var preamp = 0f
            val graphic = sortedMapOf<Int, Float>()
            val filters = mutableListOf<ParametricFilter>()

            text.lineSequence().forEach { line ->
                PREAMP.find(line)?.let { preamp = it.groupValues[1].toFloat(); return@forEach }
                GRAPHIC_EQ.find(line)?.let { match ->
                    match.groupValues[1].split(';').forEach { pair ->
                        val parts = pair.trim().split(Regex("\\s+"))
                        val freq = parts.getOrNull(0)?.toFloatOrNull()
                        val gain = parts.getOrNull(1)?.toFloatOrNull()
                        if (freq != null && gain != null) graphic[freq.toInt()] = gain
                    }
                    return@forEach
                }
                FILTER.find(line)?.let { m ->
                    val type = when (m.groupValues[1].uppercase()) {
                        "PK", "PEQ" -> FilterType.PEAKING
                        "LSC", "LS" -> FilterType.LOW_SHELF
                        "HSC", "HS" -> FilterType.HIGH_SHELF
                        "LP", "LPQ" -> FilterType.LOW_PASS
                        else -> FilterType.HIGH_PASS
                    }
                    filters += ParametricFilter(
                        type = type,
                        frequencyHz = m.groupValues[2].toFloat(),
                        gainDb = m.groupValues[3].toFloatOrNull() ?: 0f,
                        qFactor = m.groupValues[4].toFloatOrNull() ?: 0.707f,
                    )
                }
            }
            if (graphic.isEmpty() && filters.isEmpty()) return null

            val cleanName = name.substringBeforeLast('.')
                .removeSuffix(" ParametricEQ").removeSuffix(" GraphicEQ").trim().ifEmpty { "Imported profile" }
            return AutoEqProfile(
                id = IMPORTED_ID_PREFIX + cleanName,
                brand = "",
                model = cleanName,
                source = "Imported file",
                target = "Custom",
                preampDb = preamp,
                graphicEqBands = graphic,
                parametricFilters = filters,
            )
        }
    }
}
