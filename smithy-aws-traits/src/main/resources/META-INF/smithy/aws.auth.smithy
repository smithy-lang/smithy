$version: "2.0"

namespace aws.auth

/// Configures an Amazon Cognito User Pools auth scheme.
@authDefinition
@internal
@tags(["internal"])
@trait(selector: "service")
structure cognitoUserPools {
    /// A list of the Amazon Cognito user pool ARNs. Each element is of this
    /// format: `arn:aws:cognito-idp:{region}:{account_id}:userpool/{user_pool_id}`.
    @required
    providerArns: StringList
}

/// [Signature Version 4](https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html)
/// is the process to add authentication information to AWS requests sent by HTTP. For
/// security, most requests to AWS must be signed with an access key, which consists
/// of an access key ID and secret access key. These two keys are commonly referred to
/// as your security credentials.
@authDefinition(
    traits: [unsignedPayload]
)
@externalDocumentation(Reference: "https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html")
@trait(selector: "service")
structure sigv4 {
    /// The signature version 4 service signing name to use in the credential
    /// scope when signing requests. This value SHOULD match the `arnNamespace`
    /// property of the `aws.api#service` trait if present and the `name`
    /// property of the `aws.api#sigv4a` trait if present.
    @externalDocumentation(Reference: "https://docs.aws.amazon.com/general/latest/gr/sigv4-create-string-to-sign.html")
    @length(min: 1)
    @required
    name: String
}

/// Signature Version 4 Asymmetric (SigV4A), an extension of Signature Version 4 (SigV4), is the
/// process to add authentication information to AWS requests sent by HTTP. SigV4A is nearly
/// identical to SigV4, but also uses public-private keys and asymmetric cryptographic signatures
/// for every request. Most notably, SigV4A supports signatures for multi-region API requests.
@authDefinition(
    traits: [unsignedPayload]
)
@externalDocumentation(
    Reference: "https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html"
    Examples: "https://github.com/aws-samples/sigv4a-signing-examples"
)
@trait(selector: "service[trait|aws.auth#sigv4]")
structure sigv4a {
    /// The signature version 4a service signing name to use in the credential
    /// scope when signing requests. This value SHOULD match the `arnNamespace`
    /// property of the `aws.api#service` trait if present and the `name`
    /// property of the `aws.api#sigv4` trait.
    @externalDocumentation(Reference: "https://docs.aws.amazon.com/general/latest/gr/sigv4-create-string-to-sign.html")
    @length(min: 1)
    @required
    name: String
}

/// Indicates that the request payload of a signed request is not to be used
/// as part of the signature.
@trait(selector: "operation")
structure unsignedPayload {}

@private
list StringList {
    member: String
}
