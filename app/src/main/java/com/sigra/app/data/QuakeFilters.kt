package com.sigra.app.data

enum class QuakeOrder(val apiValue: String, val label: String) {
    DATE("fecha", "Más recientes"),
    DISTANCE("distancia", "Más cercanos"),
    MAGNITUDE("magnitud", "Mayor magnitud"),
}

data class QuakeFilters(
    val radiusKm: Int = 0,
    val source: String? = null,
    val order: QuakeOrder = QuakeOrder.DATE,
    val hours: Int = 120,
    val minMagnitude: Double? = null,
) {
    val periodLabel: String get() = periods[hours] ?: "$hours horas"
    val summary: String get() = listOfNotNull(source ?: "6 fuentes oficiales",
        if (radiusKm == 0) "Global" else "$radiusKm km", periodLabel,
        minMagnitude?.let { "M $it+" }).joinToString(" · ")

    companion object {
        val sources = listOf("SGC", "USGS", "EMSC", "GEONET", "GEOFON", "INGV")
        val periods = linkedMapOf(24 to "24 horas", 72 to "3 días", 120 to "5 días", 168 to "7 días", 336 to "14 días")
        val magnitudes = listOf(null, 2.0, 3.0, 4.0, 5.0, 6.0)
        val radii = listOf(0, 50, 100, 250, 500, 1000, 2000, 5000)

        fun fromStoredValues(radiusKm: Int?, source: String?, order: String?, hours: Int? = null, minMagnitude: Double? = null): QuakeFilters = QuakeFilters(
            radiusKm = radiusKm?.takeIf { it in radii } ?: 0,
            source = source?.takeIf { it in sources },
            order = QuakeOrder.entries.firstOrNull { it.apiValue == order } ?: QuakeOrder.DATE,
            hours = hours?.takeIf { it in periods } ?: 120,
            minMagnitude = minMagnitude?.takeIf { it in magnitudes },
        )
    }

    fun forLocation(hasLocation: Boolean): QuakeFilters = if (hasLocation) this else copy(
        radiusKm = 0,
        order = order.takeUnless { it == QuakeOrder.DISTANCE } ?: QuakeOrder.DATE,
    )
}
