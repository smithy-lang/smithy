namespace smithy.example

use smithy.example.nested#Hello
use smithy.example.nested#A
use smithy.example.nested#B
use smithy.example.nested#C
use smithy.example.nested#Resource

service Foo {
    version: "2020-06-11",

    // A "use" resource.
    resources: [Resource],

    // "use" instance operations.
    operations: [Hello, Local]
}

operation Local {
    // "use" input
    input: A,

    // "use" output
    output: B,

    // "use" errors
    errors: [C]
}
