package speech

import arrow.core.Option

enum class Declension(val raw: String, val decletion: String) {

    NOMINATIVE("v_naz", "називний"),
    GENETIVE("v_rod", "родовий"),
    DATIVE("v_dav", "давальний"),
    ACCUSATIVE("v_zna", "знахідний"),
    ABLATIVE("v_oru", "орудний"),
    VOCATIVE("v_kly", "кличний"),
    LOCATIVE("v_mis", "місцевий")
}

fun declension(lemma: Lemma): Option<Declension> {
    return Option.fromNullable(Declension.values().find { it.raw == lemma.value })
}