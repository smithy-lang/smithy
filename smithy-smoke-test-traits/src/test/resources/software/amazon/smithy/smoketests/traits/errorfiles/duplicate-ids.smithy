$version: "2.0"

namespace smithy.example

use smithy.test#smokeTests

@smokeTests([
    {
        id: "say_hello", // Conflicts with self and SayHello2
        params: {},
        expect: {
            success: {}
        }
    },
    {
        id: "say_hello", // Conflicts with self and SayHello2
        params: {},
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
        id: "say_hello", // Conflicts with SayHello
        expect: {
            success: {}
        }
    },
    {
        id: "not_say_hello", // No conflict
        expect: {
            success: {}
        }
    }
])
operation SayHello2 {
    input := {}
    output := {}
}
