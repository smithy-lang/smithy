# Smithy
[![Build Status](https://travis-ci.com/awslabs/smithy.svg?branch=master)](https://travis-ci.com/awslabs/smithy)

**Smithy** is a protocol-agnostic interface definition language and set of
tools for generating clients, servers, and documentation for any programming
language.

* [Smithy Homepage][docs]
* [Specifications][specs]
* [Java API documentation][javadocs]
* [Smithy Gradle Plugin][Smithy Gradle Plugin]

> **NOTE**: Smithy is a developer preview; we will continue to provide updates with new
features, integrations, and fixes.

# Modeling in Smithy

Smithy models define a service as a collection of resources, operations, and
shapes.

```
namespace example.weather

service Weather {
  version: "2006-03-01",
  resources: [City],
  operations: [GetCurrentTime]
}

resource City {
  identifiers: { cityId: CityId },
  read: GetCity,
  list: ListCities,
  resources: [Forecast],
}

// See the full example at https://awslabs.github.io/smithy/quickstart.html#complete-example
```

Find out more about modeling a service with Smithy in the [Quick Start
guide][quickstart].


# Building Smithy models

The [Smithy Gradle Plugin] is the best way to get started with building a
Smithy model. First, create a [`smithy-build.json`] file:

```json
{
    "version": "1.0"
}
```

Then, apply the Smithy Gradle Plugin in your `build.gradle.kts` file and run
`gradle build`:

```kotlin
plugins {
   java
   id("software.amazon.smithy").version("0.4.2")
}
```

Find out more about building artifacts of your Smithy model in the [Building
Smithy Models][building] guide.


# License

This library is licensed under the Apache 2.0 License.

[docs]: https://awslabs.github.io/smithy/
[specs]: https://awslabs.github.io/smithy/spec/
[javadocs]: https://awslabs.github.io/smithy/javadoc/latest/
[quickstart]: https://awslabs.github.io/smithy/quickstart.html
[Smithy Gradle Plugin]: https://github.com/awslabs/smithy-gradle-plugin/
[`smithy-build.json`]: https://awslabs.github.io/smithy/guides/building-models/build-config.html#using-smithy-build-json
[building]: https://awslabs.github.io/smithy/guides/building-models/index.html
