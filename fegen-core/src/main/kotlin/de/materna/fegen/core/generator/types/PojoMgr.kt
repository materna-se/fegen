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
import de.materna.fegen.core.domain.ClassProperty
import de.materna.fegen.core.domain.Pojo
import de.materna.fegen.core.generator.DomainMgr
import de.materna.fegen.core.generator.FieldMgr
import de.materna.fegen.core.log.FeGenLogger
import kotlin.RuntimeException

class PojoMgr(
        feGenConfig: FeGenConfig,
        logger: FeGenLogger,
        domainMgr: DomainMgr
) : ComplexTypeMgr(feGenConfig, logger, domainMgr) {

    private val class2Pojo = mutableMapOf<Class<*>, Pojo>()

    val pojos get() = class2Pojo.values.sortedBy { it.name }

    fun resolvePojo(type: Class<*>): Pojo {
        var result = class2Pojo[type]
        if (result == null) {
            result = Pojo(name = type.simpleName, typeName = type.simpleName)
            // It is crucial to insert the pojo into the map before resolving its fields
            class2Pojo[type] = result
            // because resolveFields may run into a stack overflow when encountering recursive types otherwise
            try {
                result.fields = resolveFields(type)
            } catch (ex: Exception) {
                throw RuntimeException("Failed to resolve fields of type ${type.canonicalName}", ex)
            }
        }
        return result
    }

    private fun resolveFields(type: Class<*>) =
            ClassProperty.forClass(type).asSequence().sortedBy { it.name }.map { property ->
                try {
                    domainMgr.fieldMgr.dtFieldFromType(
                            name = property.name,
                            optional = !property.notNull,
                            type = property.type,
                            context = FieldMgr.FieldContext(type)
                    )
                } catch (ex: Exception) {
                    throw RuntimeException("Failed to resolve field ${property.name}", ex)
                }
            }.toList()
}
