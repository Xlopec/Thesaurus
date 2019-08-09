package format

import speech.Declension
import speech.Declension.*
import speech.SpeechPart

val SpeechPart.humanReadableString: String
    get() = when (this) {
        is SpeechPart.Noun -> "$word (іменник, ${declension.humanReadable} відмінок)"
        is SpeechPart.Adjective -> "$word (прикметник, ${declension.humanReadable} відмінок)"
        is SpeechPart.Numeral -> "$word (числівник, ${declension.humanReadable} відмінок)"
        is SpeechPart.Verb -> "$word (дієслово)"
        is SpeechPart.Adverb -> "$word (прислівник)"
        is SpeechPart.Conjunction -> "$word (сполучник)"
        is SpeechPart.VerbalParticiple -> "$word (дієприслівник)"
        is SpeechPart.Part -> "$word (дієприкметник)"
        is SpeechPart.Interjection -> "$word (вигук)"
        is SpeechPart.Preposition -> "$word (прийменник)"
    }

fun toHumanReadableString(speechPart: SpeechPart, closest: Iterable<SpeechPart>): String {
    return "${speechPart.humanReadableString}, cхожі слова: ${closest.joinToString { it.word }}"
}

val Declension.humanReadable: String
    get() = when (this) {
        NOMINATIVE -> "називний"
        GENETIVE -> "родовий"
        DATIVE -> "давальний"
        ACCUSATIVE -> "знахідний"
        ABLATIVE -> "орудний"
        VOCATIVE -> "кличний"
        LOCATIVE -> "місцевий"
    }
