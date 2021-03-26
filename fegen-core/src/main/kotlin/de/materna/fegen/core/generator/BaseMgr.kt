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
package de.materna.fegen.core.generator

import de.materna.fegen.core.FeGenConfig
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.type.filter.AnnotationTypeFilter
import java.io.File

abstract class BaseMgr(
        protected val feGenConfig: FeGenConfig,
        val domainMgr: DomainMgr
) {

    protected fun searchForComponentClassesByAnnotation(annotationClass: Class<out Annotation>): List<Class<*>> {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        // for scanning, add only the classes dir
        scanner.resourceLoader = PathMatchingResourcePatternResolver(domainMgr.classLoader)

        // scan for annotation type
        scanner.addIncludeFilter(AnnotationTypeFilter(annotationClass))

        // load the entities
        return scanner.findCandidateComponents(feGenConfig.scanPkg).map {
            domainMgr.classLoader.loadClass(it.beanClassName)
        }
    }

    protected fun typesWithAnnotation(customPkg: String, annotationClass: Class<out Annotation>): List<Class<*>> {
        return typesInPackage(customPkg).filter {
            it.getAnnotation(annotationClass) != null
        }
    }

    protected fun typesImplementing(customPkg: String, superType: Class<*>): List<Class<*>> {
        return typesInPackage(customPkg).filter {
            superType.isAssignableFrom(it)
        }
    }

    private fun typesInPackage(customPkg: String): List<Class<*>> {
        return feGenConfig.classesDirArray.flatMap { classesDir ->
            val classesDirPath = classesDir.normalize().absolutePath
            val customPkgPath = customPkg.replace('.', '/')
            File("$classesDirPath/$customPkgPath").walkTopDown().filter {
                it.name.endsWith("class")
            }.map {
                val canonicalName = it.relativeTo(classesDir).path
                    .replace('/', '.')
                    .replace('\\', '.')
                    .removeSuffix(".${it.extension}")
                domainMgr.classLoader.loadClass(canonicalName)
            }.toList()
        }
    }
}