$version: "2.0"

namespace smithy.example

use smithy.test#smokeTests

@smokeTests([
    {
        id: "success",
        expect: {
            success: {}
        }
    }
])
operation SayHello {}
