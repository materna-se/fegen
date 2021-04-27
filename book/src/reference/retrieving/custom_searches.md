# Custom Searches

Custom searches are similar to repository searches, but reside in their own controller.
Instead of specifying queries, you are able to write Java or Kotlin code to calculate their response.

You first need to create a controller class that will contain your search methods.
There are no restrictions on how many of those controllers may exist.

```java
@BasePathAwareController
@RequestMapping(path = { "/search" })
class SearchController {
    // ...
}
```

The controller class must meet the following requirements in order for FeGen to detect it:

- It has a `@BasePathAwareController` annotation
- It has a `@RequestMapping` annotation whose value or `path` ends with `/search`

Then you can then add search methods to your controller:

```java
@BasePathAwareController
@RequestMapping(path = { "/search" })
class SearchController {
    
    @RequestMapping("usersByRegex")
    public ResponseEntity<CollectionModel<EntityModel<User.BaseProjection>>> usersByRegex(
            @RequestParam(name = "nameRegex") String name
    ) {
        // ...
        return ResponseEntity.ok(CollectionModel.wrap(userList));
    }
}

```

Those methods must adhere to the following rules to work with FeGen:

- They have a `@RequestMapping` annotation
- Their parameters must have a `@RequestParam` annotation (request body or path variables are not supported yet)
- They have a return type of the form `ResponseEntity<CollectionModel<EntityModel<P>>>` where `P` is a projection

Using a custom search in the frontend works the same as with repository searches.
