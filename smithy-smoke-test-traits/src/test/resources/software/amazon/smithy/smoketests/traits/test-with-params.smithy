$version: "2.0"

namespace smithy.example

use smithy.test#smokeTests

@smokeTests([
    {
        id: "say_hello",
        params: {
            foo: "bar"
        }
        expect: {
            success: {}
        }
    }
])
operation SayHello {
    input := {
        foo: String
    }
}
