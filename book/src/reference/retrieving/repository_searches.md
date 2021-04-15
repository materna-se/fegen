# Repository Searches

While projections give you fine-grained control over which attributes of an entity are returned, repository searches allow you to filter which entities are returned in the first place.

Spring enables you to create endpoints for searches by specifying methods in a repository interface.
They can either have a query annotation specifying their return value or have their semantics implied by their name.

```java
@RepositoryRestResource
interface ContactRepository extends JpaRepository<Contact, Long> {
    
    @Query("SELECT u FROM User u WHERE u.firstName LIKE CONCAT('%', :name, '%') OR u.lastName LIKE CONCAT('%', :name, '%')")
    Page<Contact> findByNameContaining(
        @RequestParam @Param("name") String name,
        Pageable pageable
    );
}
```

To use such a search method in your frontend, it has to satisfy certain criteria:

- It must be defined in a repository meeting the criteria specified in the Basic Usage section
- Its name must start with `find`
- It must *not* have a `@RestResource` annotation where `exported` is set to `false`

Your repository search may either return an entity, a `List` of entities, or a `Page` of entities.
The latter will enable you to retrieve only a subset of the returned entities by specifying a page and a page size in the frontend.
You will need to add a `Pageable` parameter to your method for this to work.

In your frontend code, a corresponding method will be generated in the client belonging to the repository that contains the search method.

```typescript
apiClient.userClient.searchFindByNameContaining("name_part");
```