# Architecture

Once you have successfully built FeGen and ran the tests, you can focus on making the changes you have in mind.
This page gives an overview over the directory structure of the FeGen repository.



## Project structure

The actual code within the FeGen repository is structured in three ways.

First, the examples and tests are contained inside the `fegen-examples` git submodule, while the actual implementation resides in the `fegen-core`, `fegen-kotlin` and `fegen-web` directories.

Second, FeGen is divided by the target language (the language that client code should be generated in).
Target specific code can be found in `fegen-kotlin` and `fegen-web` (Typescript), while code that is agnostic of the target language, like the analysis of the Spring application, is contained in `fegen-core`.

Third, FeGen is divided by whether code is specific to Maven or Gradle or whether it belongs to a runtime.
For example, in `fegen-core`, `fegen-gradle` and `fegen-maven` contain code specific to the Maven and Gradle plugins while `fegen-spring-util` is the library that will be included in the Spring application at runtime.
Most code, however is executed when building a project that uses FeGen and is not specific to Maven or Gradle.
That code lies in a `src` directory just inside `fegen-core`.



## /fegen-examples

The example repository that is included as a git submodule here also contains the tests for FeGen.
By using an `includeBuild` statement in the `settings.gradle`, the examples and tests are always run with the current FeGen code in the repository.
The maven example only illustrates how FeGen can be included in a Maven project.
Since FeGen behaves the same no matter if it is included using Gradle or Maven, the tests and the usage examples are inside the `fegen-example-gradle` directory.





## /book

This directory contains the sources for the site you are looking at right now.
The sources consist of markdown files that are rendered into this structured HTML book format using `mdbook`.
When you are adding or modifying features, you should adapt the book.

To do that, first go to the [mdbook repository](https://github.com/rust-lang/mdBook) to download the latest `mdbook` binary for your OS to the `/book` directory and execute it:

```shell
./mdbook serve
```

This will start a development server so you get a live preview of your changes.
Once you are done, call `./mdbook build`.



## /docs

This is the directory where `mdbook` will output the HTML for the book.
As an output directory, this would usually be on the `.gitignore` list, but it needs to be included in the git, so it can be served with GitHub pages.
Do not make any manual changes to this directory, but call `./mdbook build` in the `/book` directory to generate the contents.



## /buildSrc

This directory contains the license header plugin that is used by Gradle to check that each file in the repository has the correct license header set.