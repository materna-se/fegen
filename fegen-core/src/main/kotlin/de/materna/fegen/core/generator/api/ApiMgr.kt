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
package de.materna.fegen.core.generator.api

import de.materna.fegen.core.FeGenConfig
import de.materna.fegen.core.generator.BaseMgr
import de.materna.fegen.core.generator.DomainMgr
import de.materna.fegen.core.isEntity
import de.materna.fegen.core.isProjection
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

abstract class ApiMgr(
        feGenConfig: FeGenConfig,
        domainMgr: DomainMgr
) : BaseMgr(feGenConfig, domainMgr) {

    private fun isSupportedAsParameter(type: Type): Boolean {
        return when (type) {
            is Class<*> -> !type.isEntity && !type.isProjection
            is ParameterizedType -> {
                if (!java.lang.Iterable::class.java.isAssignableFrom(type.rawType as Class<*>)) return false
                // recursive call for list types (with boolean parameter 'list' set to true)
                isSupportedAsParameter(type.actualTypeArguments.first())
            }
            else -> false
        }
    }

    protected fun unsupportedParameters(method: Method) =
            method.parameters.filter { !isSupportedAsParameter(it.type) }

}
