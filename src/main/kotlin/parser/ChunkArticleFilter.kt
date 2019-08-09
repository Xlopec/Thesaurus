package parser

import info.bliki.wiki.dump.IArticleFilter
import info.bliki.wiki.dump.Siteinfo
import info.bliki.wiki.dump.WikiArticle
import info.bliki.wiki.filter.PlainTextConverter
import info.bliki.wiki.model.WikiModel

class ChunkArticleFilter(private val chunkSize: Int,
                         private val onChunkParsed: (List<Article>) -> Unit,
                         private val onArticleSkipped: (Int) -> Unit) : IArticleFilter {

     companion object {
        private val CONVERTER = PlainTextConverter()
        private val WIKI_MODEL = WikiModel("https://www.mywiki.com/wiki/\${image}",
                "https://www.mywiki.com/wiki/\${title}")

        val REPLACE_MATCHER = Regex("([-&&\\p{Punct}&&[^'`]\t|/])|(Template:)" +
                "|((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])" +
                "|(right)|(\\d+px)|(thumb)")
    }

    private val buffer: MutableList<Article> = ArrayList(chunkSize)

    init {
        require(chunkSize > 0)
    }

    fun onFlush() {
        if (buffer.isNotEmpty()) {
            onChunkParsed(ArrayList(buffer))
            buffer.clear()
        }
    }

    override fun process(article: WikiArticle, siteinfo: Siteinfo) {

        val sb = StringBuilder()

        WIKI_MODEL.render(CONVERTER, article.text, sb, true, true)

        val s = sb.replace(REPLACE_MATCHER, "").replace("\n", " ")

        val id = article.id.toInt()

        if (s.isBlank() || s.isEmpty()) {
            onArticleSkipped(id)
            return
        }


        buffer += Article(id, article.title.trim(), s, s, article.timeStamp)

        if (buffer.size > chunkSize) {
            onChunkParsed(ArrayList(buffer))
            buffer.clear()
        }
    }

}
