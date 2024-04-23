$version: "2.0"

metadata suppressions = [
    {
        id: "ModifiedTrait.Update.smithy.api#auth"
        namespace: "ns.foo"
    }
]

namespace ns.foo

use aws.auth#sigv4

@auth([httpBearerAuth, sigv4])
@httpBearerAuth
@sigv4(name: "service")
service Service {
    operations: [
        Operation
    ]
}

@auth([httpBearerAuth, sigv4])
operation Operation {}
