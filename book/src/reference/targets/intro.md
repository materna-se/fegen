# Targets

This section given an overview over the targets that FeGen currently supports.
It explains the differences between generating a Typescript client and generating one in Kotlin for Android or service to service communication.

You also learn how to create separate projects for your generated API code.
Although FeGen allows you to directly generate code into your frontend projects as you saw in the quick start guide, creating separate projects for the generated code allows you to share it between multiple frontends.

If you generate code in different target languages but for the same Spring application, it will be very similar.
That is why only the setup, and the basic CRUD functionality is explained in this section for each target.
If you want to know how you can use it with other features such as projections or relationships, refer to the corresponding section.
The code there will only be in Typescript, however, Kotlin code is very similar.
