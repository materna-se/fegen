# Setup

In this step you will initialize your backend and frontend project and configure them to use FeGen.

The first thing you should do is create a directory for your project.

```shell
mkdir todo-app
```

## Creating the backend

To create the web server for your application, you can use the spring initializr.
Go to [https://start.spring.io/](https://start.spring.io/).
Select the `Gradle Project` and `Java` radio buttons and change the artifact metadata to `backend` (The name will automaticaly change as well).

> FeGen also supports Maven and Kotlin, but they are not used in this guide in order to keep it simple.
> If you want to know how to configure FeGen with Maven, refer to the [Maven](../reference/plugin_configuration/maven.md) page of the reference documentation.

To use your Spring Boot application as a backend for your web application, you will need to add the following dependencies using the button in the top right:

- `Spring Web`
    - to allow your website to communicate with the backend using REST
- `H2 Database`
    - or another SQL database like PostgreSQL to store data in
- `Spring Data JPA`
    - to simplify accessing the database from your backend code by creating entities
- `Rest Repositories`
    - for your website to be able to directly access your database to via REST

After adding those, click the `Generate` button and download the zip file.
Extract the contained `backend` directory into the `todo-app` directory you created earlier, so your directory structure looks as follows:

```
todo-app/
└── backend/
    ├── build.gradle
    ├── gradle
    ...
```

## Adding FeGen to the backend

In order to generate the client for your backend, you need to add the FeGen plugin to your backend project.
Add the following lines at the top of your `build.gradle` file for gradle to be able to load the FeGen web plugin:

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.github.materna-se.fegen:fegen-web-gradle-plugin:1.0-RC9")
    }
}
```

To actually apply FeGen to the project, add the following line below (not within) the `plugins { ... }` section:

```groovy
apply(plugin = "de.materna.fegen.web")
```

To use custom endpoints with recent Spring versions, add `fegen-spring-util` as a dependency.
This is also a prerequisite for FeGen Security to work, as it provides a Spring controller with meta information about security.

```groovy
dependencies {
    // ...
    implementation("com.github.materna-se.fegen:fegen-spring-util:1.0-RC9")
}
```

> FeGen Security lets you query your current permissions as a client.
> To learn more, refer to the [FeGen Security](../reference/security.md) page of the reference documentation

The last change to the `build.gradle` file is adding the configuration for FeGen web.
Place this below the `dependencies { ... }` section:

```groovy
configure<de.materna.fegen.web.gradle.FeGenWebGradlePluginExtension> {
	scanPkg = "com.example.backend"
	frontendPath = "../frontend/src/api-client"
}
```

The first option tells FeGen where to look for your entities, repositories and controllers.
The second one tells FeGen where to put the generated typescript files that contain the client code.
We will create the referenced directory later.

> You can specify additional configuration options e.g. to control handling of dates or nullable values.
> To learn what configuration options exist, refer to the [Plugin Configuration](../reference/plugin_configuration/plugin_configuration.md) page of the reference documentation.

After those steps, your `build.gradle` should look as follows:

```kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.github.materna-se.fegen:fegen-web-gradle-plugin:1.0-RC9")
    }
}

apply(plugin = "de.materna.fegen.web")

plugins {
    id("org.springframework.boot") version "2.4.5"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.4.32"
    kotlin("plugin.spring") version "1.4.32"
    kotlin("plugin.jpa") version "1.4.32"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("com.github.materna-se.fegen:fegen-spring-util:1.0-RC9")

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configure<de.materna.fegen.web.gradle.FeGenWebGradlePluginExtension> {
    scanPkg = "com.example.backend"
    frontendPath = "../frontend/src/api-client"
}
```

To finish setting up the backend, add the `@Fegen` annotation from the FeGen runtime to your `BackendApplication`, so it looks like this:

```java
@Fegen
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}

```

## Creating the frontend

To create the actual website for your application, first create a directory for your frontend within the `todo-app` directory:

```shell
mkdir frontend
```

> Usually when you create a web application using typescript, you will rely on some sort of framework like React or Angular.
> These usually come with a way to quickly get started and setup up your project.
> Since I do not want to presume knowledge of any specific framework, we will use plain HTML and Typescript in this guide and manually set up the typescript compilation and bundling process.

Initialize this directory as an npm project by creating a `package.json` with the following content:

```json
{
  "name": "frontend",
  "devDependencies": {
    "@rollup/plugin-commonjs": "^17.0.0",
    "@rollup/plugin-node-resolve": "^11.1.0",
    "@rollup/plugin-typescript": "^8.2.1",
    "npm-run-all": "^4.1.5",
    "rollup": "^2.36.2",
    "serve": "^11.3.2",
    "tslib": "^2.1.0",
    "typescript": "^4.2.3"
  },
  "dependencies": {},
  "scripts": {
    "build": "rollup -c",
    "watch": "rollup -c -w",
    "dev": "npm-run-all --parallel start watch",
    "start": "serve public"
  }
}
```

Your frontend project will use rollup to collect your sources and dependencies and compile everything to a single file.
The development dependency `serve` will then be used as a simple HTTP server to deliver your website.

Make sure you have Node.js installed and run the following to install the dependencies declared in the `package.json`:

```shell
npm install
```

We still need to tell rollup how it is supposed to find and compile our source code.
Create a `rollup.config.js` in the `frontend` directory and add the following content:

```javascript
import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import typescript from "@rollup/plugin-typescript";

export default {
    input: 'src/main.ts',
    output: {
        file: 'public/bundle.js',
        format: 'iife', // immediately-invoked function expression — suitable for <script> tags
        sourcemap: true
    },
    plugins: [
        resolve(), // resolve dependencies (such as fegen-runtime) in node_modules
        commonjs(), // converts commonjs dependencies to ES modules
        typescript() // compile typescrip to javascript
    ]
};
```

This file tells rollup that it needs to compile the file `frontend/src/main.ts`.
Create that file and the enclosing directory, and give it the following content:

```typescript
alert("Hello frontend");
```

The rollup configuration file also specifies that the output javascript should be written to `frontend/public/bundle.js`.
Create the `public` directory and an `index.html` file within and add the following content to it:

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Todo App</title>
  <script type="text/javascript" src="bundle.js"></script>
</head>
<body>
<h1>Todo App</h1>
<div id="todoItems">

</div>
</body>
</html>
```

This is the base for your todo application.
You can see a `<script>` tag to import the compiled javascript file and a `<div id="todoItems">` tag where you will add your todo items once you have created your backend.

Run the following command to instruct rollup to compile your typescript file to bundle.js:

```shell
npm run dev
```

Your `index.html` will also be served at `http://localhost:5000` by this command, so you should see a dialog box reading "Hello frontend" when you open that address in your browser.
Your typescript code will also be recompiled each time you change it.

> The steps for creating the frontend up until now were not specific to FeGen and will probably be simpler once you decide for a frontend framework.
> However, there are two steps that you will need to perform in order for FeGen to work in the frontend.


## Adding FeGen to the frontend

First, install the FeGen runtime since it will be needed by the generated code:

```shell
npm install @materna-se/fegen-runtime
```

Then create the directory `frontend/src/api-client` so you have a place where FeGen can put its generated code.

You can now switch to your `backend` directory and run FeGen using the following Gradle command:

```shell
./gradlew fegenWeb
```

Right now FeGen will warn you that it could not find any entities and no custom endpoints.
This is expected as you have not added any to your backend yet.
If you look into the `frontend/src/api-client` directory, you will find some files that FeGen has generated, although they do not have any useful content yet.
To change that by adding some entities to your backend, go to the next page.
