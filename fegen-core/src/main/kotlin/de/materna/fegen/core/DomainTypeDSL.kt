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
package de.materna.fegen.core

import org.atteo.evo.inflector.English
import java.util.*

sealed class DomainType {
    abstract var name: String

    val nameRest by lazy {
        nameRestOverride ?: English.plural(name.decapitalize())
    }

    var nameRestOverride: String? = null
}

sealed class ComplexType: DomainType() {
    abstract var fields: List<DTReference>
}

data class EntityType(
    override var name: String,
    override var fields: List<DTReference> = emptyList(),
    var searches: List<Search> = emptyList(),
    var customEndpoints: List<CustomEndpoint> = emptyList()
): ComplexType() {

    val simpleFields by lazy {
        fields.filterIsInstance(DTRSimple::class.java)
    }

    val entityFields by lazy {
        fields.filterIsInstance(DTREntity::class.java)
    }

    val enumFields by lazy {
        fields.filterIsInstance(DTREnum::class.java)
    }

    val nonComplexFields by lazy {
        simpleFields + enumFields
    }
}

data class ProjectionType(
    override var name: String,
    val projectionName: String,
    val baseProjection: Boolean,
    /**
         * The type this type is a projection of.
         */
        val parentType: EntityType,
    override var fields: List<DTReference> = emptyList()
): ComplexType() {

    /**
     * This is for spEL fueled fields in projections. It does not contain
     * the fields of the returnType type.
     */
    val simpleFields by lazy {
        fields.filterIsInstance(DTRSimple::class.java)
    }

    /**
     * This is for spEL fueled fields in projections. It does not contain
     * the fields of the returnType type.
     */
    val entityFields by lazy {
        fields.filterIsInstance(DTREntity::class.java)
    }

    /**
     * This is for spEL fueled fields in projections. It does not contain
     * the fields of the returnType type.
     */
    val enumFields by lazy {
        fields.filterIsInstance(DTREnum::class.java)
    }

    val projectionFields by lazy {
        fields.filterIsInstance(DTRProjection::class.java)
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
    val nonComplexFields by lazy {
        simpleFields + enumFields
    }
}

data class EnumType(
        override var name: String,
        val constants: List<String>
): DomainType(), ValueType

data class Search(
    val name: String,
    val parameters: List<DTRValue>,
    val list: Boolean,
    val paging: Boolean,
    val returnType: EntityType,
    val inRepo: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Search) return false
        return name == other.name &&
                list == other.list &&
                paging == other.paging &&
                returnType.name == other.returnType.name
    }

    override fun hashCode(): Int {
        return Objects.hash(Search::class.java, name, list, paging, returnType.name)
    }

    override fun toString(): String {
        return "Search(name=$name, list=$list, paging=$paging, returnType=${returnType.name})"
    }
}

enum class EndpointMethod {
    GET, POST, PUT, PATCH, DELETE
}

data class CustomEndpoint(
    val baseUri: String,
    val name: String,
    val parentType: EntityType,
    val method: EndpointMethod,
    val pathVariables: List<DTRValue>,
    val requestParams: List<DTRValue>,
    val body: DTREntity?,
    val list: Boolean,
    val paging: Boolean,
    val returnType: DomainType?,
    val canReceiveProjection: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CustomEndpoint) return false
        return name == other.name &&
                list == other.list &&
                parentType.name == other.parentType.name &&
                method == other.method &&
                pathVariables == other.pathVariables &&
                requestParams == other.requestParams &&
                body == other.body &&
                paging == other.paging &&
                returnType?.name == other.returnType?.name
    }

    override fun hashCode(): Int {
        return Objects.hash(Search::class.java, name, parentType.name, method, pathVariables, requestParams, body, list, paging, returnType?.name)
    }

    override fun toString(): String {
        return "CustomEndpoint(name=$name, parentType=${parentType.name}, method=$method, pathVariables=$pathVariables, requestParams=$requestParams, body=$body, list=$list, paging=$paging${returnType?.let { ", returnType=${returnType.name}" } ?: ""})"
    }
}

sealed class DTReference {
    abstract val name: String
    abstract val list: Boolean
    abstract val optional: Boolean
    abstract val justSettable: Boolean
}

sealed class DTRComplex: DTReference() {
    abstract val type: ComplexType
}

sealed class DTRValue: DTReference() {
    abstract val type: ValueType
}

data class DTRSimple(
        override val name: String,
        override val list: Boolean,
        override val optional: Boolean,
        override val justSettable: Boolean,
        override val type: SimpleType
): DTRValue()

data class DTREntity(
        override val name: String,
        override val list: Boolean = false,
        override val optional: Boolean = false,
        override val justSettable: Boolean = false,
        override val type: EntityType

): DTRComplex() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DTREntity) return false
        return name == other.name &&
                list == other.list &&
                justSettable == other.justSettable &&
                type.name == other.type.name
    }

    override fun hashCode(): Int {
        return Objects.hash(DTREntity::class.java, name, list, justSettable, type.name)
    }

    override fun toString(): String {
        return "DTREntity(name=$name, list=$list, justSettable=$justSettable, type=${type.name})"
    }
}

data class DTRProjection(
        override val name: String,
        override val list: Boolean,
        override val optional: Boolean,
        override val justSettable: Boolean,
        override val type: ProjectionType
): DTRComplex() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DTREntity) return false
        return name == other.name &&
                list == other.list &&
                justSettable == other.justSettable &&
                type.name == other.type.name
    }

    override fun hashCode(): Int {
        return Objects.hash(DTRProjection::class.java, name, list, justSettable, type.name)
    }

    override fun toString(): String {
        return "DTRProjection(name=$name, list=$list, justSettable=$justSettable, type=${type.name})"
    }
}

data class DTREnum(
        override val name: String,
        override val list: Boolean,
        override val optional: Boolean,
        override val justSettable: Boolean,
        override val type: EnumType
): DTRValue()

interface ValueType

enum class SimpleType: ValueType {
    STRING, INTEGER, LONG, DOUBLE, UUID, BIGDECIMAL, BOOLEAN, DATE, DATETIME, ZONED_DATETIME, OFFSET_DATETIME, DURATION
}