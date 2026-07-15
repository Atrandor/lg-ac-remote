package de.tobias.lgacremote

/**
 * LG A/C IR-Protokoll-Encoder (28 Bit).
 * Portiert aus IRremoteESP8266 (ir_LG.cpp/h), GPL-Referenz nur als Spezifikation genutzt.
 *
 * Bit-Layout (MSB zuerst, 28 Bit):
 *   Sign(8) = 0x88 | Power(2) | frei(3) | Mode(3) | Temp(4) | Fan(4) | Checksum(4)
 * Checksumme = Summe der 6 Nibbles oberhalb der Checksumme, & 0xF.
 */
object LgAcEncoder {

    // Modi
    const val MODE_COOL = 0
    const val MODE_DRY = 1
    const val MODE_FAN = 2
    const val MODE_AUTO = 3
    const val MODE_HEAT = 4

    // Lüfterstufen
    const val FAN_LOWEST = 0
    const val FAN_LOW = 1
    const val FAN_MEDIUM = 2
    const val FAN_MAX = 4
    const val FAN_AUTO = 5

    const val MIN_TEMP = 16
    const val MAX_TEMP = 30

    // Feste Sondercodes
    const val OFF_COMMAND = 0x88C0051L
    const val LIGHT_TOGGLE = 0x88C00A6L
    const val SWING_V_TOGGLE = 0x8810001L
    const val SWING_V_SWING = 0x8813149L
    const val SWING_V_OFF = 0x881315AL

    private const val SIGNATURE = 0x88L
    private const val TEMP_ADJUST = 15
    private const val POWER_ON = 0L

    /** Erzeugt den 28-Bit-Zustandscode für "Ein" mit Modus/Temperatur/Lüfter. */
    fun buildState(mode: Int, tempC: Int, fan: Int): Long {
        val temp = tempC.coerceIn(MIN_TEMP, MAX_TEMP) - TEMP_ADJUST
        var state = (SIGNATURE shl 20) or
                (POWER_ON shl 18) or
                ((mode.toLong() and 0x7) shl 12) or
                ((temp.toLong() and 0xF) shl 8) or
                ((fan.toLong() and 0xF) shl 4)
        state = state or checksum(state)
        return state
    }

    /** Nibble-Checksumme der oberen 24 Bit. */
    fun checksum(state: Long): Long {
        val data = state shr 4
        var sum = 0L
        for (i in 0 until 6) sum += (data shr (i * 4)) and 0xF
        return sum and 0xF
    }

    /**
     * Wandelt einen 28-Bit-Code in ein Android-IR-Pattern (Mikrosekunden, an/aus alternierend).
     * @param lg2 true = LG2-Timing (moderne Geräte, z.B. AKB75215403-Fernbedienungen),
     *            false = klassisches LG-Timing (ältere Geräte / GE6711AR2853M).
     */
    fun toPattern(state: Long, lg2: Boolean = true): IntArray {
        val hdrMark = if (lg2) 3200 else 8500
        val hdrSpace = if (lg2) 9900 else 4250
        val bitMark = if (lg2) 480 else 550
        val oneSpace = 1600
        val zeroSpace = 550
        val gap = 39750

        val pattern = ArrayList<Int>(2 + 28 * 2 + 2)
        pattern.add(hdrMark)
        pattern.add(hdrSpace)
        for (i in 27 downTo 0) {
            pattern.add(bitMark)
            pattern.add(if ((state shr i) and 1L == 1L) oneSpace else zeroSpace)
        }
        pattern.add(bitMark) // Abschluss-Mark
        pattern.add(gap)
        return pattern.toIntArray()
    }

    const val CARRIER_FREQ_HZ = 38000
}
