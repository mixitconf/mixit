package mixit.util

data class Year(val value: Int, val selected: Boolean, val path: String, val talk: Boolean)
object YearSelector {

    private val YEARS = listOf(2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2021, 2022, 2023, 2024, 2025).sortedDescending()

    fun create(selectedYear: Int, path: String, talk: Boolean = false): List<Year> =
        YEARS.map { Year(it, it == selectedYear, path, talk) }
}
