@file:Suppress("unused")

package misc

import arrow.core.*
import arrow.data.extensions.list.foldable.foldLeft
import arrow.effects.IO
import arrow.instances.`try`.monadThrow.bindingCatch
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import exception.ParseException
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.word2vec.Word2Vec
import org.slf4j.LoggerFactory
import speech.SpeechPart
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val TIMESTAMP_PATTERN = "hh:mm:ss:SSS'_on_'dd-MMM-yyyy"

typealias Dictionary = Map<String, SpeechPart>

inline fun <T, reified R> Array<T>.map(transform: (T) -> R): Array<R> {
    return Array(size) { i -> transform(this[i]) }
}

inline fun <T> Iterable<T>.findOrDefault(default: T, predicate: (T) -> Boolean): T {
    return find(predicate) ?: default
}

inline fun <T> Array<T>.findOrDefault(default: T, predicate: (T) -> Boolean): T {
    return find(predicate) ?: default
}

fun File.dicUkFormatToUsableFormat(out: File) {

    fun toTuple(str: String): Pair<String, String> {
        val split = str.split(' ')

        require(split.size == 3) { "Invalid line was found=$str" }

        return split[0] to split[2]
    }

    return BufferedReader(FileReader(this)).use { br ->

        BufferedWriter(FileWriter(out, false)).use { bw ->

            br.lineSequence()
                    .drop(1)
                    .filter { it.isNotEmpty() && it.isNotBlank() }
                    .map(::toTuple)
                    .forEach { (word, data) ->

                        bw.append(word).append(' ').append(data).appendln()

                    }
        }
    }
}

fun loadDictionary(dictionaryFile: File): Either<Throwable, Dictionary> {

    fun <M> M.put(part: SpeechPart): M where M : MutableMap<String, SpeechPart> {
        this[part.word] = part
        return this
    }

    fun readAndParseDictionary() = IO { dictionaryFile.useLines { lines -> lines.parseDictionary(dictionaryFile) } }

    return bindingCatch {
        when (val parseResult = readAndParseDictionary().unsafeRunSync()) {
            is Either.Left -> throw parseResult.a
            is Either.Right -> parseResult.b.foldLeft(HashMap<String, SpeechPart>()) { acc, part -> acc.put(part) }
        }
    }.toEither()
}

fun timestamp(pattern: String = TIMESTAMP_PATTERN): Id<String> {
    return Id.just(DateTimeFormatter.ofPattern(pattern).format(LocalDateTime.now()))
}

fun updateLogLevel(level: Level): Try<Unit> = Try {
    (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = level
}

fun store(model: Word2Vec, file: File): Try<Unit> = Try {
    WordVectorSerializer.writeWord2VecModel(model, file)
}

private fun Sequence<String>.parseDictionary(dictionaryFile: File): Either<ParseException, List<SpeechPart>> {

    fun parseLine(index: Int, line: String): Either<ParseException, SpeechPart> {
        return SpeechPart.fromInput(line)
                .mapLeft { th -> parseException(th, dictionaryFile, line, index + 1) }
    }

    return filter { it.isNotEmpty() && it.isNotBlank() }
            .mapIndexed(::parseLine)
            .fold(ArrayList<SpeechPart>()) { acc, elem ->
                acc += when (elem) {
                    is Either.Left -> return Left(elem.a)
                    is Either.Right -> elem.b
                }

                acc
            }
            .let(::Right)

}

private fun parseException(cause: Throwable, dictionaryFile: File, line: String, postion: Int): ParseException {
    return ParseException("Failed to parse file $dictionaryFile, line: $line, position: $postion", cause)
}