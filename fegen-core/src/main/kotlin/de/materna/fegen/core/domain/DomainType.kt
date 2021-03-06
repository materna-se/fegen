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

import org.atteo.evo.inflector.English

sealed class DomainType: Type {
    val nameRest by lazy {
        nameRestOverride ?: English.plural(name.decapitalize())!!
    }

    var nameRestOverride: String? = null
}

sealed class ComplexType: DomainType() {

    var fields = emptyList<DTField>()

    private inline fun <reified T: DTField> filteredFields() = lazy { fields.filterIsInstance<T>() }

    open val simpleFields by filteredFields<SimpleDTField>()

    open val entityFields by filteredFields<EntityDTField>()

    open val embeddedFields by filteredFields<EmbeddableDTField>()

    open val enumFields by filteredFields<EnumDTField>()

    open val nonComplexFields by lazy {
        simpleFields + enumFields + embeddedFields
    }

    /**
     * Fields for which no reasonable default values exist.
     * Default values exist for simple types, enums, lists and optional values.
     */
    open val nonDefaultFields by lazy {
        entityFields.filter { !it.list && !it.optional }
    }

}

data class EntityType(
        override var name: String,
        var searches: List<Search> = emptyList(),
        var exported: Boolean = false
): ComplexType() {
    override val isPlain: Boolean
        get() = false
}

data class EmbeddableType(
        override var name: String
): ComplexType() {
    override val isPlain: Boolean
        get() = false
}

data class ProjectionType(
        override var name: String,
        val projectionName: String,
        val baseProjection: Boolean,
        val fullProjectionName: String,
        /**
         * The type this type is a projection of.
         */
        val parentType: EntityType
): ComplexType() {

    override val isPlain: Boolean
        get() = false

    /**
     * This is for spEL fueled fields in projections. It does not contain
     * the fields of the returnType type.
     */
    override val simpleFields by lazy {
        fields.filterIsInstance(SimpleDTField::class.java)
    }

    /**
     * This is for spEL fueled fields in projections. It does not contain
     * the fields of the returnType type.
     */
    override val entityFields by lazy {
        fields.filterIsInstance(EntityDTField::class.java)
    }

    /**
     * This is for spEL fueled fields in projections. It does not contain
     * the fields of the returnType type.
     */
    override val enumFields by lazy {
        fields.filterIsInstance(EnumDTField::class.java)
    }

    val projectionFields by lazy {
        fields.filterIsInstance(ProjectionDTField::class.java)
    }

    /**
     * This is for spEL fueled fields in projections. It does not contain
     * the fields of the returnType type.
     */
    val complexFields by lazy {
        entityFields + projectionFields
    }

    /**
     * This is for spEL fueled fields in projections. It does not contain
     * the fields of the returnType type.
     */
    override val nonComplexFields by lazy {
        simpleFields + enumFields
    }

    /**
     * Fields that are not part of the parent type
     */
    val projectionSpecificFields by lazy {
        fields.filter { ownField ->
            !parentType.nonComplexFields.any { parentField -> ownField.name == parentField.name }
        }
    }
}

data class EnumType(
        override var name: String,
        val constants: List<String>
): DomainType(), ValueType {
    override val isPlain: Boolean
        get() = true
}

data class Pojo(override val name: String, val typeName: String): ComplexType() {
    override fun toString(): String {
        return "Pojo(name=$name, typeName=$typeName, fields=$fields"
    }

    override val isPlain: Boolean
        get() = true
}

