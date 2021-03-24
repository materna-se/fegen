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

import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Links;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

public abstract class RepresentationModelResponseLinker<T> implements ResponseBodyAdvice<T> {

    protected final LinkCollector linkCollector;

    protected final RepositoryRestConfiguration repositoryRestConfiguration;

    protected final ProjectionFactory projectionFactory;

    protected final Class<T> supported;

    RepresentationModelResponseLinker(
            LinkCollector linkCollector,
            RepositoryRestConfiguration repositoryRestConfiguration,
            ProjectionFactory projectionFactory,
            Class<T> supported
    ) {
        this.linkCollector = linkCollector;
        this.repositoryRestConfiguration = repositoryRestConfiguration;
        this.projectionFactory = projectionFactory;
        this.supported = supported;
    }

    @Override
    public boolean supports(MethodParameter returnType, @NotNull Class<? extends HttpMessageConverter<?>> converterType) {
        Type responseEntityType = returnType.getGenericParameterType();
        if (!(responseEntityType instanceof ParameterizedType)) {
            return false;
        } else {
            ParameterizedType parameterizedResponseEntityType = (ParameterizedType) responseEntityType;
            if (!parameterizedResponseEntityType.getRawType().equals(ResponseEntity.class)) {
                return false;
            } else {
                Type representationModel = parameterizedResponseEntityType.getActualTypeArguments()[0];
                if (!(representationModel instanceof ParameterizedType)) {
                    return false;
                } else {
                    return ((ParameterizedType) representationModel).getRawType().equals(supported);
                }
            }
        }
    }

    public abstract T convert(T body);

    @Override
    public T beforeBodyWrite(
            T body,
            @NotNull MethodParameter returnType,
            @NotNull MediaType selectedContentType,
            @NotNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NotNull ServerHttpRequest request,
            @NotNull ServerHttpResponse response
    ) {
        return convert(body);
    }

    public EntityModel<?> entityToModel(Object entity, Links additionalLinks) {
        if (entity instanceof EntityModel) {
            return (EntityModel<?>) entity;
        }
        Map<String, Class<?>> projections = repositoryRestConfiguration.getProjectionConfiguration().getProjectionsFor(entity.getClass());
        Class<?> projectionClass = projections.get("baseProjection");
        if (projectionClass == null) {
            throw new RuntimeException("Could not return entity of type " + entity.getClass().getCanonicalName() + ", since it has no base projection");
        }
        Object projection = projectionFactory.createProjection(projectionClass, entity);
        Links links = linkCollector.getLinksFor(entity);
        if (additionalLinks != null) {
            links = links.and(additionalLinks);
        }
        return new EntityModel<>(projection, links);
    }
}
