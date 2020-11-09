package de.materna.fegen.core.generator.types

import de.materna.fegen.core.*
import de.materna.fegen.core.domain.Pojo
import de.materna.fegen.core.generator.DomainMgr
import de.materna.fegen.core.generator.FieldMgr
import de.materna.fegen.core.log.FeGenLogger

class PojoMgr(
        feGenConfig: FeGenConfig,
        logger: FeGenLogger,
        domainMgr: DomainMgr
) : ComplexTypeMgr(feGenConfig, logger, domainMgr) {

    private val class2Pojo = mutableMapOf<Class<*>, Pojo>()

    fun resolvePojo(type: Class<*>): Pojo {
        var result = class2Pojo[type]
        if (result == null) {
            result = Pojo(name = type.simpleName, typeName = type.simpleName)
            // It is crucial to insert the pojo into the map before resolving its fields
            class2Pojo[type] = result
            // because resolveFields may run into a stack overflow when encountering recursive types otherwise
            result.fields = resolveFields(type)
        }
        return result
    }

    private fun resolveFields(type: Class<*>) =
            candidateFields(type).asSequence().sortedBy { it.fieldName }.map { method ->
                domainMgr.fieldMgr.dtFieldFromType(
                        name = method.fieldName,
                        optional = method.field?.optional ?: false,
                        type = method.fieldType,
                        context = FieldMgr.FieldContext(type)
                )
            }.toList()
}
