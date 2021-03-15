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