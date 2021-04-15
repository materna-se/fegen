# Embeddables

Sometimes, you need a certain set of attributes in multiple entities.
An example for this could be longitude and latitude which should be two fields that every entity should have if a physical location can be associated with it.

Spring supports this by letting you define classes and mark them as embeddable:

```java
@Embeddable
class Location {

    public long longitude;
    
    public long latitude;
}
```

You can then add those attributes to an entity by adding a corresponding field and annotating it with `@Embedded`:

```java
@Entity
public class City {
    
    // ...
    
    @Embedded
    private Location location;
}
```

FeGen will generate a `location` field in the `City` type that will contain the two fields `longitude` and `latitude`.

At the time of writing, FeGen only supports primitive fields in `@Embeddable` classes.
This means that using types other than boolean, numbers and strings (like entities) won't work. 
