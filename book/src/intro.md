# Introduction

FeGen helps you write web and mobile apps with Spring Boot backends.
It does so by generating client code based on your Spring server, so you can access your Spring Data REST API in a type safe manner.
Typescript and Kotlin are supported as frontend languages, so you can use FeGen when creating a web app, a native Android app or another Spring application.

## Why FeGen?

What FeGen basically does is generating types and methods for a client to use the API of a spring application.
In that way, its goals are similar to projects like OpenAPI and GraphQL.
FeGen's disadvantage compared to those technologies is, that it only works for Spring Boot backends.

However, this is also a major advantage, since FeGen can leverage the APIs that Spring Data REST offers by default.
When using FeGen, you are able to simply define a data structure using JPA entities, create the corresponding repositories, and FeGen will give you methods and types to simply create, read, update and delete those entities.
For use cases that are not too complex, this greatly reduces the amount of boilerplate code you need to write.

You can also define repository and custom searches in FeGen that will provide you with only a subset of the available entities.
In more complicated cases, you can also use custom endpoints to have full control over what happens in the backend and what is returned by it.

## Getting started

If you are new to FeGen, please follow the [Quick Start Guide](./quickstart/intro.md) to get an idea of how FeGen works and how you can use it.
Once you finished the [Quick Start Guide](./quickstart/intro.md), you can go more into detail and e.g. learn how to use FeGen to access your backend from Android or another Spring application, by referring to the [Reference Documentation](./reference/intro.md).


