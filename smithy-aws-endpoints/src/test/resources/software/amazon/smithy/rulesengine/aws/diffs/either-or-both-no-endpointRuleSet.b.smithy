$version: "2.0"

metadata suppressions = [
    {
        id: "ModifiedTrait.Add.smithy.rules#endpointRuleSet"
        namespace: "ns.foo"
    }
    {
        id: "ModifiedTrait.Remove.smithy.rules#endpointRuleSet"
        namespace: "ns.foo"
    }
]

namespace ns.foo

use smithy.rules#endpointRuleSet

service Service1 {}

service Service2 {}

@endpointRuleSet(
    version: "1.0"
    serviceId: "service"
    parameters: {}
    rules: []
)
service Service3 {}
