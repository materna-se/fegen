/**
 * Copyright 2020 Materna Information & Communications SE
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.materna.fegen.core.domain

import java.lang.reflect.Type
import java.math.BigDecimal
import java.net.URI
import java.time.*
import java.util.*

interface ValueType

enum class SimpleType : ValueType {
    STRING, INTEGER, LONG, DOUBLE, UUID, BIGDECIMAL, BOOLEAN, DATE, DATETIME, ZONED_DATETIME, OFFSET_DATETIME, DURATION;

    companion object {
        fun fromType(type: Type): SimpleType? =
            when (type) {
                Boolean::class.java -> BOOLEAN
                java.lang.Long::class.java, 1L.javaClass -> LONG
                java.lang.Integer::class.java, 1.javaClass -> INTEGER
                java.lang.Double::class.java, 1.0.javaClass -> DOUBLE
                BigDecimal::class.java -> BIGDECIMAL
                java.util.UUID::class.java -> UUID
                LocalDate::class.java -> DATE
                LocalDateTime::class.java -> DATETIME
                ZonedDateTime::class.java -> ZONED_DATETIME
                OffsetDateTime::class.java -> OFFSET_DATETIME
                Duration::class.java -> DURATION
                String::class.java, URI::class.java -> STRING
                else -> null
            }
    }
}