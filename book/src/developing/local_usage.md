# Using a Custom Build Locally

If you want to use a custom build of FeGen in your project, you can install all FeGen plugins to your local `.m2` directory by running the following command in the root project:

```shell
./gradlew publishToMavenLocal
```

To include the built plugins in a gradle project, insert `mavenLocal()` as a repository for your dependencies as well as your buildscript in your `build.gradle`.
If you are using a Kotlin client, you also need to add `mavenLocal()` in the `repositories` section of your client's `build.gradle`.
If you are generating Typescript code, you need to specify the relative path to the `fegen-web/fegen-web-runtime` directory within the FeGen repository in the client's `package.json`:

```json
{
  "dependencies": {
    "@materna-se/fegen-runtime": "../../fegen/fegen-web/fegen-web-runtime"
  }
}
```
