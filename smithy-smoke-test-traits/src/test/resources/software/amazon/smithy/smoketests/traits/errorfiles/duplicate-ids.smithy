$version: "2.0"

metadata suppressions = [
    {
        id: "UnreferencedShape"
        namespace: "smithy.example"
    }
]

namespace smithy.example

use smithy.test#smokeTests

service SayStuff {
    version: "2023-10-11"
    operations: [SayHello, SayHello2]
}

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

// Not bound to service, shouldn't conflict with others that are.
@smokeTests([
    {
        id: "say_hello", // Conflicts with self
        params: {},
        expect: {
            success: {}
        }
    },
    {
        id: "say_hello", // Conflicts with self
        params: {},
        expect: {
            success: {}
        }
    }
])
operation SayHello3 {
    input := {}
    output := {}
}

@smokeTests([
    {
        id: "say_hello", // No conflict
        params: {},
        expect: {
            success: {}
        }
    }
])
operation SayHello4 {
    input := {}
    output := {}
}

service OtherSayStuff {
    version: "2023-10-11"
    operations: [SayHello5, SayHello6]
    resources: [SayHelloResource]
}

// Shouldn't conflict between services
@smokeTests([
    {
        id: "say_hello", // No conflict
        params: {},
        expect: {
            success: {}
        }
    }
])
operation SayHello5 {
   input := {}
   output := {}
}

@smokeTests([
    {
        id: "not_say_hello" // Conflicts with resource bound operation
        params: {}
        expect: {
            success: {}
        }
    }
])
operation SayHello6 {
    input := {}
    output := {}
}

resource SayHelloResource {
    operations: [SayHello7]
}

@smokeTests([
    {
        id: "not_say_hello"
        params: {}
        expect: {
            success: {}
        }
    }
])
operation SayHello7 {
    input := {}
    output := {}
}
