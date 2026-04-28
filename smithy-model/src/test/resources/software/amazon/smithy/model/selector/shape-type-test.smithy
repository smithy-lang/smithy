$version: "2.0"

namespace smithy.example

enum Enum {
    FOO
}

string String

intEnum IntEnum {
    @enumValue(1)
    FOO
}

integer Integer

list List {
    member: String
}

map Map {
    key: String
    value: String
}

structure Structure {
    member: String
}

union Union {
    a: String
    b: Integer
}

service Service {
    version: "1"
    operations: [Operation]
    resources: [Resource]
}

operation Operation {
    input := {}
    output := {}
}

resource Resource {
    identifiers: { id: String }
}
