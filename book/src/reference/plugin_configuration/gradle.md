# Gradle

The first step to use FeGen is to include the FeGen plugins for either Typescript, Kotlin or both in your build file.
If you are using gradle, you need to add a dependency in the `buildscript` within your `build.gradle`.
Make sure you have `mavenCentral()` set as one of your `buildscript` repositories.
You will need gradle version `6.0.1` or later for the plugin to work.

```
buildscript {
    repositories {
        ...
        mavenCentral()
    }
    dependencies {
        ...
        classpath "com.github.materna-se.fegen:fegen-web-gradle-plugin:1.0-RC9" // For Typescript
        classpath "com.github.materna-se.fegen:fegen-kotlin-gradle-plugin:1.0-RC9" // For Kotlin
    }
}
```

Then you need to apply the plugin(s) to your project.

```
apply plugin: 'de.materna.fegen.web'
apply plugin: 'de.materna.fegen.kotlin'
```

Applying the plugin enables you to configure the FeGen plugins using the `fegenWeb` and `fegenKotlin` sections.

```
fegenWeb {
    scanPkg = "com.example.your.main.package"
    frontendPath = "../frontend/src/generated-client"
}

fegenKotlin {
    scanPkg = "com.example.your.main.package"
    frontendPath = "../android-app/src/main/kotlin"
    frontendPkg = "com.example.your.main.package.api"
}
```

> You can find all configuration options on the [Configuration Options](./configuration_options.md) page.

If you want to use custom endpoints and are using a Spring version that ships with version 1.0.0 or later of `spring-hateoas`, you will need also need to add `fegen-spring-util` as a dependency.
This ensures that links required by FeGen will be added to the return values of custom endpoints.

```
dependencies {
    ...
    implementation "com.github.materna-se.fegen:fegen-spring-util:1.0-RC9"
}
```

Once you are happy with your configuration, you can run the following command to have FeGen generate the client code in Typescript.

```shell
./gradlew fegenWeb
```

To generate code in Kotlin, replace `fegenWeb` with `fegenKotlin`