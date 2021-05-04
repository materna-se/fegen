# Publishing

This guide describes the process of publishing all JVM artefacts on Maven Central as well as publishing the web runtime on `npmjs.org`.



## One-time preparation

First you will need an account on [npmjs.com](https://www.npmjs.com) and need to join the `materna-se` organization.
Then you execute `npm login` and enter your credentials in the prompt.

You will also need a [Sonatype Jira Account](https://issues.sonatype.org/secure/Signup!default.jspa) to be able to publish to Maven Central.
It will have to be activated for the groupId `com.github.materna-se` as seen in the following ticket: [https://issues.sonatype.org/browse/OSSRH-55108](https://issues.sonatype.org/browse/OSSRH-55108).
Talk to Jonas Tamimi for details.

While you are waiting for the activation, you can generate a personalized GPG key.
To do that, follow steps 8-10 of the "Prerequisite Steps" of this guide: [https://dzone.com/articles/publish-your-artifacts-to-maven-central](https://dzone.com/articles/publish-your-artifacts-to-maven-central).
Make sure to remember the Key ID from the line `key 27835B3BD2A2061F marked as ultimately trusted`.

Afterwards, perform step 7 of the "Publishing Steps" where you upload your public key to a key server.

To make sure that the FeGen gradle project finds your key, you need to have the following lines in the file `.gradle/gradle.properties` within your home directory:

```properties
mavenCentral.username=your_sonatype_org_username
mavenCentral.password=your_sonatype_org_password
signing.keyId=02468ACE
signing.secretKeyRingFile=/home/clemens/.gradle/secring.gpg
```

The last value points to a file within a directory, where you will have to execute the following command to save your GPG key:

```shell
gpg --export-secret-keys -o secring.gpg
```

The second to last value must be the ID of the key you generated earlier.



## Publishing

1. **Increment all Versions**\
   First you will need to increment the version of all maven artifacts and, if applicable, of the web runtime.
   The first ones reside in the top level `build.gradle` file. I strongly recommend setting all versions here on the same value and thereby accepting also publishing artifacts without changes.
   The version of the web runtime can be adjusted in the file `fegen-web/fegen-web-runtime/package.json`.
   It is important to also adapt all references to FeGen versions. They can be located in the various `build.gradle` files as well as in the documentation within the `book` directory.
   You can use the global (project wide) search to look for the old versions.
   Afterwards, you will need to rebuild the book (Execute `./mdbook build` in `/book`, see [Architecture](./architecture.md#book)).
   When you are done, commit your changes.
2. **Execute Maven Publish**\
   Execute `./gradlew publish` in your root directory. A dialog will pop up where you will need to enter the password for your GPG key.
3. **Confirm Upload to Maven Central**\
   The uploaded files of the `publish` task will end up [here](https://oss.sonatype.org/#welcome).
   Log in using the button in the top right and click on `Staging Repositories` on the left.
   Your upload should be in the list, and you can check whether all files have been successfully uploaded.
   Then you can select each uploaded artifact and click `Close` and `Release` on the toolbar.
4. **Publishing the Web Runtime**\
   Execute `npm publish` in `fegen-web/fegen-web-runtime`.
5. **Merging into the main branch**\
   The `master` branch should always represent the current state.
   That is why you should merge the `develop` branch into it using `--no-ff`.
6. **Tagging**\
   Tag the commit created by the merge with the version number of the Maven artifacts and the version of the web runtime.
7. **Switch back to develop**\
   To prevent accidentally committing on the `master` branch, it is best to immediately switch back to `develop`.