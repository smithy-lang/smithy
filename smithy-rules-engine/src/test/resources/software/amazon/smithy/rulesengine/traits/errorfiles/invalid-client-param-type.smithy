$version: "1.0"

namespace smithy.example

use smithy.rules#clientContextParams

@clientContextParams(
    invalidParam: {type: "integer"},
)
service ExampleService {
    version: "2022-01-01",
    operations: [GetThing]
}

@readonly
operation GetThing {}
