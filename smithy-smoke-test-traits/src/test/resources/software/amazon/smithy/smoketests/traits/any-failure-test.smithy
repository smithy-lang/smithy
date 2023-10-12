$version: "2.0"

namespace smithy.example

use smithy.test#smokeTests

@smokeTests([
    {
        id: "any_failure",
        expect: {
            failure: {}
        }
    }
])
operation SayHello {}
