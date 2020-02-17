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

import org.springframework.data.domain.Pageable
import org.springframework.data.rest.core.annotation.RepositoryRestResource
import org.springframework.data.rest.core.annotation.RestResource
import org.springframework.data.rest.webmvc.BasePathAwareController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.math.BigDecimal
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.Entity

class FeGenUtil(
        var classesDirArray: List<File>,
        var scanPkg: String,
        private val classpath: List<File>,
        internal val entityPkg: String,
        internal val repositoryPkg: String,
        private val implicitNullable: DiagnosticsLevel,
        private val logger: FeGenLogger
) {

    // for loading the isEntity classes add project classpath (so that we have all dependencies of the target project
    // on the classpath)
    val classLoader by lazy {
        val arrayURL: Array<URL> = classesDirArray.map { it.toURI().toURL() }.toTypedArray()
        URLClassLoader(
                arrayURL + classpath.map { it.toURI().toURL() },
                this.javaClass.classLoader
        )
    }

    fun createModelInstanceList(): List<DomainType> {

        val domainClasses = lookupDomainClasses()

        // transform the entities and projections to an instance of the domain type meta model.
        val class2DT = buildDomainTypes(domainClasses)

        warnMissingBaseProjections(class2DT)

        populateWithCustomSearches(class2DT)

        populateWithRepositorySearches(class2DT)

        populateWithCustomEndpoints(class2DT)

        return class2DT.values.toList()
    }


    private fun lookupDomainClasses(): List<Class<*>> {
        val projectionClasses = searchForProjectionClasses()
        if (projectionClasses.isEmpty()) {
            logger.info("No projections found")
            logger.info("Projections must be located in the package ${entityPkg}")
            logger.info("and be annotated with Projection")
        } else {
            logger.info("Projections found: ${projectionClasses.size}")
        }
        val entityClasses = searchForComponentClassesByAnnotation(Entity::class.java)
        if (entityClasses.isEmpty()) {
            logger.warn("No entity classes were found")
            logger.info("Entity classes must be located in the package ${entityPkg}")
            logger.info("and have the annotation Entity")
        } else {
            logger.info("Entity classes found: ${entityClasses.size}")
        }

        return entityClasses + projectionClasses
    }


    private fun buildDomainTypes(classes: List<Class<*>>): MutableMap<Class<*>, DomainType> {
        val class2CT = initDomainTypes(classes)
        return populateDomainTypes(class2CT)
    }

    private fun initDomainTypes(classes: List<Class<*>>): LinkedHashMap<Class<*>, ComplexType> {
        // create a domain type instance for each entity and associate each class to the respective domain type (for cyclic dependencies)
        val class2ET = classes.asSequence().filter {
            it.isEntity
        }.associateWith { ec -> EntityType(name = ec.simpleName) }.toMap()

        // create a domain type instance for each projection and associate each class to the respective domain type (for cyclic dependencies)
        val class2PT = classes.asSequence().filter {
            it.isProjection
        }.associateWith { pc ->
            val parentType = pc.projectionType!!
            ProjectionType(
                    name = pc.simpleName,
                    projectionName = pc.projectionName!!,
                    baseProjection = pc.simpleName == "BaseProjection",
                    parentType = class2ET[parentType]
                            ?: error("Parent ${parentType.simpleName} of projection ${pc.simpleName} is not an entity. " +
                                    "Entities: ${classes.joinToString("\n") { it.simpleName }}")
            )
        }.toMap()

        // use linked hash map, so that the field creation is executed on entities first (see below)
        val class2CT = LinkedHashMap<Class<*>, ComplexType>(class2ET)

        class2CT.putAll(class2PT)
        return class2CT
    }

    private fun populateDomainTypes(class2CT: LinkedHashMap<Class<*>, ComplexType>): MutableMap<Class<*>, DomainType> {
        // this is the resulting map, which contains all domain types (including enums, therefore it
        // is mutable as they are added on the fly)
        val class2DT: MutableMap<Class<*>, DomainType> = class2CT.toMutableMap()

        // iterate all complex types (entities and projections) and add fields. This is done in a second iteration, because
        // there can be cyclic dependencies between complex types.
        class2CT.forEach { (clazz, complexType) ->
            complexType.fields = clazz.getters.filter { m ->
                // we are interested in non-ignored fields only, i.e., fields that are either notIgnored or writable.
                m.field?.run { (notIgnored && m.notIgnored) || (setter?.writable ?: false) } ?: true
            }.filter { m ->
                // if the type is a projection, omit non-complex fields that are already defined on the returnType type
                when (complexType) {
                    // TODO omit fields that are complex but represented with same type on returnType type, too.
                    is ProjectionType -> complexType.parentType.fields.firstOrNull { it.name == m.fieldName }?.let {
                        it is DTRComplex
                    } ?: true
                    is EntityType -> true
                }
            }.filter { it.fieldName != "version" }.filter { it.fieldType.typeName != "byte[]" }.sortedBy {
                if (it.fieldName == "id") "" else it
                        .fieldName
            }
                    .map { m ->
                        // E.g., the name of getter method "getName()" should be "name".
                        val name = m.fieldName
                        // TODO find out, why I have to use classmate here. Since e.g. Project in plan-info implements Identifiable the return type of getId is Serializable ?!?
                        // use either the raw return type if it is a overriden type parameter, or else the return type.
                        val type = m.fieldType
                        val field = m.field
                        if (clazz.isProjection && type is Class<*> && type.isEntity) {
                            logger.error("Field \"${name}\" in projection \"${clazz.canonicalName}\" has an entity type.")
                            logger.error("This will cause issues when trying to modify or delete the entity contained in the field.")
                            logger.error("Please use a projection of \"${type.simpleName}\" instead")
                        }
                        if (field != null) {
                            implicitNullable.check(logger, { !field.required && !field.explicitOptional }) { print ->
                                print("Field \"$name\" in entity \"${clazz.canonicalName}\" is implicitly nullable.")
                                print("  Please add a @Nullable annotation if this is intentional")
                                if (implicitNullable == DiagnosticsLevel.WARN) {
                                    print("  Set implicitNullable to ALLOW in FeGen's build configuration to hide this warning")
                                } else {
                                    print("  Set implicitNullable to WARN to continue the build despite missing @Nullable annotations")
                                }
                            }
                        }
                        val parentField = if (complexType is ProjectionType) {
                            complexType.parentType.entityFields.find { it.name == name }
                        } else {
                            null
                        }

                        typeToDTReference(
                                clazz.canonicalName,
                                name, type, class2DT,
                                list = false,
                                optional = field?.optional ?: parentField?.optional ?: false,
                                justSettable = !(m.notIgnored && field?.notIgnored ?: false) && (field?.setter?.writable
                                        ?: false)
                        )
                    }
        }
        return class2DT
    }

    private fun isValueType(type: Type): Boolean {
        return when (type) {
            is Class<*> -> !type.isEntity && !type.isProjection
            is ParameterizedType -> {
                if (!java.lang.Iterable::class.java.isAssignableFrom(type.rawType as Class<*>)) return false
                // recursive call for list types (with boolean parameter 'list' set to true)
                isValueType(type.actualTypeArguments.first())
            }
            else -> false
        }
    }

    private fun typeToDTReference(
            className: String = "",
            name: String = "",
            type: Type,
            class2DT: MutableMap<Class<*>, DomainType>,
            list: Boolean = false,
            optional: Boolean = false,
            justSettable: Boolean = false
    ): DTReference {
        return when (type) {
            is Class<*> -> when (type) {
                Boolean::class.java -> DTRSimple(
                        name = name,
                        list = list,
                        optional = optional,
                        justSettable = justSettable,
                        type = SimpleType.BOOLEAN
                )
                java.lang.Long::class.java, 1L.javaClass -> DTRSimple(
                        name = name,
                        list = list,
                        optional = optional,
                        justSettable = justSettable,
                        type = SimpleType.LONG
                )
                java.lang.Integer::class.java, 1.javaClass -> DTRSimple(
                        name = name,
                        list = list,
                        optional = optional,
                        justSettable = justSettable,
                        type = SimpleType.INTEGER
                )
                java.lang.Double::class.java, 1.0.javaClass -> DTRSimple(
                        name = name,
                        list = list,
                        optional = optional,
                        justSettable = justSettable,
                        type = SimpleType.DOUBLE
                )
                BigDecimal::class.java -> DTRSimple(
                        name = name,
                        list = list,
                        optional = optional,
                        justSettable = justSettable,
                        type = SimpleType.BIGDECIMAL
                )
                UUID::class.java -> DTRSimple(
                        name = name,
                        list = list,
                        optional = optional,
                        justSettable = justSettable,
                        type = SimpleType.UUID
                )
                LocalDate::class.java -> DTRSimple(
                        name = name,
                        list = list,
                        optional = optional,
                        justSettable = justSettable,
                        type = SimpleType.DATE
                )
                LocalDateTime::class.java -> DTRSimple(
                        name = name,
                        list = list,
                        optional = optional,
                        justSettable = justSettable,
                        type = SimpleType.DATETIME
                )
                ZonedDateTime::class.java -> DTRSimple(
                        name = name,
                        list = list,
                        optional = optional,
                        justSettable = justSettable,
                        type = SimpleType.ZONED_DATETIME
                )
                OffsetDateTime::class.java -> DTRSimple(
                        name = name,
                        list = list,
                        optional = optional,
                        justSettable = justSettable,
                        type = SimpleType.OFFSET_DATETIME
                )
                Duration::class.java -> DTRSimple(
                        name = name,
                        list = list,
                        optional = optional,
                        justSettable = justSettable,
                        type = SimpleType.DURATION
                )
                String::class.java, URI::class.java -> DTRSimple(
                        name = name,
                        list = list,
                        optional = optional,
                        justSettable = justSettable,
                        type = SimpleType.STRING
                )
                else -> {
                    when {
                        type.isEnum -> DTREnum(
                                name = name,
                                list = list,
                                optional = optional,
                                justSettable = justSettable,
                                type = class2DT.getOrDefault(type,
                                        EnumType(
                                                name = type.simpleName,
                                                constants = type.enumConstants.map { c -> c.toString() }
                                        ).apply { class2DT[type] = this }
                                ) as EnumType
                        )
                        !class2DT.containsKey(type) -> throw IllegalStateException("UNKNOWN class ${type.typeName} : ${type::class.java.name} for field '${name}' in entity $className")
                        type.isEntity -> DTREntity(
                                name = name,
                                list = list,
                                optional = optional,
                                justSettable = justSettable,
                                type = class2DT[type] as? EntityType
                                        ?: EntityType("UNKNOWN")
                        )
                        type.isProjection -> DTRProjection(
                                name = name,
                                list = list,
                                optional = optional,
                                justSettable = justSettable,
                                type = class2DT[type] as? ProjectionType
                                        ?: ProjectionType(
                                                name = "UNKNOWN",
                                                projectionName = "UNKNOWN",
                                                baseProjection = false,
                                                parentType = EntityType("UNKNOWN")
                                        )

                        )
                        else -> throw IllegalStateException("UNKNOWN class '${name}': ${type.typeName} & ${type::class.java.name} in entity $className")
                    }
                }
            }
            is ParameterizedType -> {
                if (!java.lang.Iterable::class.java.isAssignableFrom(type.rawType as Class<*>)) throw IllegalStateException("Cannot handle ${type}.")
                // recursive call for list types (with boolean parameter 'list' set to true)
                typeToDTReference(
                        className,
                        name,
                        type.actualTypeArguments.first(),
                        class2DT,
                        true,
                        false,
                        justSettable
                )
            }
            else -> throw IllegalStateException("UNKNOWN non-class '$name': ${type.typeName} & ${type::class.java.name}")
        }
    }

    private fun warnMissingBaseProjections(class2DT: Map<Class<*>, DomainType>) {
        val allEntities = class2DT.values.mapNotNull { it as? EntityType }
        val allProjections = class2DT.values.mapNotNull { it as? ProjectionType }
        val allBaseProjections = allProjections.filter { it.baseProjection }
        val allBaseProjectionParents = allBaseProjections.map { it.parentType }
        val entitiesWithoutBP = allEntities - allBaseProjectionParents
        if (entitiesWithoutBP.isNotEmpty()) {
            logger.warn("The following entities do not have a base projection:")
            logger.warn(entitiesWithoutBP.join(separator = ", ") { name })
        }
    }


    private fun populateWithRepositorySearches(class2DT: MutableMap<Class<*>, DomainType>) {
        val repositoryClasses = searchForRepositoryClasses().filter {
            it.getAnnotation(RepositoryRestResource::class.java).exported
        }
        if (repositoryClasses.isEmpty()) {
            logger.info("No repository classes found")
            logger.info("Repository classes must be located in the package $repositoryPkg")
            logger.info("and must be annotated with RepositoryRestResource")
        } else {
            logger.info("Repository classes found: ${repositoryClasses.size}")
        }
        // use @RepositoryRestResource path value for the name of domain types!
        repositoryClasses.forEach {
            val path = it.getAnnotation(RepositoryRestResource::class.java)?.path

            val domainType = class2DT[it.repositoryType] as? EntityType ?: return@forEach
            if (!path.isNullOrEmpty()) {
                domainType.name = path.capitalize()
            }
        }

        // retrieve all exported (search) methods
        repositoryClasses.associateWith { c ->
            c.declaredMethods.filter { m ->
                m.getAnnotation(RestResource::class.java)?.exported ?: true
            }.filter {
                it.name.startsWith("find")
            }.filter { m ->
                m.parameters.all { p ->
                    isValueType(p.type).also {
                        if (!it) {
                            logger.warn("Repository search method ${c.simpleName}::${m.name} will be ignored because the type of parameter ${p.name} cannot be handled")
                        }
                    }
                }
            }
        }.also {
            if (it.all { it.value.isEmpty() }) {
                logger.info("No repository search methods were found")
                logger.info("Repository search methods' names must start with \"find\"")
            } else {
                logger.info("Repository search methods found: ${it.values.sumBy { it.size }}")
            }
        }.flatMap { (c, m) -> m.map { c to it } }.sortedBy { it.second.name }.forEach { (clazz, search) ->
            val resultType = search.declaringClass.repositoryType
            val domainType = class2DT[resultType] as? EntityType ?: return@forEach
            domainType.searches +=
                    Search(
                            name = search.getAnnotation(RestResource::class.java)?.path ?: search.name,
                            paging = search.repoPaging,
                            list = search.repoList,
                            parameters = search.parameters
                                    .filter { p -> !Pageable::class.java.isAssignableFrom(p.type) }
                                    .map { p ->
                                        typeToDTReference(
                                                className = clazz.canonicalName,
                                                name = p.nameREST,
                                                type = p.type,
                                                class2DT = class2DT
                                        ) as DTRValue
                                    }.toList(),
                            returnType = domainType,
                            inRepo = true
                    )
        }
    }

    private fun populateWithCustomSearches(class2DT: MutableMap<Class<*>, DomainType>) {
        // retrieve classes with search methods
        val controllerClasses = searchForComponentClassesByAnnotation(BasePathAwareController::class.java)
        if (controllerClasses.isEmpty()) {
            logger.info("Found no BasePathAwareController classes")
            logger.info("which can be used to add custom searches")
        } else {
            logger.info("Custom search controller classes found: ${controllerClasses.size}")
        }
        val searchClasses = controllerClasses.filter { clazz ->
            clazz.isSearchController.also {
                if (!it) {
                    logger.info("${clazz.simpleName} is not a search controller since the first path of its RequestMapping annotation does not end with /search")
                }
            }
        }

        // retrieve search methods and add them to the corresponding entity type.
        var searchMethods = searchClasses.associateWith { c ->
            c.methods.filter { m -> m.getAnnotation(RequestMapping::class.java) != null }.also {
                if (it.isEmpty()) {
                    logger.warn("${c.simpleName} does not contain any custom search methods")
                    logger.info("Custom search methods must be annotated with RequestMapping")
                }
            }
        }
        searchMethods = searchMethods.filter { (c, ms) ->
            ms.all { m ->
                m.requestParams.all { p ->
                    isValueType(p.type).also {
                        if (!it) {
                            logger.warn("Custom search method ${c.simpleName}::${m.name} will be ignored because the type of the request parameter ${p.name} cannot be handled")
                        }
                    }
                }
            }
        }
        searchMethods.flatMap { (clazz, method) -> method.map { clazz to it } }.sortedBy { it.second.name }.forEach { (clazz, search) ->
            val requestMapping = search.getAnnotation(RequestMapping::class.java) ?: return@forEach

            // Try to retrieve search type via return type. If this is not possible, try to extract the name from the request mapping.
            // If nothing works, the code will die :).
            val domainType = (search.entityType?.let { class2DT[it] }
                    ?: class2DT.values.first { it.name == search.searchTypeName }) as EntityType
            val searchName = requestMapping.value.firstOrNull() ?: requestMapping.path.firstOrNull()
            if (searchName == null) {
                logger.warn("Custom search method ${clazz.simpleName}::${search.name} will be ignored because neither value nor path is specified for its RequestMapping annotation")
                return@forEach
            }
            domainType.searches +=
                    Search(
                            name = requestMapping.value.firstOrNull() ?: requestMapping.path.firstOrNull()
                            ?: throw IllegalStateException(""),
                            paging = search.paging,
                            list = search.list,
                            parameters = search.requestParams.map { p ->
                                val requestParam = p.getAnnotation(RequestParam::class.java)
                                typeToDTReference(
                                        className = clazz.canonicalName,
                                        name = p.nameREST,
                                        type = p.type,
                                        optional = !requestParam.required,
                                        class2DT = class2DT
                                ) as DTRValue // TODO split typeToDTReference into two parts in order to omit cast...
                            }.toList(),
                            returnType = domainType,
                            inRepo = false
                    )
        }
    }

    private fun populateWithCustomEndpoints(class2DT: MutableMap<Class<*>, DomainType>) {
        val controllerClasses2DT = searchForComponentClassesByAnnotation(RestController::class.java).associateWith { c ->
            c.getAnnotation(RequestMapping::class.java)?.run {
                (value.firstOrNull() ?: path.firstOrNull())?.let { path ->
                    class2DT.values.firstOrNull { dt -> path.endsWith(dt.nameREST) }
                }
            } as EntityType?
        }.mapNotNull { if (it.value != null) it.key to (it.value as EntityType) else null }

        if (controllerClasses2DT.isEmpty()) {
            logger.info("Found no custom endpoint classes")
            logger.info("Those classes must be annotated with a RestController annotation")
            logger.info("whose value ends with the decapitalized name of an entity")
        } else {
            logger.info("Custom endpoint classes found: ${controllerClasses2DT.size}")
        }

        controllerClasses2DT.sortedBy { it.first.name }.forEach { (c, domainType) ->
            val baseUri = c.getAnnotation(RequestMapping::class.java).run { value.firstOrNull() ?: path.first() }
            val methods = c.declaredMethods.filter {
                try {
                    it.requestMapping != null
                } catch (e: Exception) {
                    logger.warn("Method ${c.canonicalName}.${it.name} will be ignored: ${e.message}")
                    false
                }
            }.filter { m ->
                m.pathVariables.all { p ->
                    isValueType(p.parameterizedType).also {
                        if (!it) {
                            logger.warn("Custom endpoint ${c.simpleName}::${m.name} will be ignored")
                            logger.warn("because its parameter ${p.name} has an unsupported type")
                        }
                    }
                }
            }.sortedBy { it.name }

            if (methods.isEmpty()) {
                logger.warn("Class \"${c.simpleName}\" has no custom endpoints")
                logger.warn("Custom endpoints must be methods annotated with RequestMapping")
            }

            methods.forEach { m ->
                val returnType = class2DT[m.entityType]
                if (returnType != null && returnType !is ProjectionType) {
                    logger.warn("Return type of custom endpoint ${c.simpleName}::${m.name} is not a projection")
                    logger.warn("This may cause issues when using the return type in the frontend")
                }
                val (name, endpointMethod) = m.requestMapping!!
                domainType.customEndpoints += CustomEndpoint(
                        baseUri = baseUri,
                        name = name,
                        parentType = domainType,
                        method = endpointMethod,
                        pathVariables = m.pathVariables.map {
                            typeToDTReference(
                                    className = c.canonicalName,
                                    name = it.nameREST,
                                    type = it.parameterizedType,
                                    class2DT = class2DT
                            ) as DTRValue
                        },
                        requestParams = m.requestParams.map {
                            val req = it.getAnnotation(RequestParam::class.java)
                            typeToDTReference(
                                    className = c.canonicalName,
                                    name = it.nameREST,
                                    type = it.parameterizedType,
                                    optional = !req.required,
                                    class2DT = class2DT
                            ) as DTRValue
                        },
                        body = m.requestBody?.let {
                            typeToDTReference(
                                    className = c.canonicalName,
                                    name = "body",
                                    type = it.parameterizedType,
                                    class2DT = class2DT
                            ) as? DTREntity
                        },
                        //returnType = class2DT.entries.firstOrNull{(c, dt) -> c.name == m.entityType?.name}?.value,
                        returnType = returnType,
                        paging = m.paging,
                        list = m.list,
                        canReceiveProjection = m.canReceiveProjection
                )
            }
        }
    }

}