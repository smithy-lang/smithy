$version: "2.0"

namespace smithy.example

service MyService {
    version: "1",
    rename: {
        "com.foo#Baz": "Nope"
    }
}
