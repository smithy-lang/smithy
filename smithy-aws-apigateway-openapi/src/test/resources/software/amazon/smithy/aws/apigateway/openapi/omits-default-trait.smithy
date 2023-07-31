$version: "2.0"

namespace example.smithy

use aws.protocols#restJson1

@restJson1
service MyService {
    version: "2020-07-02",
    operations: [HasDefault]
}

@http(method: "POST", uri: "/defaults")
operation HasDefault {
    input := {
        foo: String = ""
        bar: StringList = []
    }
    output := {
        foo: String = ""
        bar: StringList = []
        baz: DefaultEnum = "FOO"
    }
}

list StringList {
    member: String
}

enum DefaultEnum {
    FOO
    BAR
}
