package speech

import arrow.core.Either
import arrow.core.Option
import arrow.core.Option.Companion.empty
import arrow.core.Some
import arrow.core.getOrElse
import arrow.data.NonEmptyList
import arrow.instances.either.applicativeError.fromOption

typealias Lemmas = NonEmptyList<Lemma>

typealias ParseResult = Either<RuntimeException, SpeechPart>

typealias SpeechPartParser = (word: String, lemmas: Lemmas) -> Option<SpeechPart>

sealed class SpeechPart {

    companion object {

        private val parsers: NonEmptyList<SpeechPartParser> = NonEmptyList.of(Noun.Companion,
                Adjective.Companion, Numeral.Companion, Verb.Companion,
                Adverb.Companion, Conjunction.Companion, VerbalParticiple.Companion,
                Part.Companion, Interjection.Companion, Preposition.Companion)

        fun fromInput(input: String): ParseResult {

            val filtered = input.takeIf { it.isNotEmpty() }
                    ?: return Either.left(IllegalArgumentException("The input was empty"))

            val word = filtered.substringBefore(' ')
            val forms = NonEmptyList.fromListUnsafe(filtered.substringAfter(' ')
                    .splitToSequence(':').map(Lemma.Companion::of).toList())

            tailrec fun fromInput(it: Iterator<SpeechPartParser>): Option<SpeechPart> {
                if (!it.hasNext()) return empty()

                val parsed = it.next()(word, forms)

                if (parsed is Some) return parsed

                return fromInput(it)
            }

            return fromInput(parsers.iterator())
                    .fromOption { IllegalArgumentException("Couldn't parse input string=$input") }
        }

    }

    data class Noun(override val word: String, val declension: Declension) : SpeechPart() {

        companion object : SpeechPartParser {
            override fun invoke(word: String, lemmas: Lemmas): Option<Noun> {

                if (lemmas.head.value == "noun") {
                    return Option.just(Noun(word, lemmas.tail.parseDeclension()))
                }

                return empty()
            }
        }

        override fun toString(): String {
            return "іменник, ${declension.decletion} відм."
        }
    }

    data class Adjective(override val word: String, val declension: Declension) : SpeechPart() {

        companion object : SpeechPartParser {
            override fun invoke(word: String, lemmas: Lemmas): Option<Adjective> {
                if (lemmas.head.value == "adj") {
                    return Option.just(Adjective(word, lemmas.tail.parseDeclension()))
                }

                return empty()
            }
        }
    }


    data class Numeral(override val word: String,
                       val declension: Declension) : SpeechPart() {

        companion object : SpeechPartParser {
            override fun invoke(word: String, lemmas: Lemmas): Option<Numeral> {
                if (lemmas.head.value == "num") {
                    return Option.just(Numeral(word, lemmas.tail.parseDeclension()))
                }

                return empty()
            }
        }
    }

    data class Verb(override val word: String) : SpeechPart() {

        companion object : SpeechPartParser {
            override fun invoke(word: String, lemmas: Lemmas): Option<Verb> {
                if (lemmas.head.value == "verb") {
                    return Option.just(Verb(word))
                }

                return empty()
            }
        }
    }

    data class Adverb(override val word: String) : SpeechPart() {

        companion object : SpeechPartParser {
            override fun invoke(word: String, lemmas: Lemmas): Option<Adverb> {
                if (lemmas.head.value == "adv") {
                    return Option.just(Adverb(word))
                }

                return empty()
            }
        }
    }

    data class Conjunction(override val word: String) : SpeechPart() {

        companion object : SpeechPartParser {
            override fun invoke(word: String, lemmas: Lemmas): Option<Conjunction> {
                if (lemmas.head.value == "conj") {
                    return Option.just(Conjunction(word))
                }

                return empty()
            }
        }
    }

    data class VerbalParticiple(override val word: String) : SpeechPart() {

        companion object : SpeechPartParser {
            override fun invoke(word: String, lemmas: Lemmas): Option<VerbalParticiple> {
                if (lemmas.head.value == "advp") {
                    return Option.just(VerbalParticiple(word))
                }

                return empty()
            }
        }
    }

    data class Part(override val word: String) : SpeechPart() {

        companion object : SpeechPartParser {
            override fun invoke(word: String, lemmas: Lemmas): Option<Part> {
                if (lemmas.head.value == "part") {
                    return Option.just(Part(word))
                }

                return empty()
            }
        }
    }

    data class Interjection(override val word: String) : SpeechPart() {
        companion object : SpeechPartParser {
            override fun invoke(word: String, lemmas: Lemmas): Option<Interjection> {
                if (lemmas.head.value == "intj") {
                    return Option.just(Interjection(word))
                }

                return empty()
            }
        }
    }

    data class Preposition(override val word: String) : SpeechPart() {
        companion object : SpeechPartParser {
            override fun invoke(word: String, lemmas: Lemmas): Option<Preposition> {
                if (lemmas.head.value == "prep") {
                    return Option.just(Preposition(word))
                }

                return empty()
            }
        }
    }

    abstract val word: String
}

private fun List<Lemma>.parseDeclension(parser: (Lemma) -> Option<Declension> = ::declension,
                                        default: Declension = Declension.NOMINATIVE): Declension {

    return parse(parser).getOrElse { default }
}

private fun <T> List<Lemma>.parse(parser: (Lemma) -> Option<T>): Option<T> {

    tailrec fun find(i: Int): Option<T> {
        if (i == size) return empty()

        val parsed = parser(this[i])

        if (parsed is Some) return parsed

        return find(i + 1)
    }

    return find(0)
}