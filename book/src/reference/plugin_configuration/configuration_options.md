# Configuration options

The following configuration keys are mandatory:

| Key | Description |
| --- | --- |
| `scanPkg` | The package that contains all entities, projections, repositories and custom endpoints you want to generate frontend code for |
| `frontendPath` | The directory to which the generated (Typescript or Kotlin) files should be written. It must exist and should be specified relative to the `targetProject`. If you are generating Kotlin code, specify the source directory corresponding to the root package. It might be useful to create a sub project for each target language containing solely the generated code in case multiple applications should communicate with the backend. |

The following configuration keys are optional:

| Key | Default value | Description |
| --- | --- | --- |
| `targetProject` | The project the fegen plugin is applied to | The project that contains the spring application for which frontend code should be generated |
| `entityPkg` | Same as `scanPkg` | The package that contains all entities and projections you want to generate frontend code for |
| `repositoryPkg` | Same as `scanPkg` | The package that contains all repositories and custom endpoints you want to generate frontend code for |
| `datesAsString` | `false` | Whether time data such as `java.time.LocalDateTime` should be transmitted as strings |
| `implicitNullable` | `"ERROR"` | How to treat fields of entities that are nullable, but not explicitly annotated with `@Nullable`. Possible values are `"ERROR"`, `"WARN"` and `"ALLOW"` |

The following configuration keys are required when the output of FeGen is Kotlin code (meaning that the fegenKotlin plugin is used):

| Key | Description |
| --- | --- |
| `frontendPkg` | The package to which the frontend code should be generated |