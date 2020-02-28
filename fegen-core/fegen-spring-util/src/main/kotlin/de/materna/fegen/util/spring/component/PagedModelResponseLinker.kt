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
import org.springframework.data.projection.ProjectionFactory
import org.springframework.data.rest.core.config.RepositoryRestConfiguration
import org.springframework.data.rest.webmvc.mapping.LinkCollector
import org.springframework.hateoas.PagedModel
import org.springframework.web.bind.annotation.ControllerAdvice
import javax.persistence.Entity

@ControllerAdvice
open class PagedModelResponseLinker(
        @Autowired private val linkCollector: LinkCollector,
        @Autowired private val repositoryRestConfiguration: RepositoryRestConfiguration,
        @Autowired private val projectionFactory: ProjectionFactory
): RepresentationModelResponseLinker<PagedModel<*>>(linkCollector, repositoryRestConfiguration, projectionFactory) {

    override val supported = PagedModel::class.java

    override fun convert(body: PagedModel<*>): PagedModel<*> {
        val entities = body.content
        return if (entities.firstOrNull()?.javaClass?.getAnnotation(Entity::class.java) != null ?: false) {
            val entityModels = entities.map { entityToModel(it) }
            PagedModel(entityModels, body.metadata, body.links)
        } else {
            body
        }
    }
}