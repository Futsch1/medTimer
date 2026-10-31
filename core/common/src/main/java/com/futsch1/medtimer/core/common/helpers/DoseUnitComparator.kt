package com.futsch1.medtimer.core.common.helpers

import java.util.Locale

object DoseUnitComparator {
    internal data class UnitFamily(val singulars: Set<String>, val plurals: Set<String>)

    internal val families = listOf(
        family(listOf("tablet"), listOf("tablets")),
        family(listOf("tablette"), listOf("tabletten")),
        family(listOf("tablette"), listOf("tablettes")),
        family(listOf("δισκίο"), listOf("δισκία")),
        family(listOf("tableta"), listOf("tabletas")),
        family(listOf("comprimido"), listOf("comprimidos")),
        family(listOf("comprimé"), listOf("comprimés")),
        family(listOf("pastilla"), listOf("pastillas")),
        family(listOf("tablete"), listOf("tabletes")),
        family(listOf("tabletta"), listOf("tabletták")),
        family(listOf("tabletka"), listOf("tabletki", "tabletek")),
        family(listOf("tableta"), listOf("tablety")),
        family(listOf("таблетка", "таблетку"), listOf("таблетки", "таблеток")),
        family(listOf("таблетці"), listOf("таблетки")),
        family(listOf("tabletti"), listOf("tabletit")),
        family(listOf("tablett"), listOf("tabletter")),
        family(listOf("tablett"), listOf("tabletid")),
        family(listOf("tafla"), listOf("töflur")),
        family(listOf("tablet"), listOf("tabletten")),
        family(listOf("tablet"), listOf("tabletter")),
        family(listOf("tablet"), listOf("tablettene")),
        family(listOf("tablet"), listOf("tabletler")),
        family(listOf("טבליה"), listOf("טבליות")),
        family(listOf("قرص"), listOf("أقراص")),
        family(listOf("حبّة"), listOf("حبوب")),
        family(listOf("மாத்திரை"), listOf("மாத்திரைகள்")),
        family(listOf("capsule"), listOf("capsules")),
        family(listOf("капсула"), listOf("капсули")),
        family(listOf("κάψουλα"), listOf("κάψουλες")),
        family(listOf("kapsel"), listOf("kapslid")),
        family(listOf("kapsül"), listOf("kapsüller")),
        family(listOf("capsula"), listOf("capsule")),
        family(listOf("pilule"), listOf("pilules")),
        family(listOf("kapsel"), listOf("kapseln")),
        family(listOf("cápsula"), listOf("cápsulas")),
        family(listOf("capsula"), listOf("capsule")),
        family(listOf("gélule"), listOf("gélules")),
        family(listOf("kapszula"), listOf("kapszulák")),
        family(listOf("kapsułka"), listOf("kapsułki")),
        family(listOf("капсула"), listOf("капсулы", "капсул")),
        family(listOf("kapsula"), listOf("kapsuly")),
        family(listOf("kapsel"), listOf("kapsler")),
        family(listOf("kapsel"), listOf("kapslar")),
        family(listOf("kapseli"), listOf("kapselit")),
        family(listOf("kapsulė"), listOf("kapsulės")),
        family(listOf("קפסולה"), listOf("קפסולות")),
        family(listOf("كبسولة"), listOf("كبسولات")),
        family(listOf("காப்ஸ்யூல்"), listOf("காப்ஸ்யூல்கள்")),
        family(listOf("pill"), listOf("pills")),
        family(listOf("pille"), listOf("pillen")),
        family(listOf("píldora"), listOf("píldoras")),
        family(listOf("pilula"), listOf("pilulas")),
        family(listOf("pílula"), listOf("pílulas")),
        family(listOf("pigułka"), listOf("pigułki")),
        family(listOf("пігулка"), listOf("пігулки")),
        family(listOf("pille"), listOf("piller")),
        family(listOf("pilleri"), listOf("pillerit")),
        family(listOf("drop"), listOf("drops")),
        family(listOf("goutte"), listOf("gouttes")),
        family(listOf("gota"), listOf("gotas")),
        family(listOf("goccia"), listOf("gocce")),
        family(listOf("druppel"), listOf("druppels")),
        family(listOf("dråbe"), listOf("dråber")),
        family(listOf("dråpe"), listOf("dråper")),
        family(listOf("droppe"), listOf("droppar")),
        family(listOf("tipa"), listOf("tipas")),
        family(listOf("kropla"), listOf("krople")),
        family(listOf("капля"), listOf("капли", "капель")),
        family(listOf("крапля"), listOf("краплі")),
        family(listOf("kapka"), listOf("kapky")),
        family(listOf("damla"), listOf("damlalar")),
        family(listOf("טיפה"), listOf("טיפות")),
        family(listOf("قطرة"), listOf("قطرات")),
        family(listOf("துளி"), listOf("துளிகள்")),
        family(listOf("dose"), listOf("doses")),
        family(listOf("dosis"), listOf("dosissen")),
        family(listOf("dosis"), listOf("dosis")),
        family(listOf("dávka"), listOf("dávky")),
        family(listOf("doza"), listOf("doze")),
        family(listOf("доза"), listOf("дозы", "доз")),
        family(listOf("доза"), listOf("дозы")),
        family(listOf("dos"), listOf("doser")),
        family(listOf("annos"), listOf("annokset")),
        family(listOf("skammtur"), listOf("skammtar")),
        family(listOf("dosage"), listOf("dosages")),
        family(listOf("جرعة"), listOf("جرعات")),
        family(listOf("מנה"), listOf("מנות")),
        family(listOf("டோஸ்"), listOf("டோஸ்கள்")),
        family(listOf("unit"), listOf("units")),
        family(listOf("einheit"), listOf("einheiten")),
        family(listOf("unité"), listOf("unités")),
        family(listOf("unidad"), listOf("unidades")),
        family(listOf("unità"), listOf("unità")),
        family(listOf("unidade"), listOf("unidades")),
        family(listOf("eenheid"), listOf("eenheden")),
        family(listOf("enhet"), listOf("enheter")),
        family(listOf("yksikkö"), listOf("yksiköt")),
        family(listOf("jednotka"), listOf("jednotky")),
        family(listOf("jednostka"), listOf("jednostki")),
        family(listOf("единица"), listOf("единицы")),
        family(listOf("одиниця"), listOf("одиниці")),
        family(listOf("ühik"), listOf("ühikud")),
        family(listOf("birim"), listOf("birimler")),
        family(listOf("eining"), listOf("einingar")),
        family(listOf("יחידה"), listOf("יחידות")),
        family(listOf("وحدة"), listOf("وحدات")),
        family(listOf("அலகு"), listOf("அலகுகள்")),
        family(listOf("patch"), listOf("patches")),
        family(listOf("parche"), listOf("parches")),
        family(listOf("cerotto"), listOf("cerotti")),
        family(listOf("plåster"), listOf("plåster")),
        family(listOf("plasture"), listOf("plasturi")),
        family(listOf("пластир"), listOf("пластирі")),
        family(listOf("пластир"), listOf("пластири")),
        family(listOf("spray"), listOf("sprays")),
        family(listOf("sprühstoß"), listOf("sprühstöße")),
        family(listOf("pulverización"), listOf("pulverizaciones")),
        family(listOf("spruzzo"), listOf("spruzzi")),
        family(listOf("sprayuri"), listOf("spray-uri")),
        family(listOf("puff"), listOf("puffs")),
        family(listOf("huff"), listOf("hübe")),
    )
    private val familyByForm = families.flatMap { family ->
        (family.singulars + family.plurals).map { it to family }
    }.groupBy({ it.first }, { it.second })

    /** Returns a shared comparison form, or null when the units differ. */
    fun compare(first: String, second: String): String? {
        val firstTrimmed = first.trim()
        val secondTrimmed = second.trim()
        val firstNormalized = normalize(firstTrimmed)
        val secondNormalized = normalize(secondTrimmed)
        if (firstNormalized == secondNormalized) return firstNormalized

        val family = familyByForm[firstNormalized]
            ?.firstOrNull { it in familyByForm[secondNormalized].orEmpty() }
            ?: return null
        return when {
            firstNormalized in family.plurals -> firstTrimmed
            secondNormalized in family.plurals -> secondTrimmed
            else -> secondTrimmed
        }
    }

    private fun normalize(unit: String): String = unit.lowercase(Locale.ROOT)

    private fun family(singulars: List<String>, plurals: List<String>) =
        UnitFamily(singulars.toSet(), plurals.toSet())
}
