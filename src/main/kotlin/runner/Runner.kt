package runner

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import parser.DataSetGeneratorCommand
import word2vec.EvaluateCommand
import word2vec.Word2VecToDictCommand
import word2vec.Word2VecTrainCommand

class Thesaurus: CliktCommand(name = "thesaurus") {
    override fun run() = Unit
}


fun main(args: Array<String>) = Thesaurus().subcommands(Word2VecTrainCommand(), Word2VecToDictCommand(),
        EvaluateCommand(), DataSetGeneratorCommand()).main(args)