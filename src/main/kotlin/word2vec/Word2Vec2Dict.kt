package word2vec

import arrow.core.*
import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import format.toHumanReadableString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import misc.*
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.word2vec.Word2Vec
import org.slf4j.LoggerFactory
import speech.SpeechPart
import java.io.File
import java.io.FileWriter

fun main(args: Array<String>) = Word2VecToDictCommand().main(args)

class Word2VecToDictCommand : CliktCommand(
        help = "Builds thesaurus using a trained word2vec model and POS tag dictionary",
        name = "build",
        printHelpOnEmptyArgs = true) {

    private val dictionaryFile: File by argument(name = "dictionary", help = "Dictionary file")
            .file(exists = true, fileOkay = true, folderOkay = false, readable = true)

    private val modelFile: File by argument(name = "model", help = "Model file")
            .file(exists = true, fileOkay = true, folderOkay = false, readable = true)

    private val outputFile: File by option("-d", "--destination", help = "Destination file or directory to store the model")
            .file(writable = true)
            .defaultLazy { generateDefaultFile().extract() }

    private val chunkSize: Int by option("-c", "--chunk-size", help = "Chunk size to be used")
            .int()
            .default(1000)
            .validate { require(it > 0) { "--chunk-size should be greater than zero" } }

    private val top: Int by option("-t", "--top", help = "Top N related words to be used")
            .int()
            .default(10)
            .validate { require(it > 0) { "--top should be greater than zero" } }

    private val isVerbose: Boolean by option("-v", "--verbose", help = "Enables verbose mode").flag()

    private val logger = LoggerFactory.getLogger(Word2VecToDictCommand::class.java)

    override fun run() {
        runBlocking {
            val dictionary = GlobalScope.async { loadDictionary(dictionaryFile) }
            val model = GlobalScope.async { loadModel(modelFile) }
            @Suppress("MoveVariableDeclarationIntoWhen")
            val result = updateLogLevel(if (isVerbose) Level.ALL else Level.OFF).toEither().flatMap {
                dictionary.await().flatMap { dict -> model.await().flatMap { model -> Right(buildThesaurus(dict, model)) } }
                        .flatMap { entries -> storeToFile(entries).toEither() }
            }

            when (result) {
                is Either.Left -> logger.error(result.a.localizedMessage, result.a)
                is Either.Right -> logger.info("Thesaurus was successfully built and stored to file ${outputFile.absolutePath}")
            }
        }
    }

    private suspend fun buildThesaurus(dictionary: Dictionary, model: Word2Vec): List<DictionaryEntry> {
        logger.info("Loaded ${dictionary.size} POS definitions, ${model.vocab.numWords()} word2vec words")

        val allWords = model.vocab.words()

        val vocabEntries = allWords.asSequence().chunked(chunkSize)
                .map { chunk -> GlobalScope.async(Dispatchers.Default) { chunk.toDictionaryEntries(model, dictionary, top) } }
                .map { runBlocking { it.await() } }
                .flatten()
                .sortedByDescending { it.word }
                .toList()

        val loss = allWords.size - vocabEntries.size

        logger.info("%s entries left unprocessed (%.1f%% loss)".format(loss, (loss.toDouble() / allWords.size) * 100.0))
        logger.info("Storing ${vocabEntries.size} definitions into file")

        return vocabEntries
    }

    private fun storeToFile(vocabEntries: List<DictionaryEntry>) = Try {
        FileWriter(outputFile).use { fr -> vocabEntries.forEach { entry -> fr.append(entry.content).appendln() } }
    }

}

private data class DictionaryEntry(val word: String, val content: String)

private fun generateDefaultFileName(pattern: String = TIMESTAMP_PATTERN): Id<String> {
    return Id.just("thesaurus_${timestamp(pattern).value()}.txt")
}

private fun generateDefaultFile(pattern: String = TIMESTAMP_PATTERN): Id<File> {
    return Id(File(generateDefaultFileName(pattern).extract()))
}

private fun loadModel(file: File): Either<Throwable, Word2Vec> {
    return Try { WordVectorSerializer.readWord2VecModel(file, true) }.toEither()
}

private fun Collection<String>.toDictionaryEntries(vec: Word2Vec, dictionary: Dictionary, topN: Int): List<DictionaryEntry> {
    return asSequence()
            .mapNotNull(dictionary::get)
            .map { speechPart ->

                val content = toHumanReadableString(speechPart, vec.nearestSpeechParts(dictionary, speechPart, topN))

                DictionaryEntry(speechPart.word, content)
            }
            .toList()
}

private fun Word2Vec.nearestSpeechParts(dictionary: Dictionary, to: SpeechPart, topN: Int): List<SpeechPart> {
    return wordsNearest(to.word, topN).mapNotNull(dictionary::get)
}