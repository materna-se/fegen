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


    fun fromClass(type: Class<*>): Pojo =
            Pojo(name = type.simpleName, typeName = type.simpleName).apply {
                fields = candidateFields(type).asSequence().sortedBy { it.fieldName }.map { method ->
                    domainMgr.fieldMgr.dtFieldFromType(
                            name = method.fieldName,
                            optional = method.field?.optional ?: false,
                            type = method.fieldType,
                            context = FieldMgr.FieldContext(type)
                    )
                }.toList()
            }
}
