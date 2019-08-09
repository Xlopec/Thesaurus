package word2vec
import arrow.core.Either
import arrow.core.Id
import arrow.core.Try
import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import misc.*
import org.deeplearning4j.models.embeddings.learning.ElementsLearningAlgorithm
import org.deeplearning4j.models.embeddings.learning.impl.elements.GloVe
import org.deeplearning4j.models.embeddings.learning.impl.elements.SkipGram
import org.deeplearning4j.models.word2vec.VocabWord
import org.deeplearning4j.models.word2vec.Word2Vec
import org.deeplearning4j.text.documentiterator.FileDocumentIterator
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.reflect.KClass
import org.deeplearning4j.models.embeddings.learning.impl.elements.CBOW as ND4JCBOW

typealias ElemAlgorithm = ElementsLearningAlgorithm<VocabWord>

class Word2VecTrainCommand : CliktCommand(
        help = """Trains word2vec model using source file or directory
        as input and stores model into default or specific destination file.

        Each sentence in text corpus should be start from a new line""",
        name = "train",
        printHelpOnEmptyArgs = true) {

    private val input: File by argument(name = "input", help = "Input directory of file that contains text corpus")
            .file(exists = true, fileOkay = true, folderOkay = true, readable = true)

    private val outputFile: File by option("-d", "--destination", help = "Destination file or directory to word2vec.store the model")
            .file(writable = true)
            .defaultLazy { generateDefaultFile().extract() }

    private val isVerbose: Boolean by option("-v", "--verbose", help = "Enables verbose mode").flag()

    private val algorithm: TrainAlgorithm by option("-a", "--algorithm", help = "Algorithm used to word2vec.train a model. " +
            "Default algorithm is ${TrainAlgorithm.SKIP_GRAM.shortName}, options ${TrainAlgorithm.values().joinToString("|", "[", "]") { it.shortName }}")
            .choice(*TrainAlgorithm.values().map { it.shortName })
            .convert { shortName ->
                TrainAlgorithm.values().findOrDefault(TrainAlgorithm.SKIP_GRAM) { it.shortName == shortName }
            }
            .default(TrainAlgorithm.SKIP_GRAM)

    private val minWordFrequency: Int by option("-m", "--min-word-frequency", help = "Minimum word frequency to be used in training")
            .int()
            .default(5)
            .validate { require(it > 0) { "--min-word-frequency should be greater than zero" } }

    private val layerSize: Int by option("-l", "--layer-size", help = "Layer size to be used in training")
            .int()
            .default(150)
            .validate { require(it > 0) { "--layer-size should be greater than zero" } }

    private val workers: Int by option("-w", "--workers", help = "Workers number to be used in training")
            .int()
            .default(Runtime.getRuntime().availableProcessors())
            .validate { require(it > 0) { "--workers should be greater than zero" } }

    private val iterations: Int by option("-i", "--iterations", help = "Iterations number to be used in training")
            .int()
            .default(5)
            .validate { require(it > 0) { "--iterations should be greater than zero" } }

    private val epochs: Int by option("-e", "--epochs", help = "Epochs number to be used in training")
            .int()
            .default(1)
            .validate { require(it > 0) { "--epochs should be greater than zero" } }

    private val logger = LoggerFactory.getLogger(Word2VecToDictCommand::class.java)

    override fun run() = when (val result = trainModel().toEither()) {
        is Either.Left -> logger.error(result.a.localizedMessage, result.a)
        is Either.Right -> logger.info("Model was successfully trained and stored to file ${outputFile.absolutePath}")
    }

    private fun trainModel() = updateLogLevel(if (isVerbose) Level.ALL else Level.OFF)
            .flatMap { algorithm.new() }
            .map { instance ->
                Word2VecArgs(input, instance, minWordFrequency, layerSize, workers, iterations, epochs)
            }
            .flatMap(::train)
            .flatMap { model -> prepareOutFile(outputFile).map { file -> model to file } }
            .map { (model, outFile) -> store(model, outFile) }

}

private enum class TrainAlgorithm(private val kClass: KClass<out ElementsLearningAlgorithm<*>>,
                                  val shortName: String) {

    SKIP_GRAM(SkipGram::class, "skip-gram"),
    CBOW(ND4JCBOW::class, "cbow"),
    GLOVE(GloVe::class, "glove");

    fun new(): Try<ElemAlgorithm> = Try {
        @Suppress("UNCHECKED_CAST")
        kClass.constructors.first().call() as ElemAlgorithm
    }
}

private data class Word2VecArgs(val input: File,
                                val algorithm: ElemAlgorithm,
                                val minWordFrequency: Int,
                                val layerSize: Int,
                                val workers: Int,
                                val iterations: Int,
                                val epochs: Int,
                                val tokenizer: TokenizerFactory = defaultTokenizer())

private fun defaultTokenizer() = DefaultTokenizerFactory().apply { tokenPreProcessor = CommonPreprocessor() }

private fun train(args: Word2VecArgs): Try<Word2Vec> = Try {
    args.run {
        Word2Vec.Builder()
                .minWordFrequency(minWordFrequency)
                .iterations(iterations)
                .epochs(epochs)
                .windowSize(layerSize)
                .layerSize(layerSize)
                .workers(workers)
                .elementsLearningAlgorithm(algorithm)
                .iterate(FileDocumentIterator(input))
                .tokenizerFactory(tokenizer)
                .build().also { it.fit() }
    }
}

private fun prepareOutFile(fileOrDir: File): Try<File> = Try {
    if (fileOrDir.isDirectory) {
        fileOrDir.mkdirs()
        require(fileOrDir.canWrite()) { "Can't write into $fileOrDir" }
        return@Try File(fileOrDir, generateDefaultFileName().extract())
    }

    return@Try fileOrDir
}

private fun generateDefaultFileName(pattern: String = TIMESTAMP_PATTERN): Id<String> {
    return Id.just("word2vec_${timestamp(pattern).value()}.zip")
}

private fun generateDefaultFile(pattern: String = TIMESTAMP_PATTERN): Id<File> {
    return Id(File(generateDefaultFileName(pattern).extract()))
}

fun main(args: Array<String>) = Word2VecTrainCommand().main(args)