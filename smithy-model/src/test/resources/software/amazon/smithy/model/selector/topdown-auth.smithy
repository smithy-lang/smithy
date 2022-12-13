$version: "2"

namespace smithy.example

@httpBasicAuth
@httpDigestAuth
@httpBearerAuth
service ServiceWithNoAuthTrait {
    version: "2020-01-29",
    operations: [
        OperationWithNoAuthTrait,
        OperationWithAuthTrait
    ]
}

@httpBasicAuth
@httpDigestAuth
@httpBearerAuth
@auth([httpBasicAuth, httpDigestAuth])
service ServiceWithAuthTrait {
    version: "2020-01-29",
    operations: [
        OperationWithNoAuthTrait,
        OperationWithAuthTrait
    ],
    resources: [
        RA
    ]
}

operation OperationWithNoAuthTrait {}

resource RA {
    operations: [OperationWithNoAuthTrait2]
}

@auth([])
operation OperationWithNoAuthTrait2 {}

@auth([httpDigestAuth])
operation OperationWithAuthTrait {}
