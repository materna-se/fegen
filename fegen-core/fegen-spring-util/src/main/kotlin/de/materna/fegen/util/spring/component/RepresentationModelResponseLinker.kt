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
package de.materna.fegen.example.gradle.component

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.MethodParameter
import org.springframework.data.projection.ProjectionFactory
import org.springframework.data.rest.core.config.RepositoryRestConfiguration
import org.springframework.data.rest.webmvc.mapping.LinkCollector
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Links
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import java.lang.reflect.ParameterizedType

abstract class RepresentationModelResponseLinker<T>(
        @Autowired private val linkCollector: LinkCollector,
        @Autowired private val repositoryRestConfiguration: RepositoryRestConfiguration,
        @Autowired private val projectionFactory: ProjectionFactory
) : ResponseBodyAdvice<T> {

    abstract val supported: Class<T>

    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean =
            returnType.genericParameterType.let { responseEntityType ->
                when {
                    responseEntityType !is ParameterizedType -> false
                    responseEntityType.rawType != ResponseEntity::class.java -> false
                    else -> {
                        val representationModel = responseEntityType.actualTypeArguments.first()
                        when {
                            representationModel !is ParameterizedType -> false
                            representationModel.rawType != supported -> false
                            else -> true
                        }
                    }
                }
            }

    abstract fun convert(body: T): T

    override fun beforeBodyWrite(body: T, returnType: MethodParameter, selectedContentType: MediaType, selectedConverterType: Class<out HttpMessageConverter<*>>, request: ServerHttpRequest, response: ServerHttpResponse): T {
        return convert(body)
    }

    fun entityToModel(entity: Any, additionalLinks: Links? = null): EntityModel<*> {
        if (entity is EntityModel<*>) {
            return entity
        }
        val projections = repositoryRestConfiguration.projectionConfiguration.getProjectionsFor(entity.javaClass)
        val projectionClass = projections["baseProjection"]
                ?: error("Could not return entity of type ${entity.javaClass.canonicalName}, since it has no base projection")
        val projection = projectionFactory.createProjection(projectionClass, entity)
        var links = linkCollector.getLinksFor(entity)
        if (additionalLinks != null) {
            links = links.and(additionalLinks)
        }
        return EntityModel(projection, links)
    }
}
