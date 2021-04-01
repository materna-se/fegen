# Gradle

The first step to use FeGen is to include the FeGen plugins for either Typescript, Kotlin or both in your build file.
If you are using maven, add the following configuration to the `build/plugins` section of your `pom.xml` to generate a Typescript client using FeGen:

```
<plugin>
    <groupId>com.github.materna-se.fegen</groupId>
    <artifactId>fegen-web-maven-plugin</artifactId>
    <version>1.0-RC9</version>
    <configuration>
        <scanPkg>com.example.your.main.package</scanPkg>
        <frontendPath>../frontend/src/generated-client</frontendPath>
    </configuration>
</plugin>
```

If you want to generate Kotlin instead, use the following:

```
<plugin>
    <groupId>com.github.materna-se.fegen</groupId>
    <artifactId>fegen-kotlin-maven-plugin</artifactId>
    <version>1.0-RC9</version>
    <configuration>
        <scanPkg>com.example.your.main.package</scanPkg>
        <frontendPath>../android-app/src/main/kotlin</frontendPath>
        <frontendPkg>com.example.your.main.package.api</frontendPkg>
    </configuration>
</plugin>
```

> You can find all configuration options on the [Configuration Options](./configuration_options.md) page.
> Add them as XML elements next to `scanPkg` and `frontendPath`.

You can also add both plugins to generate a web and an android or inter service client.

If you want to use custom endpoints and are using a Spring version that ships with version 1.0.0 or later of `spring-hateoas`, you will need also need to add `fegen-spring-util` as a dependency.
This ensures that links required by FeGen will be added to the return values of custom endpoints.

```
<dependency>
    <groupId>com.github.materna-se.fegen</groupId>
    <artifactId>fegen-spring-util</artifactId>
    <version>1.0-RC9</version>
</dependency>
```
