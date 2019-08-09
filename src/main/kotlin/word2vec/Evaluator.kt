package word2vec

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer
import org.deeplearning4j.models.word2vec.Word2Vec
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import kotlin.math.roundToInt

typealias Questions = List<Pair<String, List<String>>>

class EvaluateCommand : CliktCommand(
        help = "Evaluates word2vec",
        name = "evaluate",
        printHelpOnEmptyArgs = true) {

    private val modelFile: File by argument(name = "model", help = "Model file or directory")
            .file(exists = true, fileOkay = true, folderOkay = true, readable = true)

    private val questionFile: File by argument(name = "questions", help = "Questions file")
            .file(exists = true, fileOkay = true, folderOkay = false, readable = true)

    override fun run() {

        val questions = BufferedReader(FileReader(questionFile))
                .lineSequence()
                .map { it.split(" ").filter { it.isNotEmpty() && it.isNotBlank() } }
                .map { it[0] to it.subList(1, it.size) }
                .toList()

        if (modelFile.isFile) {
            evaluate(questions, modelFile)
        } else {
            modelFile.listFiles().forEach { modelFile -> evaluate(questions, modelFile) }
        }
    }

    private fun evaluate(questions: Questions, modelFile: File) {
        require(modelFile.isFile) { "$modelFile is not a file" }

        val model: Word2Vec = WordVectorSerializer.readWord2VecModel(modelFile, true)

        val correct = questions.fold(0) { acc, (given, expected) ->
            val nearest = model.wordsNearest(given, 10)

            if (expected.minus(nearest).size != expected.size) {
                acc + 1
            } else {
                acc
            }
        }

        echo("Model ${modelFile.name} score is ${((correct.toDouble() / questions.size) * 100).roundToInt()}%")
    }

}