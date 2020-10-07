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

import java.util.*

sealed class DTField {
    abstract val name: String
    abstract val list: Boolean
    abstract val optional: Boolean
    abstract val justSettable: Boolean
}

sealed class ComplexDTField: DTField() {
    abstract val type: ComplexType
}

sealed class ValueDTField: DTField() {
    abstract val type: ValueType
}

data class SimpleDTField(
        override val name: String,
        override val list: Boolean,
        override val optional: Boolean,
        override val justSettable: Boolean,
        override val type: SimpleType
): ValueDTField()

data class EntityDTField(
        override val name: String,
        override val list: Boolean = false,
        override val optional: Boolean = false,
        override val justSettable: Boolean = false,
        override val type: EntityType

): ComplexDTField() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntityDTField) return false
        return name == other.name &&
                list == other.list &&
                justSettable == other.justSettable &&
                type.name == other.type.name
    }

    override fun hashCode(): Int {
        return Objects.hash(EntityDTField::class.java, name, list, justSettable, type.name)
    }

    override fun toString(): String {
        return "EntityDTField(name=$name, list=$list, justSettable=$justSettable, type=${type.name})"
    }
}

data class EmbeddableDTField(
        override val name: String,
        override val justSettable: Boolean = false,
        override val type: EmbeddableType
): ComplexDTField() {

    override val list get() = false

    // If every field of an embeddable is null,
    // the embeddable itself may always be null
    override val optional get() = true
}

data class ProjectionDTField(
        override val name: String,
        override val list: Boolean,
        override val optional: Boolean,
        override val justSettable: Boolean,
        override val type: ProjectionType
): ComplexDTField() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EntityDTField) return false
        return name == other.name &&
                list == other.list &&
                justSettable == other.justSettable &&
                type.name == other.type.name
    }

    override fun hashCode(): Int {
        return Objects.hash(ProjectionDTField::class.java, name, list, justSettable, type.name)
    }

    override fun toString(): String {
        return "ProjectionDTField(name=$name, list=$list, justSettable=$justSettable, type=${type.name})"
    }
}

data class EnumDTField(
        override val name: String,
        override val list: Boolean,
        override val optional: Boolean,
        override val justSettable: Boolean,
        override val type: EnumType
): ValueDTField()

data class DTPojo(override val name: String,
                  var simpleFields: List<SimpleDTField> = listOf(),
                  var enumFields: List<EnumDTField> = listOf()
): ValueType
