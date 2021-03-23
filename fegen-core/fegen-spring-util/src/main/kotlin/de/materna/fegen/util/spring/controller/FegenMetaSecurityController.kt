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
package de.materna.fegen.util.spring.controller

import de.materna.fegen.util.spring.annotation.FegenIgnore
import org.springframework.data.rest.webmvc.BasePathAwareController
import org.springframework.http.ResponseEntity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The endpoints in this controller provide information about
 * which endpoints in this spring application may be called
 * by the caller of the endpoints in this controller
 */
@BasePathAwareController
@RestController
@FegenIgnore
@RequestMapping("/fegen/security")
class FegenMetaSecurityController(
    private val webSecurityConfiguration: WebSecurityConfiguration
)  {

    private val httpMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE")

    /**
     * Returns a list of capitalized HTTP methods that the caller of this endpoint may use
     * to call the endpoint at the given path.
     */
    @GetMapping("allowedMethods")
    fun allowedMethods(
        @RequestParam path: String
    ): ResponseEntity<List<String>> {
        val authentication = SecurityContextHolder.getContext().authentication
        val privilegeEvaluator = webSecurityConfiguration.privilegeEvaluator()

        val allowedMethods = httpMethods.filter {
            privilegeEvaluator.isAllowed(null, path, it, authentication)
        }

        return ResponseEntity.ok(allowedMethods)
    }

    /**
     * Returns true iff the caller of this endpoint may use the specified method
     * to call the endpoint at the specified path.
     */
    @GetMapping("isAllowed")
    fun isAllowed(
        @RequestParam path: String,
        @RequestParam method: String
    ): ResponseEntity<Boolean> {
        val authentication = SecurityContextHolder.getContext().authentication
        val privilegeEvaluator = webSecurityConfiguration.privilegeEvaluator()

        return ResponseEntity.ok(privilegeEvaluator.isAllowed(null, path, method, authentication))
    }
}
