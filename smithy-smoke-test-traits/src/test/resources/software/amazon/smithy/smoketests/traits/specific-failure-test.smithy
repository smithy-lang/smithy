$version: "2.0"

namespace smithy.example

use smithy.test#smokeTests

@smokeTests([
    {
        id: "specific_failure",
        expect: {
            failure: {
                errorId: SayHelloError
            }
        }
    }
])
operation SayHello {
    errors: [SayHelloError]
}

@error("server")
structure SayHelloError {}
