# Security

With FeGen security, clients can determine their permissions to access entities, searches and custom endpoints at runtime.
This works by using two endpoints that supply meta information about authorization.
These endpoints are automatically added by adding the `fegen-spring-util` library in your application.

When you call these endpoints, you supply them with a path.
The endpoint then determines whether you are authorized to call that path.
This can be useful if you want to display a UI to the user and show or hide certain elements depending on the user's permissions to retrieve or change related data.

The permissions always apply to your current authentication when making the call.
It does not matter whether that happened using basic HTTP authentication, OIDC or any other method.
The endpoints ask Spring's `WebSecurityConfiguration` class, so all authorization methods configured using Spring will be considered.
If you do not have Spring security enabled, the endpoints will return that you are allowed to do everything.

FeGen generates methods that allow you to determine your permissions for specific entities as well as searches and custom endpoints.

## Entities

To determine what permissions you have on a certain entity, just call the `allowedMethods` method.
It will return an object with boolean fields that tell you whether you can create, read, update or delete that entity.

```typescript
const mayCreateUser: boolean = await apiClient.userClient.allowedMethods().create;
```



## Searches

When FeGen generates a search method named `searchMethod`, another method named `isSearchMethodAllowed` is also generated.
It returns a boolean indicating whether you have the correct permissions to call the former method.

```typescript
apiClient.userClient.isSearchFindByNamesAllowed()
```


## Custom Endpoints

This works the same as searches.
For each custom endpoint method `customEndpoint` that FeGen generates, it also generates another method named `isCustomEndpointAllowed`.

```typescript
if (apiClient.someController.isMyEndpointAllowed()) {
    button.onclick = () => apiClient.someController.myEndpoint("someParameter");
} else {
    button.style = "display: none";
}
```
