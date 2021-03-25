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
package de.materna.fegen.core.domain

import com.fasterxml.classmate.MemberResolver
import com.fasterxml.classmate.ResolvedTypeWithMembers
import com.fasterxml.classmate.TypeResolver
import com.fasterxml.classmate.members.ResolvedField
import com.fasterxml.classmate.members.ResolvedMember
import com.fasterxml.classmate.members.ResolvedMethod
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.lang.Nullable
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Type
import javax.persistence.*
import javax.validation.constraints.NotNull

/**
 * Represents a property of a java or kotlin class that will be returned in a JSON object
 * if an instance of the class is serialized. This can either be a field or a getter.
 */
sealed class ClassProperty {

    protected abstract val member: ResolvedMember<*>

    /**
     * Elements that can be annotated with e.g. NotNull to change the represented property.
     * This may include the java / kotlin field and the getter method.
     */
    protected abstract val annotatable: List<AccessibleObject>

    abstract val name: String

    abstract fun notIgnored(): Boolean

    abstract val type: Type

    val owningClass: Class<*> by lazy {
        member.declaringType.erasedType
    }

    private val resolvedDeclaringType: ResolvedTypeWithMembers by lazy {
        memberResolver.resolve(member.declaringType, null, null)
    }

    private val setterName by lazy {
        "set${name.capitalize()}"
    }

    protected val setter by lazy {
        resolvedDeclaringType.memberMethods.singleOrNull { it.name == setterName }
    }

    val field by lazy {
        resolvedDeclaringType.memberFields.find { f ->
            f.name == name
        }
    }

    val justSettable by lazy {
        if (field == null) {
            return@lazy false
        }
        if (annotatable.all { notIgnored(it) }) {
            return@lazy false
        }
        setter?.let { hasAnnotation(it.rawMember, JsonProperty::class.java) } ?: false
    }

    val notNull by lazy {
        if ((type as? Class<*>?)?.isPrimitive == true) {
            !explicitNullable
        } else {
            explicitNotNull
        }
    }

    val explicitNullable by lazy {
        annotatable.any {
            hasAnnotation(it, Nullable::class.java) || hasAnnotation(it, javax.annotation.Nullable::class.java)
        }
    }

    private val explicitNotNull by lazy {
        annotatable.any {
            hasAnnotation(it, OneToMany::class.java) ||
                    hasAnnotation(it, ManyToMany::class.java) ||
                    it.getAnnotation(ManyToOne::class.java)?.optional?.not() ?: false ||
                    it.getAnnotation(OneToOne::class.java)?.optional?.not() ?: false ||
                    it.getAnnotation(Column::class.java)?.nullable?.not() ?: false ||
                    hasAnnotation(it, Id::class.java) ||
                    hasAnnotation(it, NotNull::class.java)
        }
    }

    companion object {

        private val typeResolver = TypeResolver()

        private val memberResolver = MemberResolver(typeResolver)

        fun forClass(clazz: Class<*>): List<ClassProperty> {
            val resolvedClass =  memberResolver.resolve(typeResolver.resolve(clazz), null, null)
            val getters = getters(resolvedClass).map { ClassPropertyGetter(it) }.associateBy { it.name }
            val fields = resolvedClass.memberFields.map { ClassPropertyField(it) }.associateBy { it.name }
            val properties = fields + getters
            return properties.values.filter { it.notIgnored() }.toList()
        }

        private fun getters(clazz: ResolvedTypeWithMembers): List<ResolvedMethod> {
            return clazz.memberMethods.filter { m ->
                // we are interested in getter methods only...
                m.name.startsWith("get") || m.name.startsWith("is")
            }
        }
    }

    protected fun notIgnored(member: AccessibleObject): Boolean {
        return !hasAnnotation(member, JsonIgnore::class.java)
    }

    protected fun isWritable(member: AccessibleObject): Boolean {
        return hasAnnotation(member, JsonProperty::class.java)
    }

    private fun hasAnnotation(member: AccessibleObject, annotationType: Class<out Annotation>): Boolean {
        return member.getAnnotation(annotationType) != null
    }

    class ClassPropertyField(
        override val member: ResolvedField
    ): ClassProperty() {

        override val name: String get() = this.member.name

        override val type: Type by lazy {
            if (member.rawMember.type == member.type.erasedType) {
                member.rawMember.genericType
            } else {
                member.type.erasedType
            }
        }

        override val annotatable = listOf(member.rawMember)

        override fun notIgnored(): Boolean {
            return notIgnored(member.rawMember)
        }
    }

    class ClassPropertyGetter(
        override val member: ResolvedMethod
    ): ClassProperty() {

        override val name = member.name.removePrefix("get").removePrefix("is").decapitalize()

        override val type: Type by lazy {
            if (member.rawMember.returnType == member.returnType.erasedType) {
                member.rawMember.genericReturnType
            } else {
                member.returnType.erasedType
            }
        }

        override val annotatable = listOfNotNull<AccessibleObject>(member.rawMember, field?.rawMember)

        override fun notIgnored(): Boolean {
            val field = field
            return if (field == null) {
                true
            } else {
                if (notIgnored(field.rawMember) && notIgnored(member.rawMember)) {
                    true
                } else {
                    setter?.let { isWritable(it.rawMember) } ?: false
                }
            }
        }
    }
}