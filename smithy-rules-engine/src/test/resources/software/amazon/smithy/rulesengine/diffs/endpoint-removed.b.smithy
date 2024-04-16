$version: "2.0"

metadata suppressions = [
    {
        id: "ModifiedTrait.Update.smithy.rules#endpointRuleSet"
        namespace: "ns.foo"
    }
]

namespace ns.foo

use smithy.rules#endpointRuleSet

@endpointRuleSet(
    version: "1.0"
    serviceId: "service"
    parameters: {}
    rules: []
)
service Service {}
