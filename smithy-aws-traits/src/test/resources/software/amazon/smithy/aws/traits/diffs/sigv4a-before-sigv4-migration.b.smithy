$version: "2.0"

metadata suppressions = [
    {
        id: "ModifiedTrait.Update.smithy.api#auth"
        namespace: "ns.foo"
    }
]

namespace ns.foo

use aws.auth#sigv4
use aws.auth#sigv4a

@auth([sigv4a, sigv4])
@sigv4(name: "service")
@sigv4a(name: "service")
service Service {
    operations: [
        Operation
    ]
}

@auth([sigv4a, sigv4])
operation Operation {}
