# Custom endpoints

Custom endpoints are methods inside a controller class that may look as follows.

```java
@RestController
@RequestMapping("/api/todoItems")
public class CustomEndpointController {

    // ...
}
```

If you are using Kotlin, you should add the `open` modifier to the class declaration.

For a controller to be recognized as a container for custom endpoints, it needs to fulfill the following criteria:

- It must have a `@RestController` annotation
- It must have a `@RequestMapping` annotation whose value or `path` that ends with the same segment as an entity's REST endpoint. (For a `User` entity it should end with `/users` and for an `Address` entity it should end with `/addresses`)

Take note that the `spring.data.rest.base-path` you specified in `application.properties` will not be applied to the `@RequestMapping` unless you also add the annotation `@BasePathAwareController`.

You can then specify custom endpoints in such classes where parameters are annotated with `@RequestParam`, `@PathVariable` or `@RequestBody`.

```kotlin
@RequestMapping("createOrUpdate", method = [RequestMethod.POST])
open fun createOrUpdateContact(
        @RequestParam firstName: String,
        @RequestParam lastName: String
): ResponseEntity<EntityModel<User>> {
    // ...
}
```

FeGen will generate a method in the API client corresponding to the `path` or value in `@RequestMapping` if the backend method has a `@RequestMapping` (or `@GetMapping`, `@PostMapping`, etc.) annotation.

Custom endpoints must have a void return type or return entities.
In the latter case, the entity class must be wrapped in an `EntityModel` (if a single entity should be returned) or in `CollectionModel` or `PagedModel` (if multiple entities should be returned).
Those classes themselves must be wrapped in a `ResponseEntity`.

Custom endpoints can also return pojos. In this case, a pojo or a list of those must be wrapped in a `ResponseEntity`.
They may also respond with primitive values in the same way.
Returning `String` is however not supported, since they will not be converted to a JSON string.
Instead, return a `TextNode` wrapped in a `ReponseEntity`.

To reliably use the returned values in the frontend, make sure you added the `fegen-spring-util` dependency to your Spring project.
