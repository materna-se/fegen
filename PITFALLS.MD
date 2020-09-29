# Common Pitfalls

This document describes common pitfalls you might run into while using FeGen and how you can fix them.

#### Plugin not found

> ```
> > Plugin with id 'de.materna.fegen.web' not found.
> ```

Make sure you added a dependency to the correct fegen plugin.
Its name should end with `gradle-plugin` if you are using gradle or `maven-plugin` if you are using maven.

```
// Wrong:
classpath "com.github.materna-se.fegen:fegen-web:1.0"
// Correct:
classpath "com.github.materna-se.fegen:fegen-web-gradle-plugin:1.0"
```

Also make sure you are using the `fegen-web-*-plugin` to generate Typescript and `fegen-kotlin-*-plugin` to generate Kotlin code.

#### Outdated gradle version

> ```
> > Failed to notify project evaluation listener.
>    > org.gradle.api.tasks.TaskContainer.named(Ljava/lang/String;)Lorg/gradle/api/tasks/TaskProvider;
> ```

Your gradle version is too old.
You can update your gradle wrapper using the following command.
The line `apply plugin: 'de.materna.fegen.web'` must be removed before doing so because Gradle will need to be able to read your `build.gradle` even for updating. 

```shell script
./gradlew wrapper --gradle-version 6.6.1
```

You might need to update your other plugins as well.
Otherwise, you might get an error message such as the following:

> ```
> FAILURE: Build failed with an exception.
> 
> * Where:
> Build file <Line with "apply plugin: 'kotlin'">
> 
> * What went wrong:
> A problem occurred evaluating project '<Your project>'.
> > java.lang.ExceptionInInitializerError (no error message)
> ```

This happens if you are trying to use the Kotlin plugin with the version `1.2` with a recent Gradle version.
Use at least `1.3` to avoid this problem.

#### Nullable fields

You may have encountered the following error message:

```
Field "xyz" in entity "com.example.entity.SomeEntity" is implicitly nullable
    Please add a @Nullable annotation if this is intentional
    or add a @NotNull annotation to forbid null values
    Set implicitNullable to WARN to continue the build despite missing @Nullable annotations
```

Java does not distinguish between types that are nullable and types that are not.
The target languages of FeGen (Typescript and Kotlin) however use different types depending on whether `null` is an allowed value.

If nullability is not explicitly specified using an annotation for an entity's field, by default that field will be treated as nullable by Spring.
Therefore, FeGen will make the corresponding field in Typescript and Kotlin nullable.
This default behavior will cause the null safety features of those languages to become useless for the generated types and may encourage the bad practice of not handling the nullability of a field correctly.

The error message you saw is there to prevent this from happening.
Consider whether `null` is a meaningful value for this field and, if not, add a `@NotNull` annotation to the field.
Otherwise, if nullability is intentional, explicitly mark the field with `@Nullable`.

The default behaviour of FeGen is to fail the build with an error if it encounters a field that is implicitly nullable.
If you just want to display a warning, add the following to the `fegenWeb` or `fegenKotlin` section in your `build.gradle`:

```
implicitNullable = DiagnosticsLevel.WARN
```

To completely ignore implicit nullability, change `WARN` to `ALLOW`