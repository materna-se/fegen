# @FegenIgnore

Sometimes you might want to exclude a [custom endpoint](./custom_endpoints.md) or a [repository](../retrieving/repository_searches.md) or [custom search](../retrieving/custom_searches.md) from FeGen code generation, so FeGen will ignore the respective endpoint.
One of those use cases could be that it uses a feature like file up- or download that FeGen does not currently support.

You can do that by using the annotation `@FegenIgnore` from the `fegen-spring-util` library.
Apply it to a controller class for FeGen to not generate any custom search or custom endpoint code for it or apply it to just one method for FeGen to only ignore that method.

If you annotate an interface that defines a repository, all methods inside that repository will be ignored, so no repository search client code will be generated.
You can also annotate single methods within the repository for that purpose.