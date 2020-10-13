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
package de.materna.fegen.core.generator

import de.materna.fegen.core.domain.*
import de.materna.fegen.core.isEmbeddable
import de.materna.fegen.core.isEntity
import de.materna.fegen.core.isProjection
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class FieldMgr(
        private val domainMgr: DomainMgr
) {

    /**
     * If a field cannot be resolved, the context provides information about where the error happened
     */
    abstract class ErrorContext {
        abstract fun context(): String
    }

    class FieldContext(private val clazz: Class<*>): ErrorContext() {
        override fun context() = "while resolving field of class ${clazz.canonicalName}"
    }

    class ParameterContext(private val method: Method): ErrorContext() {
        override fun context() = "while resolving parameter of method ${method.declaringClass.canonicalName}::${method.name}"
    }

    fun dtFieldFromType(
            name: String = "",
            type: Type,
            list: Boolean = false,
            optional: Boolean = false,
            justSettable: Boolean = false,
            context: ErrorContext
    ): DTField {
        val simpleType = SimpleType.fromType(type)
        return if (simpleType != null) {
            SimpleDTField(
                    name,
                    list,
                    optional,
                    justSettable,
                    simpleType
            )
        } else {
            when (type) {
                is Class<*> -> when {
                    type.isEnum -> EnumDTField(
                            name = name,
                            list = list,
                            optional = optional,
                            justSettable = justSettable,
                            type = domainMgr.enumMgr.resolveEnum(type)
                    )
                    type.isEntity -> EntityDTField(
                            name = name,
                            list = list,
                            optional = optional,
                            justSettable = justSettable,
                            type = domainMgr.entityMgr.class2Entity[type]
                                    ?: throw RuntimeException("Could not resolve entity type $type")
                    )
                    type.isEmbeddable -> EmbeddableDTField(
                            name = name,
                            justSettable = justSettable,
                            type = domainMgr.embeddableMgr.class2Embeddable[type]
                                    ?: throw RuntimeException("Could not resolve embedded type $type")
                    )
                    type.isProjection -> ProjectionDTField(
                            name = name,
                            list = list,
                            optional = optional,
                            justSettable = justSettable,
                            type = domainMgr.projectionMgr.class2Projection[type]
                                    ?: throw RuntimeException("Could not resolve projection type $type")

                    )
                    else -> PojoDTField(
                            name = name,
                            list = list,
                            optional = optional,
                            justSettable = justSettable,
                            type = Pojo(name = name, typeName = type.simpleName).apply {
                                fields = type.declaredFields.map { field ->  domainMgr.fieldMgr.dtFieldFromType(name = field.name, type = field.genericType, context = FieldContext(field.type)) }
                            }
                    )
                }
                is ParameterizedType -> {
                    if (!java.lang.Iterable::class.java.isAssignableFrom(type.rawType as Class<*>)) throw IllegalStateException("Cannot handle ${type}.")
                    // recursive call for list types (with boolean parameter 'list' set to true)
                    dtFieldFromType(
                            name,
                            type.actualTypeArguments.first(),
                            true,
                            false,
                            justSettable,
                            context
                    )
                }
                else -> throw IllegalStateException("UNKNOWN non-class '$name': ${type.typeName} & ${type::class.java.name} ${context.context()}")
            }
        }
    }
}