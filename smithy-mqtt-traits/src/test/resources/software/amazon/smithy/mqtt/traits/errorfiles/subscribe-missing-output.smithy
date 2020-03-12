// Subscribe operations must define output structures.

namespace smithy.example

service FooService {
    version: "2020-03-12",
    operations: [Foo]
}

@smithy.mqtt#subscribe("events")
operation Foo {}
