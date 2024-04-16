$version: "2.0"

metadata suppressions = [
    {
        id: "ModifiedTrait.Update.smithy.api#auth"
        namespace: "ns.foo"
    }
    {
        id: "ModifiedTrait.Update.smithy.rules#endpointRuleSet"
        namespace: "ns.foo"
    }
]

namespace ns.foo

use aws.auth#sigv4
use aws.auth#sigv4a
use smithy.rules#endpointRuleSet

@endpointRuleSet(
    version: "1.0"
    serviceId: "service"
    parameters: {}
    rules: [
        {
            type: "endpoint"
            conditions: []
            endpoint: {
                url: "https://abc.service.com"
                properties: {
                    authSchemes: [
                        {
                            name: "sigv4"
                        }
                        {
                            name: "sigv4a"
                            signingRegionSet: ["*"]
                        }
                    ]
                }
            }
        }
    ]
)
@auth([sigv4, sigv4a])
@sigv4(name: "service1")
@sigv4a(name: "service1")
service Service1 {}

@endpointRuleSet(
    version: "1.0"
    serviceId: "service"
    parameters: {}
    rules: [
        {
            type: "endpoint"
            conditions: []
            endpoint: {
                url: "https://abc.service.com"
                properties: {
                    authSchemes: [
                        {
                            name: "sigv4"
                        }
                        {
                            name: "sigv4a"
                            signingRegionSet: ["*"]
                        }
                    ]
                }
            }
        }
    ]
)
@auth([sigv4])
@sigv4(name: "service2")
@sigv4a(name: "service2")
service Service2 {}

@endpointRuleSet(
    version: "1.0"
    serviceId: "service"
    parameters: {}
    rules: [
        {
            type: "endpoint"
            conditions: []
            endpoint: {
                url: "https://abc.service.com"
                properties: {}
            }
        }
    ]
)
@auth([sigv4, sigv4a])
@sigv4(name: "service3")
@sigv4a(name: "service3")
service Service3 {}

@endpointRuleSet(
    version: "1.0"
    serviceId: "service"
    parameters: {}
    rules: [
        {
            type: "endpoint"
            conditions: []
            endpoint: {
                url: "https://abc.service.com"
                properties: {
                    authSchemes: [
                        {
                            name: "sigv4"
                        }
                        {
                            name: "sigv4a"
                            signingRegionSet: ["*"]
                        }
                    ]
                }
            }
        }
    ]
)
@auth([sigv4])
@sigv4(name: "service4")
@sigv4a(name: "service4")
service Service4 {}

@endpointRuleSet(
    version: "1.0"
    serviceId: "service"
    parameters: {}
    rules: [
        {
            type: "endpoint"
            conditions: []
            endpoint: {
                url: "https://abc.service.com"
                properties: {
                    authSchemes: [
                        {
                            name: "sigv4"
                        }
                        {
                            name: "sigv4a"
                            signingRegionSet: ["*"]
                        }
                    ]
                }
            }
        }
    ]
)
@auth([sigv4, sigv4a])
@sigv4(name: "service5")
@sigv4a(name: "service5")
service Service5 {}

@endpointRuleSet(
    version: "1.0"
    serviceId: "service"
    parameters: {}
    rules: [
        {
            type: "endpoint"
            conditions: []
            endpoint: {
                url: "https://abc.service.com"
                properties: {}
            }
        }
    ]
)
@auth([sigv4, sigv4a])
@sigv4(name: "service6")
@sigv4a(name: "service6")
service Service6 {}
