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
    ]
}

operation OperationWithNoAuthTrait {}

@auth([httpDigestAuth])
operation OperationWithAuthTrait {}
