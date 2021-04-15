# Contributing

If you have fixed a bug or added a feature, you are welcome to contribute it back to the main FeGen repository.
There is no CI/CD pipeline yet, but still make sure your pull request fulfills the following criteria:

- Your commit of `fegen` references the correct commit of `fegen-examples` in the `fegen-examples` git subproject
- Running `./gradlew build` is successful in the `fegen` repository's root directory as well as in the `fegen-examples` git subproject
- If you added a feature
  - you have added some tests to make sure it is working and won't be accidentally broken in the future
  - you have added some documentation in the book and ran `mdbook build`