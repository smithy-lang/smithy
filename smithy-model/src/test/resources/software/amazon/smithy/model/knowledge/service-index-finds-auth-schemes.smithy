$version: "2"

namespace smithy.example

@customAuth
@httpBasicAuth
@httpDigestAuth
@httpBearerAuth
service ServiceWithoutAuthTrait {
    version: "2020-01-29"
    operations: [
        OperationWithoutAuthTrait
        OperationWithAuthTrait
        OperationWithEmptyAuthTrait
        OperationWithOptionalAuthTrait
    ]
}

@customAuth
@httpBasicAuth
@httpDigestAuth
@httpBearerAuth
@auth([httpBasicAuth, httpDigestAuth])
service ServiceWithAuthTrait {
    version: "2020-01-29"
    operations: [
        OperationWithoutAuthTrait
        OperationWithAuthTrait
        OperationWithEmptyAuthTrait
        OperationWithOptionalAuthTrait
    ]
}

@customAuth
@httpBasicAuth
@httpDigestAuth
@httpBearerAuth
@auth([])
service ServiceWithEmptyAuthTrait {
    version: "2020-01-29"
    operations: [
        OperationWithoutAuthTrait
        OperationWithAuthTrait
        OperationWithEmptyAuthTrait
        OperationWithOptionalAuthTrait
    ]
}

service ServiceWithoutAuthDefinitionTraits {
    version: "2020-01-29"
    operations: [
        OperationWithoutAuthTrait
        OperationWithEmptyAuthTrait
        OperationWithOptionalAuthTrait
    ]
}

operation OperationWithoutAuthTrait {}

@auth([httpDigestAuth])
operation OperationWithAuthTrait {}

@auth([])
operation OperationWithEmptyAuthTrait {}

@optionalAuth
operation OperationWithOptionalAuthTrait {}

// Defining a custom trait, to assert that alphabetical sorting of traits takes namespace into account, as well as
// smithy.api#noAuth is added to the end, and not included in sorting.
@trait(
    selector: "service"
    breakingChanges: [
        {change: "remove"}
    ]
)
@authDefinition
structure customAuth {}
