$version: "2.0"

namespace ns.example

use aws.api#service
use aws.iam#conditionKeys
use aws.iam#defineConditionKeys
use aws.iam#disableConditionKeyInference
use aws.iam#supportedPrincipalTypes

/// MyService is a service for me.
@service(
    sdkId: "My Value"
    arnNamespace: "myservice"
)
@defineConditionKeys(
    "aws:accountId": {
        type: "Numeric"
    }
    "aws:region": {
        type: "String"
    }
    "myservice:pop": {
        type: "String"
    }
    "myservice:snap": {
        type: "String"
    }
    "otherservice:crackle": {
        type: "String"
    }
    "s3:bucket": {
        type: "String"
    }
)
@supportedPrincipalTypes([
    "Root"
    "IAMUser"
    "IAMRole"
    "FederatedUser"
])
@title("My Service")
service MyService {
    version: "0.0.1"
    resources: [
        MyResource
    ]
}

@conditionKeys([
    "aws:region"
    "myservice:pop"
    "myservice:snap"
    "otherservice:crackle"
])
@disableConditionKeyInference
resource MyResource {
    identifiers: {
        buzz: String
        fizz: String
        pop: String
    }
    collectionOperations: [
        MyOperation
    ]
}

@conditionKeys([
    "aws:accountId"
    "s3:bucket"
])
operation MyOperation {
    input := {
        @required
        fizz: String
        @required
        buzz: String
        pop: String
    }
}
