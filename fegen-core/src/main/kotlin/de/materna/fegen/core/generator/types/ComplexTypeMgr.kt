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

import com.fasterxml.classmate.members.ResolvedField
import com.fasterxml.classmate.members.ResolvedMethod
import de.materna.fegen.core.*
import de.materna.fegen.core.domain.*
import de.materna.fegen.core.log.DiagnosticsLevel
import de.materna.fegen.core.log.FeGenLogger
import de.materna.fegen.core.generator.BaseMgr
import de.materna.fegen.core.generator.DomainMgr

abstract class ComplexTypeMgr(
        feGenConfig: FeGenConfig,
        protected val logger: FeGenLogger,
        domainMgr: DomainMgr
): BaseMgr(feGenConfig, domainMgr) {

    private fun candidateFields(clazz: Class<*>): List<ResolvedMethod> =
        clazz.getters.filter { m ->
            val field = m.field
            if (field == null) {
                true
            } else {
                if (field.notIgnored && m.notIgnored) {
                    true
                } else {
                    field.setter?.writable ?: false
                }
            }
        }

    private fun justSettable(method: ResolvedMethod, field: ResolvedField?): Boolean {
        if (field == null) {
            return false
        }
        if (method.notIgnored && field.notIgnored) {
            return false
        }
        return field.setter?.writable ?: false
    }

    protected open fun omitField(field: ResolvedMethod, complexType: ComplexType): Boolean = false

    protected open fun checkField(owningClass: Class<*>, field: ResolvedMethod) {}

    protected open fun parentField(fieldName: String, owningType: ComplexType): EntityDTField? = null

    private fun isVersion(field: ResolvedMethod) = field.fieldName == "version"

    private fun isByteArray(field: ResolvedMethod) = field.fieldType.typeName == "byte[]"

    private val idFirstSort = { field: ResolvedMethod -> if (field.fieldName == "id") "" else field.fieldName }

    private fun checkImplicitNullable(method: ResolvedMethod, field: ResolvedField, clazz: Class<*>) {
        val name = method.fieldName
        val type = method.fieldType
        val isEmbeddable = (type as? Class<*>)?.isEmbeddable ?: false
        val isImplicitlyNullable = { !field.required && !field.explicitOptional && !isEmbeddable }
        feGenConfig.implicitNullable.check(logger, isImplicitlyNullable) { print ->
            print("Field \"$name\" in entity \"${clazz.canonicalName}\" is implicitly nullable.")
            print("    Please add a @Nullable annotation if this is intentional")
            print("    or add a @NotNull annotation to forbid null values")
            if (feGenConfig.implicitNullable == DiagnosticsLevel.WARN) {
                print("    Set implicitNullable to ALLOW in FeGen's build configuration to hide this warning")
            } else {
                print("    Set implicitNullable to WARN to continue the build despite missing @Nullable annotations")
            }
        }
    }

    private fun methodToDtField(owningClass: Class<*>, owningType: ComplexType, method: ResolvedMethod): DTField {
        val name = method.fieldName
        // TODO find out, why I have to use classmate here. Since e.g. Project in plan-info implements Identifiable the return type of getId is Serializable ?!?
        // use either the raw return type if it is a overriden type parameter, or else the return type.
        val type = method.fieldType
        val field = method.field
        checkField(owningClass, method)
        if (field != null) {
            checkImplicitNullable(method, field, owningClass)
        }
        val parentField = parentField(name, owningType)

        val justSettable = justSettable(method, field)

        return domainMgr.fieldMgr.dtFieldFromType(
                owningClass.canonicalName,
                name,
                type,
                list = false,
                optional = field?.optional ?: parentField?.optional ?: false,
                justSettable = justSettable
        )
    }

    protected fun addFields(owningClass: Class<*>, owningType: ComplexType) {
        owningType.fields = candidateFields(owningClass)
                .asSequence()
                .filter { !omitField(it, owningType) }
                .filter { !isVersion(it) }
                .filter { !isByteArray(it) }
                .sortedBy(idFirstSort)
                .map { methodToDtField(owningClass, owningType, it) }
                .toList()
    }
}
