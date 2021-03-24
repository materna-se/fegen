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
package de.materna.fegen.util.spring.component;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.ControllerAdvice;

import javax.persistence.Entity;

@SuppressWarnings("rawtypes")
@ControllerAdvice
public class EntityModelResponseLinker extends RepresentationModelResponseLinker<EntityModel> {

    public EntityModelResponseLinker(
            LinkCollector linkCollector,
            RepositoryRestConfiguration repositoryRestConfiguration,
            ProjectionFactory projectionFactory
    ) {
        super(linkCollector, repositoryRestConfiguration, projectionFactory, EntityModel.class);
    }

    @Override
    public EntityModel convert(EntityModel body) {
        Object entity = body.getContent();
        if (entity == null) {
            return body;
        }
        if (body.getLinks().stream().anyMatch(link -> link.getRel().value().equals("self"))) {
            return body;
        } else if (entity.getClass().getAnnotation(Entity.class) == null) {
            return body;
        } else {
            return entityToModel(entity, body.getLinks());
        }
    }
}
