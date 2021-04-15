# Typescript

The typescript target is allows you to use your Spring API in the browser.



## Standalone API project

If you want to share the generated typescript code to access your Spring API, you should generate a separate NPM project for it, so execute the following command in a new directory and follow the instructions:

```shell
npm init
```

Your API project will also need to have a development dependency to Typescript in order to transpile it to javascript as well as a normal dependency to the FeGen runtime which will be used by the generated code:

```shell
npm install --save-dev typescript
npm install @materna-se/fegen-runtime
```

To build the project, you also need to generate a Typescript configuration file.
Install Typescript globally if you haven't already:

```shell
npm install -g typescript
```

Then initialize the configuration in your API project:

```shell
tsc --init
```

You can then further configure Typescript, for example by setting `./dist` as the output directory:

```json
{
  "compilerOptions": {
    // ...
    "outDir": "./dist",
    // ...
  }
}
```

Create an `index.ts` with the following content to conveniently access the generated types and functions:

```typescript
export * from "./ApiClient";
export * from "./Entities";
```

To get the files referenced here, make sure that the `frontendPath` you configured in the backend project's `build.gradle` points to the `src` directory of your NPM project and execute FeGen.

Configure the build script in your `package.json` and specify the correct `main` and `types` files:

```json
{
  // ...
  "main": "./dist/index.js",
  "types": "./dist/index.d.ts",
  "scripts": {
    "build": "npm run tsc",
    // ...
  },
  // ...
}
```

Now you can specify this project as a dependency for your frontends or publish it to a registry.

An example for an API project can be found in `fegen-examples/fegen-example-gradle/web-api` within FeGen's git repository.



## Basic usage

No matter whether you use a separate API project or generate your code right into a subdirectory of your frontend project, there will be the following two files in the directory specified by `frontendPath`:

- `Entities.ts` exports interfaces that correspond to entities and projections defined in the backend.
- `ApiClient.ts` exports the class `ApiClient` which contains a client object for each entity defined in the backend.

If you specified the property `spring.data.rest.base-path` in the `application.properties` of your spring application, you need to include that path in the `baseUrl` parameter when instantiating the `ApiClient`.
You can omit protocol and domain `baseUrl`, which will cause the current website's domain to be used.
It might be a good idea to do instantiate the ApiClient in your frontend project once and export the `ApiClient` instance as a singleton.
For the remainder of this guide it is assumed that the variable `apiClient` contains such an instance.

```typescript
import {ApiClient} from "your-api-project";

export default new ApiClient(undefined, "api");
```

Assume that you have defined an entity class with the name `User`. Then methods for basic CRUD operations will be contained in its client object `apiClient.userClient`.
The operation for reading all objects for an entity takes `page`, `size` and `sort` as parameters.
The latter determines the order of the returned objects.
The other parameters allow you to only query a certain page (starting with offset 0) assuming that the list of entities is divided in pages of size `size`.

The following code demonstrates the basic CRUD functionality offered by FeGen's generated clients:

```typescript
// Create a user
const createdUser: User = await apiClient.userClient.create({
    firstName: "First",
    lastName: "Last"
});

// Get all users
let users: User[] = (await apiClient.userClient.readAll()).items;
// Get 5 users
let pagedUsers: PagedItems<User> = (await apiClient.userClient.readAll(0, 5));
// If there are more than 5 users
if (pagedUsers.page.totalPages > 0) {
    // Get the next 5 users
    users = (await apiClient.userClient.readAll(1, 5)).items;
}
// Get the first 20 users sorted by name
const sortedUsers: User[] = (await apiClient.userClient.readAll(0, 20, "lastName,ASC"));
// Get a single user object by its id
const user: User = await apiClient.userClient.readOne(idOfUser);

// Update a user
createdUser.firstName = "Other First Name";
await apiClient.userClient.update(createdUser);

// Delete a user
await apiClient.userClient.delete(createdUser);
```
