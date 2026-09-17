$version: "2.0"

namespace smithy.example

service ExampleService {
    version: "2022-04-01"
    resources: [
        ExampleResource
    ]
}

resource ExampleResource {
    identifiers: {
        id: String
    }
    properties: {
        example: String
        documentation: String
    }
    list: ListExamples
}

@readonly
operation ListExamples {
    output := {
        examples: ExampleList
    }
}

list ExampleList {
    member: Example
}

structure Example for ExampleResource {
    @required
    $id

    @required
    $example

    $documentation
}
