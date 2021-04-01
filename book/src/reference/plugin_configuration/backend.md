# Backend Configuration

In addition to adding and configuring the FeGen plugin, you may need to change some options in your `application.properties` in order for FeGen to work.

## Base Path

If you use a development server provided by your frontend framework (such as React) it is convenient to enable the proxying of requests that the frontend cannot handle to your backend server.
In React this can be done by adding the following property to your `package.json`.

```
"proxy": "http://localhost:8080"
```

However, if you use this setup, you might run into two issues.

The first one arises from conflicts of endpoints that you use in your frontend as well as your backend.
For example, a GET request to `/users` may cause your frontend server to return an HTML page with a list of users, while your backend might return a JSON list of available users.
To avoid problems associated with URL clashes like that, you can specify a base path for your backend.

```
spring.data.rest.base-path=api
```

## Forward Headers Strategy

The Spring server must know how it can be reached, because when it returns an entity, it also includes links to manipulate this entity and related entities in its response and FeGen relies on those links to be correct.
Some proxies (such as the development server used by React) will however not include the original host name in the HTTP request, but instead place it in a header like `X-Forwarded-Host`.

**Trusting those headers may however impose a security risk**, as they can be added by malicious clients if do not implement measures to prevent that (See [this](https://docs.spring.io/spring-framework/docs/5.2.6.RELEASE/spring-framework-reference/web.html#filters-forwarded-headers) section of the Spring documentation).
You can tell spring to trust those headers by adding the following to your `application.properties`.

```properties
server.forward-headers-strategy=framework
```