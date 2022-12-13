namespace smithy.example

service Example {
    version: "1.0.0",
    operations: [
        Foo
    ]
}

@http(method: "POST", uri: "/foo")
operation Foo {
    input: FooRequest,
    output: FooResponse
}

@input
@xmlName("CustomFooRequest")
structure FooRequest {
    @xmlFlattened
    listVal: ListOfString,

    @xmlFlattened
    mapVal: MapOfInteger
}

@output
structure FooResponse {}
