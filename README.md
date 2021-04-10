# Smithy
[![Build Status](https://github.com/awslabs/smithy/workflows/ci/badge.svg)](https://github.com/awslabs/smithy/actions/workflows/ci.yml)

Smithy defines and generates clients, services, and documentation for
any protocol.

* [Smithy Homepage][docs]
* [Specifications][specs]
* [Java API documentation][javadocs]
* [Smithy Gradle Plugin][Smithy Gradle Plugin]


# Smithy IDL

Smithy models define a service as a collection of resources, operations, and
shapes.

```
namespace example.weather

service Weather {
    version: "2006-03-01"
    resources: [City]
    operations: [GetCurrentTime]
}

resource City {
    identifiers: { cityId: CityId }
    read: GetCity
    list: ListCities
    resources: [Forecast]
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
   id("software.amazon.smithy").version("0.5.3")
}
```

Finally, create your first model `model/main.smithy`:

```
namespace com.example

service ExampleService {
    version: "2020-05-27"
}
```

Find out more about building artifacts of your Smithy model in the [Building
Smithy Models][building] guide. For more examples, see the
[examples directory](https://github.com/awslabs/smithy-gradle-plugin/tree/main/examples)
of the Smithy Gradle Plugin repository.

# License

This library is licensed under the Apache 2.0 License.

[docs]: https://awslabs.github.io/smithy/
[specs]: https://awslabs.github.io/smithy/1.0/spec/
[javadocs]: https://awslabs.github.io/smithy/javadoc/latest/
[quickstart]: https://awslabs.github.io/smithy/quickstart.html
[Smithy Gradle Plugin]: https://github.com/awslabs/smithy-gradle-plugin/
[`smithy-build.json`]: https://awslabs.github.io/smithy/1.0/guides/building-models/build-config.html#using-smithy-build-json
[building]: https://awslabs.github.io/smithy/1.0/guides/building-models/index.html
