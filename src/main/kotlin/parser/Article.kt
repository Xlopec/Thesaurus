package parser

data class Article(val id: Int,
                   val title: String,
                   val text: String,
                   val raw: String?,
                   val timestamp: String) {

    init {
        require(title.isNotBlank()) { println("$this") }
        require(text.isNotBlank()) { println("$this") }
        require(timestamp.isNotBlank()) { println("$this") }
    }
}