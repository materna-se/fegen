# POJOs

POJO (also known as Plain Old Java Object) refers to simple Java object, which is not annotated with any specific annotation. POJO (or list of them ) can be used to define a custom response body or custom return value in controllers implementation. 
FeGen supports POJOs, which contain primitives as well as references to another POJOs.

The following example (Kotlin) shows how you can define your POJO class and integrate it into the code of custom controllers (definition of custom controllers is described at [Custom endpoints](custom_endpoints.md) page):

```kotlin
class SomePojo(
	@NotNull var string: String,
	@Nullable var number: Double?,
	@Nullable var boolean: Boolean?
)

@RequestMapping("pojo", method = [RequestMethod.POST])
@ResponseBody
fun pojosEndpoint(@RequestBody body: SomePojo): ResponseEntity<SomePojo> {
	return ResponseEntity.ok(body)
}
```

In Kotlin, you may define a POJO using the syntax above. You can annotate the attributes with `@NotNull` and `@Nullable` as well. Then it can be used as a request body (annotated with `@RequestBody`) as well as a return value if you place it as a generic argument of `ResponseEntity`. FeGen recognizes lists of POJOs too.

POJOs can reference other POJOs or contain lists of them. In these cases FeGen generates type definitions for all types of POJOs including referenced ones. 



