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



## Defining the api object

No matter whether you use a separate API project or generate your code right into a subdirectory of your frontend project, there will be the following two files in the directory specified by `frontendPath`:

- `Entities.ts` exports interfaces that correspond to entities and projections defined in the backend.
- `ApiClient.ts` exports the class `ApiClient` which contains a client object for each entity defined in the backend.

To use the generated code, you must instantiate the generated `ApiClient` class first which takes an instance of the runtime's class `FetchAdapter`.

Its constructor takes an object that you can use to configure how the generated code accesses your backend.
The following optional properties will be evaluated by the generated code if they are present:

| Property | Description | Default |
| -------- | ----------- | ------- |
| `baseUrl` | Under which URL your backend can be accessed. No matter if you specify the base URL, if you specified the property `spring.data.rest.base-path` in the `application.properties` of your spring application, it will be considered by the api client, so you must not repeat it in this property  | `""`, so the protocol and host of the current site will be used (works only in browsers) |
| `interceptors` | see below | `[]` (No interceptors) |
| `fetchImpl` | An implementation similar to `window.fetch` that the generated code will use for all its requests. This is needed for example if the code will not run in the browser, but in an environment, where the default `fetch` is not available. Take note that in order to use authentication, you might need an implementation of `fetch` that preserves cookies. | `window.fetch` (works only in browsers) |

It might be a good idea to do instantiate the ApiClient in your frontend project once and export the `ApiClient` instance as a singleton.
For the remainder of this guide it is assumed that the variable `apiClient` contains such an instance.

```typescript
import { ApiClient } from "your-api-project";
import { FetchAdapter } from "@materna-se/fegen-runtime";

export default new ApiClient(new FetchAdapter({
    baseUrl: "https://example.com"
}));
```



## Intercepting requests

Interceptors can be used to manipulate the requests that the generated code makes as well as the responses it gets.
They can be used e.g. to implement authentication, logging or retrying requests.
An interceptor is simply a function that takes a URL and request options as well as a function `executor` that accepts the previous two parameters, makes the actual request and returns a response wrapped into a promise.
The interceptor itself must also return a `Promise<Response>`.
The simplest interceptor looks like this:

```typescript
const noopInterceptor = async (url: string, options: FetchOptions | undefined, executor: FetchFn): Promise<FetchResponse> => {
    return await executor(url, options);
}
```

While this interceptor is useless, as it does not do change anything if it is added, interceptors allow you to manipulate the request before sending it.
The following interceptor can be used for basic authentication:

```typescript
const basicAuthInterceptor: Interceptor = (url, options, execution) => {
  const optionsWithAuth = {
      // Keep all options but the headers 
    ...options,
    headers: {
      // Keep all headers but replace Authorization
      ...options?.headers,
      Authorization: "Basic " + btoa(credentials.username + ":" + credentials.password)
    }
  }
  // Continue with the request, but with included Authorization header
  return execution(url, optionsWithAuth);
}
```

And this one will retry 2 times if a connection error occurred:

```typescript
const retryInterceptor: Interceptor = async (url, options, execution) => {
  let tries = 0;
  while (true) {
    try {
      return await execution(url, options);
    } catch (ex) {
      if (tries < 3) {
        // Probably just a bad connection, lets try again
        tries++;
        await new Promise(resolve => setTimeout(resolve, 1_000));
      } else {
        // It failed 3 times, lets give up
        throw ex;
      }
    }
  }
}
```

You can pass any number of interceptors to the `FetchAdapter` by specifying an array for the corresponding property.
Once the generated code wants to make a request, it will call the first interceptor in the array.
Whenever that interceptor calls the `executor` function, the second interceptor in the array will be called with the `url` and `options` passed to the `executor` by the first interceptor and the third interceptor as `executor` and so forth.
Whenever the last interceptor calls the `executor`, the actual request will be made with `window.fetch` or your `fetchFn` if you specified one.



## Using the API

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
