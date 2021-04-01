# Relationships

Entities in your data model may reference each other.
For example, you might want to introduce a `User` entity to the todo list example from the quick start guide that looks like this:

```java
@Entity
class User {
    
    @Id
    @GeneratedValue
    public long id;
    
    @Column(nullable = false)
    public String firstName;
    
    @Column(nullable = false)
    public String lastName;

    @Projection(name = "baseProjection", types = {User.class})
    interface BaseProjection {
        long getId();
        String getFirstName();
        String getLastName();
    }
}
```

## Backend

Now you want to associate each `User` with a list of `TodoItem`s.
You can do this by adding the following attribute to the `TodoItem` entity class:

```java
@ManyToOne(optional = false)
public User creator;
```

Take note that in this case, it is also required to specify whether the `User` is optionally associated with a `TodoItem`, so the `creator` property may be null, or not.
Also you must not add a corresponding getter (or `val` in case of Kotlin) to your base projection, since the base projection should only cover primitive properties.

Now each `TodoItem` is associated with one `User` that you can access in the Typescript frontend as follows (the Kotlin frontend works analogous):

If you want to go the other way around and access all `TodoItem`s that belong to a certain `User`, you have to add the corresponding property to the `User` class:

```java
@OneToMany(mappedBy = "creator")
public List<TodoItem> todos;
```

In this case you do not need to specify nullability because `@OneToMany` and `@ManyToMany` relationships will never be nullable since they may just contain an empty list.

Accessing the `TodoItems` for a `User` works analogous to the reverse access discussed before, but the call to `readTodos` will of course return a promise with an array of values.

If you specify your entities this way, a single `TodoItem` may belong to only a single `User`.
FeGen also supports the use of `@ManyToMany` relationships on both sides to lift this restriction.
In the frontend, accessing related items works the same as with `@OneToMany` relationships.

If you wanted to add a associate an address entity with each user, and each address may only belong to one user, you can use a `@OneToOne` relationship on both sides, which is also supported by FeGen and works like the `@ManyToOne` relationship in the frontend.

## Frontend

To access related entities in the generated code, you have two options.

The first one is to use projections which enable you to fetch an entity and a whole set of (even transitively) related entities in one api call.
To see how they work, refer to the page [Projections](../retrieving/projections.md).

The other one is using the methods that are generated into the API clients of the entities that contain the relationships.

For example, if you added the `creator` property to your `TodoItem` entity, you can now use the `readCreator` method to retrieve `User`s associated with a specific `TodoItem`.

```typescript
const todoItems: TodoItem[] = (await apiClient.todoItemClient.readAll()).items;
const todoItem = todoItems[27];
const user: User = await apiClient.todoItemClient.readCreator(todoItem);
```

Such a `read` method is generated for all relationships mentioned earlier, although `@OneToMany` and `@ManytoMany` relationships will of course return an array instead of a single value.
The corresponding Kotlin code is analogous.

You can use the following methods to add or remove relationships between entities.
It is important that all the objects passed to such a set method have already been created in the backend.
Conversely, the `todosToDelete` object will not be deleted from the database.

```typescript
apiClient.userClient.setTodos(user, todos);
apiClient.userClient.deleteFromTodos(user, todosToDelete);
```

If your relationship is `@OneToMany` or `@ManyToMany`, you can use the following method to add a single object to the relationship without removing the others.

```typescript
apiClient.userClient.addToTodos(room, todoToAdd);
```
