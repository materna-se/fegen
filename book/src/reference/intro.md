# Reference Documentation

The following pages contain detailed information about how FeGen can be used to generate type safe code to access Spring Data REST APIs.

## [Plugin Configuration](./plugin_configuration/intro.md)

This section explains how to set up the FeGen plugin in your backend Spring project.
This includes all available configuration options for FeGen Typescript and FeGen Kotlin as well as instruction on how to include FeGen in a Gradle or Maven build.

## [Data Model](./data_model/intro.md)

The data model section explains how you can use FeGen to make entities and custom endpoints available to your frontend.
It goes into detail about how entities can reference each other, how you can create custom controllers whose endpoints will be callable using FeGen's generated code and how you can exclude certain parts of your backend from FeGen code generation.

## [Retrieving Data](./retrieving/intro.md)

This section is about how you can add projections as well as repository and custom searches to gain a more fine-grained control over what entities are returned to your client and which of their properties are included.
It also explains how you can use the built-in capabilities for paging and sorting.

## [Targets](./targets/intro.md)

The targets section given an overview over the targets that FeGen currently supports.
It explains the differences between generating a Typescript client and generating one in Kotlin for Android or service to service communication.

## [Security](./security.md)

In this section the features of FeGen Security will be explained.
With FeGen Security, you can check the kind of permissions you currently have to access or modify a certain entity or to call a certain endpoint or search.
