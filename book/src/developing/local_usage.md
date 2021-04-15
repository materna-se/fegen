# Using a Custom Build Locally

If you want to use a custom build of FeGen in your project, you can install all FeGen plugins to your local `.m2` directory by running the following command in the root project:

```shell
gradle publishToMavenLocal
```

To include the built plugins in a gradle project, insert `mavenLocal()` as a repository in the buildscript in your `build.gradle`.
