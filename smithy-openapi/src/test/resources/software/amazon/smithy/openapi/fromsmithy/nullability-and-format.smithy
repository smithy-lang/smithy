namespace example
use aws.protocols#restJson1

@restJson1
service Example {
    version: "2022-07-10",
    operations: [FooBar]
}

@idempotent
@http(method: "PUT", uri: "/test", code: 200)
operation FooBar {
    input: FooBarInput,
    output: FooBarOutput,
    errors: [FooBarError]
}

@input
structure FooBarInput {
    foo: BoxedInteger,
    file: FilePayload
}

@mediaType("video/quicktime")
blob FilePayload

@output
structure FooBarOutput {
    bar: BoxedInteger,
    baz: MyMap
}

@error("client")
structure FooBarError {
    message: BoxedInteger
}

@box
integer BoxedInteger

map MyMap {
    key: MyEnum,
    value: String
}

@enum([
    {
        value: "FOO"
    },
    {
        value: "BAR"
    }
])
string MyEnum
