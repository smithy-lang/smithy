$version: "2.0"

namespace smithy.example

// Site 1: NODE_ARRAY whose flat width exceeds 120 columns.
@tags(["AaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaA", "BbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbB", "CcccccccccccccccccccccccccccccccccccccccccccC"])
string ArrayOfStrings

// Site 2: NODE_OBJECT whose flat width exceeds 120 columns, nested inside a trait-applied
// outer NODE_OBJECT.
@authorizers({SomeAuthorizer: {scheme: "httpBearerAuth", type: "request", identitySource: "method.request.header.Authorization", uri: "${someAuthorizerInvokeArn}", resultTtlInSeconds: 1000}})
service SomeService { version: "2024-01-01" }

// Site 3: TRAIT_BODY structured-body collapsed onto one line, no nested objects or arrays.
@externalDocumentation(aaaaaaa: "http://aaaaaaaaaaaaaa.com", bbbbbbb: "http://bbbbbbbbbbbbbb.com", ccccccc: "http://cccccccccccccc.com", ddddddd: "http://dddddddddddddd.com")
string ExternalDocsCollapsed
