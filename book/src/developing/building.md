# Building

To build your own version of FeGen, clone this repository if you haven't already using the following command:

```shell script
git clone --recurse-submodules https://github.com/materna-se/fegen.git
```

If you have not used the `--recurse-submodules`, execute the following in the cloned directory:

```shell script
git submodule init
git submodule update
```

Tests are included in the `fegen-examples` directory and can be run by executing the following commands:

```
cd fegen-examples
./gradlew build
``` 

The `fegen-examples` directory consists of a gradle project that contains the line `includeBuild '..'` in its `settings.gradle`.
This causes it to use gradle's composite build feature and ensures that the example and tests inside it always use the FeGen instance built from the sources in the directory where this `README.MD` resides.