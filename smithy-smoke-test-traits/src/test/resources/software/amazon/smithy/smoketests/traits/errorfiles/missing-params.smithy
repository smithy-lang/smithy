$version: "2.0"

namespace smithy.example

use smithy.test#smokeTests

@smokeTests([
    {
        id: "MissingParams"
        expect: {
            failure: {}
        }
    }
])
operation Op {
    input := {
        @required
        requiredInputMember: String
    }
}
