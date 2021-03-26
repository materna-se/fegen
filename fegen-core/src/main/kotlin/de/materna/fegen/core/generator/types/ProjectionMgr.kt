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
import de.materna.fegen.core.domain.ProjectionType
import de.materna.fegen.core.generator.DomainMgr
import de.materna.fegen.core.log.FeGenLogger
import org.springframework.data.rest.core.config.Projection


class ProjectionMgr(
        feGenConfig: FeGenConfig,
        logger: FeGenLogger,
        private val entityMgr: EntityMgr,
        domainMgr: DomainMgr
): ComplexTypeMgr(feGenConfig, logger, domainMgr) {

    val class2Projection by lazy {
        // Since projections are no Spring components, they have to be looked up separately
        typesWithAnnotation(feGenConfig.entityPkg, Projection::class.java)
                .associateWith { toProjectionType(it) }
    }

    val projections by lazy {
        class2Projection.values.sortedBy { it.fullProjectionName }
    }

    private fun toProjectionType(clazz: Class<*>): ProjectionType {
        val parentType = clazz.projectionType!!
        val name = clazz.simpleName
        val projectionName = clazz.projectionName!!
        val isBaseProjection = clazz.simpleName == "BaseProjection"
        val parentEntity = entityMgr.class2Entity[parentType]
                ?: error("Parent ${parentType.simpleName} of projection ${clazz.simpleName} is not an entity. " + "Entities: ${entityMgr.class2Entity.values.sortedBy { it.name }.joinToString(", ") { it.name }}")
        return ProjectionType(
                name = name,
                projectionName = projectionName,
                fullProjectionName = if (isBaseProjection) "${parentEntity.name}$name" else name,
                baseProjection = isBaseProjection,
                parentType = parentEntity
        )
    }

    fun addFields() {
        class2Projection.forEach { addFields(it.key, it.value) }
    }

    override fun omitField(field: ClassProperty, complexType: ComplexType): Boolean {
        val projectionType = complexType as ProjectionType
        val parentField = projectionType.parentType.fields.firstOrNull { it.name == field.name }
        return if (parentField == null) {
            false
        } else {
            parentField !is ComplexDTField
        }
    }

    override fun checkField(field: ClassProperty) {
        val fieldType = field.type
        if (fieldType is Class<*> && fieldType.isEntity) {
            logger.error("Field \"${field.name}\" in projection \"${field.owningClass.canonicalName}\" has an entity type.")
            logger.error("This will cause issues when trying to modify or delete the entity contained in the field.")
            logger.error("Please use a projection of \"${fieldType.simpleName}\" instead")
        }
    }

    override fun parentField(fieldName: String, owningType: ComplexType): EntityDTField? {
        val projectionType = owningType as ProjectionType
        return projectionType.parentType.entityFields.find { it.name == fieldName }
    }

    fun warnIfEmpty() {
        if (class2Projection.isEmpty()) {
            logger.info("No projections found")
            logger.info("Projections must be located in the package ${feGenConfig.entityPkg}")
            logger.info("and be annotated with Projection")
        } else {
            logger.info("Projections found: ${class2Projection.size}")
        }
    }

    fun warnMissingBaseProjections() {
        val allBaseProjections = class2Projection.values.filter { it.baseProjection }
        val allBaseProjectionParents = allBaseProjections.map { it.parentType }
        val entitiesWithoutBP = entityMgr.class2Entity.values - allBaseProjectionParents
        if (entitiesWithoutBP.isNotEmpty()) {
            logger.warn("The following entities do not have a base projection:")
            logger.warn(entitiesWithoutBP.join(separator = ", ") { name })
        }
    }

    override fun checkImplicitNullable(property: ClassProperty) {}
}
