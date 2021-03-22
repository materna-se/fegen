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
package de.materna.fegen.web.templates

import de.materna.fegen.core.*
import de.materna.fegen.core.domain.EntityType
import de.materna.fegen.core.domain.DTField
import de.materna.fegen.core.domain.ProjectionType
import de.materna.fegen.core.domain.RestMultiplicity
import de.materna.fegen.web.*
import de.materna.fegen.web.declaration
import de.materna.fegen.web.initialization
import de.materna.fegen.web.mayHaveSortParameter
import de.materna.fegen.web.nameNew
import de.materna.fegen.web.nameClient
import de.materna.fegen.web.nameDto
import de.materna.fegen.web.paramDecl
import de.materna.fegen.web.readOrderByParameter
import de.materna.fegen.web.returnDeclaration
import org.atteo.evo.inflector.English

fun FeGenWeb.toEntityClientTS() = entityTypes.filter { it.exported }.join(separator = "\n\n") domainType@{
    """
export class $nameClient extends BaseClient<ApiClient, $nameNew, $name> {

    constructor(apiClient: ApiClient, requestAdapter?: RequestAdapter){
        super("${uriREST(restBasePath)}", "${English.plural(name.decapitalize())}", apiClient, requestAdapter);
        this.readOne = this.readOne.bind(this);
        this.readProjection = this.readProjection.bind(this);
        ${
        entityFields.join(indent = 3, separator = "\n") {
            "this.${readAssociation}Projection = this.${readAssociation}Projection.bind(this);"
        }
    }
    }
  ${buildEntityTemplate(this)}
  ${plainObjTemplate(this)}
  ${readEntityTemplate(this, projectionTypes)}
  ${deleteEntityTemplate(this, restBasePath)}
  ${allowedMethodsTemplate(this, restBasePath)}
  ${associationEntityTemplate(this, projectionTypes)}
  ${searchEntityTemplate(this, restBasePath)}
}""".trimIndent()
}

private fun buildEntityTemplate(entityType: EntityType): String {
    val requiredEntities = entityType.entityFields.filter { !it.optional && !it.list }
    val fields = entityType.nonComplexFields.filter { it.name != "id" } + entityType.entityFields
    val baseType = "Partial<${entityType.nameNew}>" + if (requiredEntities.isNotEmpty()) {
        " & {${requiredEntities.join(separator = ",") { "$name: $declaration" }}}"
    } else {
        " = {}"
    }
    val fieldDecl: DTField.() -> String = {
        if (requiredEntities.contains(this)) {
            "$name: base.$name"
        } else {
            "$name: base.$name !== undefined ? base.$name : ${if (list) "[]" else initialization}"
        }
    }
    return """
    public static build(base: $baseType): ${entityType.nameNew} {
        return {
            ${fields.join(indent = 3, separator = ",\n") { fieldDecl(this) }}
        }
    }"""
}

private fun readEntityTemplate(entityType: EntityType, projectionTypes: List<ProjectionType>) = """
    ${
    projectionTypes.filter { it.parentType == entityType }.join(separator = "\n\n") {
        """public async readProjections$fullProjectionName(page?: number, size?: number${if (mayHaveSortParameter) readOrderByParameter else ""}) : Promise<PagedItems<$fullProjectionName>> {
        return this.readProjections<$fullProjectionName>("$projectionName", page, size${if (mayHaveSortParameter) ", sort" else ""});
    }
            
    public async readProjection$fullProjectionName(id: number): Promise<$fullProjectionName| undefined> {
        return this.readProjection<$fullProjectionName>(id, "$projectionName");
    }
    """
    }
}
    
    public async readAll(page?: number, size?: number${if (entityType.mayHaveSortParameter) entityType.readOrderByParameter else ""}) : Promise<PagedItems<${entityType.name}>> {
        return await this.readProjections<${entityType.name}>(undefined, page, size${if (entityType.mayHaveSortParameter) ", sort" else ""});
    }"""

private fun plainObjTemplate(entityType: EntityType): String {
    val fields = entityType.nonComplexFields.map { it.name } + "_links"
    return """
    protected toPlainObj(obj: ${entityType.name}): ${entityType.name} {
        return {
            ${fields.join(indent = 3, separator = ",\n") { "$this: obj.$this" }}
        };
    }"""
}

private fun deleteEntityTemplate(entityType: EntityType, restBasePath: String) = """
    ${
    entityType.entityFields.join(indent = 1, separator = "\n\n") dtField@{
        """
    public async $deleteFromAssociation(returnType: ${entityType.name}, childToDelete: ${type.name}) {
        await this._requestAdapter.getRequest().delete(`${entityType.uriREST(restBasePath)}/${'$'}{returnType.id}/$name/${'$'}{childToDelete.id}`);
    }""".trimIndent()
    }
}"""

private fun allowedMethodsTemplate(entityType: EntityType, restBasePath: String): String = """
    public allowedMethods(): Promise<EntitySecurity> {
        return EntitySecurity.fetch(this._requestAdapter.getRequest(), "/${entityType.uriREST(restBasePath)}");
    }
"""

private fun associationEntityTemplate(entityType: EntityType, projectionTypes: List<ProjectionType>) = """
    ${
    entityType.entityFields.join(indent = 1, separator = "\n\n") dtField@{
        """
    public async $readAssociation(obj: ${entityType.nameDto}): Promise<${if (list) declaration else "$declaration | undefined"}> {
        return this.${readAssociation}Projection<${type.declaration}>(obj);
    }

    ${
            projectionTypes.filter { it.parentType == type }.join(indent = 1, separator = "\n\n") {
                """
    public async ${this@dtField.readAssociation}Projection$fullProjectionName(obj: ${entityType.nameDto}): Promise<${if (this@dtField.list) "$fullProjectionName[]" else "$fullProjectionName | undefined"}> {
        return this.${this@dtField.readAssociation}Projection<$fullProjectionName>(obj, "$projectionName");
    }
    """
            }
        }

    public async ${readAssociation}Projection<T extends Dto>(obj: ${entityType.nameDto}, projection?: string): Promise<${if (list) "T[]" else "T | undefined"}> {
        const hasProjection = !!projection;
        let fullUrl = apiHelper.removeParamsFromNavigationHref(obj._links.${name});
        fullUrl = hasProjection ? `${'$'}{fullUrl}?projection=${'$'}{projection}` : fullUrl;
    
        const response = await this._requestAdapter.getRequest().get(fullUrl);
        if(response.status === 404) { return ${if (list) "[]" else "undefined"}; }
        if(!response.ok){ throw response; }
        ${
            if (list) """
            return (((await response.json()) as ApiHateoasObjectBase<T[]>)._embedded.${type.nameRest}).map(item => (apiHelper.injectIds(item)));""".doIndent(5)
            else """
        const result = (await response.json()) as T;
        return apiHelper.injectIds(result);"""
        }
    }

    ${
            if (list) """
    public async $setAssociation(returnType: ${entityType.name}, children: $declaration) {
        // eslint-disable-next-line no-throw-literal
        if(!returnType._links) throw `Parent has no _links: ${'$'}{returnType.id}`;
        await this._requestAdapter.adaptAnyToMany(
            apiHelper.removeParamsFromNavigationHref(returnType._links.${name}),
            children.map(c => {
                // eslint-disable-next-line no-throw-literal
                if(!c._links) throw `Child has no _links: ${'$'}{c.id}`;
                return c._links.self.href;
            })
        )
    }

    public async $addToAssociation(returnType: ${entityType.name}, childToAdd: ${type.name}) {
        await this._requestAdapter.addToObj(childToAdd, returnType, "$name");
    }""".doIndent(1)
            else """
    public async $setAssociation(returnType: ${entityType.name}, child: $declaration) {
        // eslint-disable-next-line no-throw-literal
        if(!returnType._links) throw `Parent has no _links: ${'$'}{returnType.id}`;
        // eslint-disable-next-line no-throw-literal
        if(!child._links) throw `Child has no _links: ${'$'}{child.id}`;
        await this._requestAdapter.adaptAnyToOne(
            apiHelper.removeParamsFromNavigationHref(returnType._links.$name),
            child._links.self.href
        );
    }""".doIndent(1)
        }""".trimIndent()
    }.trimIndent()
}"""

private fun searchEntityTemplate(entityType: EntityType, restBasePath: String) = """
    ${
    entityType.searches.join(indent = 1, separator = "\n\n") search@{
        val configurePagingParameters = """
        if (page !== undefined) {
            parameters["page"] = `${'$'}{page}`;
        }
        if (size !== undefined) {
            parameters["size"] = `${'$'}{size}`;
        }
        
    """.doIndent(2)


        val multiplicity = when {
            paging -> RestMultiplicity.PAGED
            list -> RestMultiplicity.LIST
            else -> RestMultiplicity.SINGLE
        }
        val responseType = responseType(multiplicity)
        val responseHandling = responseHandling(multiplicity, entityType.nameRest)

        return@search """
    public async search${name.capitalize()}<T extends ${returnType.name}>(${parameters.paramDecl}${if (parameters.isNotEmpty()) ", " else ""}$pagingParameters): Promise<$returnDeclaration> {
        const request = this._requestAdapter.getRequest();
        
        const parameters: {[key: string]: string | number | boolean | undefined} = {${parameters.join(separator = ", ") { name }}};
        ${if (paging) configurePagingParameters else ""}    
        const url = stringHelper.appendParams("$restBasePath/$path", parameters);
    
        const response = await request.get(url);
        const responseObj = ((await response.json()) as $responseType);
        
        $responseHandling
    }
    
    public async isSearch${name.capitalize()}Allowed(): Promise<boolean> {
        return isEndpointCallAllowed(this._requestAdapter.getRequest(), "/$restBasePath", "GET", "/$restBasePath/$path");
    }""".trimIndent()
    }.trimIndent()
}"""
