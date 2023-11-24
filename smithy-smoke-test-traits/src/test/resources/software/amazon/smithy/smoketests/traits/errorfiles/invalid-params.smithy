$version: "2.0"

namespace smithy.example

use smithy.test#smokeTests

@smokeTests([
    {
        id: "say_hello",
        params: {
            "foo": "bar"
        },
        expect: {
            success: {}
        }
    }
])
operation SayHello {
    input := {}
    output := {}
}

@smokeTests([
    {
        id: "say_hello_2",
        params: {
            foo: 1
        },
        expect: {
            success: {}
        }
    }
])
operation SayHello2 {
    input := {
        foo: String
    }
    output := {}
}

@smokeTests([
    {
        id: "say_hello_3",
        params: {},
        expect: {
            success: {}
        }
    }
])
operation SayHello3 {
    input := {
        @required
        foo: String
    }
    output := {}
}
