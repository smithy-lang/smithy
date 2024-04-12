$version: "2.0"

metadata suppressions = [
    {
        id: "ModifiedTrait.Update.smithy.api#auth"
        namespace: "ns.foo"
    }
]

namespace ns.foo

use aws.auth#sigv4

@auth([sigv4])
@httpBearerAuth
@sigv4(name: "service")
service Service {
    operations: [
        Operation
    ]
}

@auth([sigv4])
operation Operation {}
