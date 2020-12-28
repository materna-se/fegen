package de.materna.fegen.core.generator.security

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import de.materna.fegen.core.domain.EntityType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File

class SecurityControllerGen(private val dir: File, private val entities: List<EntityType>) {

	fun generateController() {
		val controllerName = "SecurityController"
		val file = FileSpec.builder("de.materna.fegen.generated", controllerName)
		val annotations = listOf(
			AnnotationSpec.builder(RestController::class.java).build(), AnnotationSpec.builder(
				RequestMapping::class.java).addMember("\"/security\"").build())

		val controllerClass = TypeSpec.classBuilder(controllerName)
			.addAnnotations(annotations)
			.addFunctions(entities.filter { it.security.isNotEmpty() }.map { buildMethod(it) })
			.build()

		file.addType(controllerClass)

		file.indent("    ")
		file.build().writeTo(dir)
	}

	private fun buildMethod(entityType: EntityType): FunSpec {

		val securityContext = ClassName("org.springframework.security.core.context", "SecurityContextHolder")

		return FunSpec.builder("getSecurityConfig${entityType.name.capitalize()}")
			.addAnnotation(AnnotationSpec.builder(GetMapping::class).addMember("\"${entityType.nameRest}\"").build())
			.returns(List::class.asClassName().parameterizedBy(ClassName("kotlin", "String")))
			.addStatement("val securityContext = %T.getContext()", securityContext)
			.addStatement("val authentication = securityContext.authentication")
			.addStatement("val res = mutableMapOf<String, Array<String>>()")
			.addCode(entityType.security.joinToString(separator = "\n") {
				"res.put(\"${it.method}\", arrayOf(${
					it.roles.joinToString(
						separator = ",",
						prefix = "\"",
						postfix = "\""
					)
				}))"
			})
			.addStatement("\n")
			.addStatement("val roles = authentication.authorities.map{grantedAuthority -> grantedAuthority.authority.substringAfter(\"ROLE_\") }")
			.addStatement("return res.filter { entry -> entry.value.any {it in roles }}.map{ it.key.toUpperCase()}")
			.build()
	}

}