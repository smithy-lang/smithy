// awsQuery does not support inline documents. Because an inline document is
// used in the closure of InvalidExample, this model creates a DANGER event.
$version: "2.0"

namespace smithy.example

use aws.protocols#awsQuery

@awsQuery
@suppress(["DeprecatedTrait"]) // ignore the fact that the awsQuery trait is deprecated
@xmlNamespace(uri: "https://example.com")
service InvalidExample {
    version: "2020-06-15",
    operations: [Operation1]
}

operation Operation1 {
    input: Operation1Input,
    output: Operation1Output
}

@input
structure Operation1Input {
    foo: InlineDocument,
}

document InlineDocument

@output
structure Operation1Output {}
