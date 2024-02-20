# <img alt="Smithy" src="docs/_static/favicon.png" width="28"> Smithy
[![Build Status](https://github.com/smithy-lang/smithy/workflows/ci/badge.svg)](https://github.com/smithy-lang/smithy/actions/workflows/ci.yml)

Smithy defines and generates clients, services, and documentation for
any protocol.

* [Smithy Homepage][docs]
* [Specifications][specs]
* [Java API documentation][javadocs]
* [Smithy Gradle Plugin][Smithy Gradle Plugin]
* [Awesome Smithy][awesome-smithy]


# Smithy IDL

Smithy models define a service as a collection of resources, operations, and
shapes.

```smithy
$version: "2"

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

// See the full example at https://smithy.io/2.0/quickstart.html#complete-example
```

Find out more about modeling a service with Smithy in the [Quick Start
guide][quickstart].


# Building Smithy models
> [!IMPORTANT]  
> Before you proceed, make sure you have the [Smithy CLI installed](https://smithy.io/2.0/guides/smithy-cli/cli_installation.html#cli-installation).

The [Smithy CLI] is the easiest way to get started with building Smithy models. First, create a [`smithy-build.json`] file:

```json
{
    "version": "1.0",
    "sources": ["model"]
}
```

Next, create your first model `model/main.smithy`:

```smithy
$version: "2"

namespace com.example

service ExampleService {
    version: "2020-05-27"
}
```

Finally, run `smithy build` to build the model with the [Smithy CLI].

Find out more about building artifacts of your Smithy model in the [Building
Smithy Models][building] guide. For more examples, see the
[examples repository](https://github.com/smithy-lang/smithy-examples)

# License

This library is licensed under the Apache 2.0 License.

[docs]: https://smithy.io/
[specs]: https://smithy.io/2.0/spec/
[javadocs]: https://smithy.io/javadoc/latest/
[quickstart]: https://smithy.io/2.0/quickstart.html
[Smithy Gradle Plugin]: https://github.com/awslabs/smithy-gradle-plugin/
[Smithy CLI]: https://smithy.io/2.0/guides/smithy-cli/index.html
[`smithy-build.json`]: https://smithy.io/2.0/guides/building-models/build-config.html#using-smithy-build-json
[building]: https://smithy.io/2.0/guides/building-models/index.html
[awesome-smithy]: https://github.com/smithy-lang/awesome-smithy
