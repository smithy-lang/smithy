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

@auth([sigv4, sigv4a])
@sigv4(name: "service1")
@sigv4a(name: "service1")
service Service1 {
    operations: [
        Operation1
    ]
}

@auth([sigv4, sigv4a])
operation Operation1 {}

@auth([sigv4a, sigv4])
@sigv4(name: "service2")
@sigv4a(name: "service2")
service Service2 {
    operations: [
        Operation2
    ]
}

@auth([sigv4a, sigv4])
operation Operation2 {}
