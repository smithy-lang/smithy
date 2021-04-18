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
    output: FooRequest
}

@xmlName("CustomFooRequest")
structure FooRequest {
    @xmlFlattened
    listVal: ListOfString,

    @xmlFlattened
    mapVal: MapOfInteger
}
