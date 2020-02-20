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
./gradlew wrapper --gradle-version 6.1.1
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
