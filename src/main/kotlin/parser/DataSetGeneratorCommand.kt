package parser

import arrow.core.Id
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import info.bliki.wiki.dump.WikiXMLParser
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import misc.timestamp
import org.slf4j.LoggerFactory
import word2vec.Word2VecToDictCommand
import java.io.File

class DataSetGeneratorCommand : CliktCommand(
        help = "Reads a compressed wiki file, sanitizes text and splits it in a smaller chunks",
        name = "dataset",
        printHelpOnEmptyArgs = true) {

    private val wikiFile: File by argument(name = "wiki", help = "A compressed wiki file")
            .file(exists = true, fileOkay = true, folderOkay = false, readable = true)

    private val outputFile: File by option("-d", "--destination", help = "Destination file or directory to word2vec.store the model")
            .file(writable = true)
            .defaultLazy { generateDefaultFile().extract() }

    private val chunkSize: Int by option("-c", "--chunk-size", help = "Chunk size to be used")
            .int()
            .default(1000)
            .validate { require(it > 0) { "--chunk-size should be greater than zero" } }

    private val isVerbose: Boolean by option("-v", "--verbose", help = "Enables verbose mode").flag()

    private val logger = LoggerFactory.getLogger(Word2VecToDictCommand::class.java)

    override fun run() {

        (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger).level = if (isVerbose) Level.ALL else Level.OFF

        val outFile = if (outputFile.isDirectory) {
            outputFile.mkdirs()
            File(outputFile, generateDefaultFileName().extract())
        } else {
            outputFile
        }

        logger.info("Processing file $wikiFile, data will be located in $outFile")

        val workers = mutableListOf<Deferred<Unit>>()

        val handler = ChunkArticleFilter(
                chunkSize,
                { chunk -> logger.info("Parsed chunk of size ${chunk.size}"); workers += GlobalScope.async { writeChunk(chunk, outFile) } },
                { logger.info("Skipping article $it") }
        )

        val wxp = WikiXMLParser(outFile, handler)

        wxp.parse()

        runBlocking {
            handler.onFlush()
            workers.forEach { it.await() }
        }

        logger.info("Done parsing wiki")
    }

}

private fun generateDefaultFileName(pattern: String = "hh:mm:ss:SSS'_on_'dd-MMM-yyyy"): Id<String> {
    return Id.just("dataset_${timestamp(pattern).value()}")
}

private fun generateDefaultFile(pattern: String = "hh:mm:ss:SSS'_on_'dd-MMM-yyyy"): Id<File> {
    return Id(File(generateDefaultFileName(pattern).extract()))
}

fun main(args: Array<String>) = DataSetGeneratorCommand().main(args)

private fun writeChunk(articles: List<Article>, outFile: File) {
    articles.forEach { article ->
        File(outFile, "${article.title}_${timestamp()}.txt").writeText(article.text)
    }
}