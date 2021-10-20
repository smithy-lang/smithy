$version: "1.0"

namespace smithy.example

@aws.iam#supportedPrincipalTypes(["IAMUser", "IAMRole"])
service MyService {}

@aws.iam#supportedPrincipalTypes(["Root", "FederatedUser"])
operation MyOperation {}
