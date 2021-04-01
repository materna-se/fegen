# Projections

You already got to know a specific kind of projection in the quick start guide: the base projection.
While that one has no further benefits but to make your FeGen client work in the first place, projections in general can be used to fetch related data from different entities at once.

Generally speaking, a projection is an interface that satisfies the following criteria.

- It is located in the package specified by `entityPkg`
- It is annotated with `@Projection`
  - The `types` property of the annotation contains the class of the entity for which the interface is a projection
- It contains a getter for a subset of the fields declared in the entity (In Kotlin, a `val` field is sufficient)
  - If the type an entity field is another entity, the type of the getter in the projection must be a projection of the other entity

The last bullet point sets projections in general apart from base projections, which must not include any fields containing related entities.

You can use projections to include fields that contain related entities.
Assume you have a `User` entity that has a field `address` annotated with `@OneToOne` containing an `Address` entity.
You can then create the following projection.

```java
@Projection(name = "withAddress", types = {User.class})
interface UserWithAddress {
    long getId();
    // ... other fields like first and last name
    Address.BaseProjection getAddress();
}
```

It is important to not return entity classes in projection's getters, but other projections.
Otherwise, issues can arise in the frontend that are caused by link properties missing from certain objects returned by the REST API.

In the frontend, methods corresponding to your projection will be generated in the entity's client that allow you to retrieve all or a single object based on your entity with the fields specified in your projection.
FeGen will also generate an interface for all projections you specify.

```typescript
const users: UserWithAddress[] = (await apiClient.userClient.readProjectionsUserWithAddress()).items;
```