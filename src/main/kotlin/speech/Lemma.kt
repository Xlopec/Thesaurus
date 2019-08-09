package speech

import java.util.*

class Lemma private constructor(val value: String) {
    companion object {

        private val CACHE = WeakHashMap<String, Lemma>()

        fun of(value: String): Lemma {
            return CACHE.getOrPut(value) { Lemma(value) }
        }
    }

    init {
        require(value.isNotEmpty() && value.isNotBlank())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Lemma

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = "Lemma(value='$value')"
}