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
package de.materna.fegen.core.generator.types

import de.materna.fegen.core.*
import de.materna.fegen.core.domain.*
import de.materna.fegen.core.domain.ComplexType
import de.materna.fegen.core.log.DiagnosticsLevel
import de.materna.fegen.core.log.FeGenLogger
import de.materna.fegen.core.generator.BaseMgr
import de.materna.fegen.core.generator.DomainMgr
import de.materna.fegen.core.generator.FieldMgr

abstract class ComplexTypeMgr(
        feGenConfig: FeGenConfig,
        protected val logger: FeGenLogger,
        domainMgr: DomainMgr
): BaseMgr(feGenConfig, domainMgr) {

    protected open fun omitField(field: ClassProperty, complexType: ComplexType): Boolean = false

    protected open fun checkField(field: ClassProperty) {}

    protected open fun parentField(fieldName: String, owningType: ComplexType): EntityDTField? = null

    private fun isVersion(field: ClassProperty) = field.name == "version"

    private fun isByteArray(field: ClassProperty) = field.type.typeName == "byte[]"

    private val idFirstSort = { field: ClassProperty -> if (field.name == "id") "" else field.name }

    protected open fun checkImplicitNullable(property: ClassProperty) {
        val name = property.name
        val type = property.type
        val isEmbeddable = (type as? Class<*>)?.isEmbeddable ?: false
        val isImplicitlyNullable = { !property.notNull && !property.explicitNullable && !isEmbeddable }
        feGenConfig.implicitNullable.check(logger, isImplicitlyNullable) { print ->
            print("Field \"$name\" in entity \"${property.owningClass.canonicalName}\" is implicitly nullable.")
            print("    Please add a @Nullable annotation if this is intentional")
            print("    or add a @NotNull annotation to forbid null values")
            if (feGenConfig.implicitNullable == DiagnosticsLevel.WARN) {
                print("    Set implicitNullable to ALLOW in FeGen's build configuration to hide this warning")
            } else {
                print("    Set implicitNullable to WARN to continue the build despite missing @Nullable annotations")
            }
        }
    }

    private fun methodToDtField(owningType: ComplexType, property: ClassProperty): DTField {
        // TODO find out, why I have to use classmate here. Since e.g. Project in plan-info implements Identifiable the return type of getId is Serializable ?!?
        // use either the raw return type if it is a overriden type parameter, or else the return type.
        checkField(property)
        checkImplicitNullable(property)
        val parentField = parentField(property.name, owningType)

        val justSettable = property.justSettable

        return domainMgr.fieldMgr.dtFieldFromType(
                property.name,
                property.type,
                list = false,
                optional = parentField?.optional ?: !property.notNull,
                justSettable = justSettable,
                context = FieldMgr.FieldContext(property.owningClass)
        )
    }

    protected fun addFields(owningClass: Class<*>, owningType: ComplexType) {
        owningType.fields = ClassProperty.forClass(owningClass)
                .asSequence()
                .filter { !omitField(it, owningType) }
                .filter { !isVersion(it) }
                .filter { !isByteArray(it) }
                .sortedBy(idFirstSort)
                .map { methodToDtField(owningType, it) }
                .toList()
    }
}
