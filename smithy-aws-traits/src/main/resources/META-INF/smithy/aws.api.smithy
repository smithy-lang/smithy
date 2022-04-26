$version: "2.0"

namespace aws.api

/// Specifies an ARN template for the resource.
@externalDocumentation(
    Reference: "https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html"
)
@trait(selector: "resource")
structure arn {
    @required
    template: String
    absolute: Boolean
    noRegion: Boolean
    noAccount: Boolean
}

/// Marks a string as containing an ARN.
@trait(
    selector: "string"
)
structure arnReference {
    type: String
    resource: String
    service: String
}

/// Indicates that the target operation should use the client's endpoint
/// discovery logic.
@trait(selector: "operation")
structure clientDiscoveredEndpoint {
    @required
    required: Boolean
}

/// Configures endpoint discovery for the service.
@trait(selector: "service")
structure clientEndpointDiscovery {
    /// Indicates the operation that clients should use to discover endpoints
    /// for the service.
    @idRef(
        failWhenMissing: true
        selector: "operation"
    )
    @required
    operation: String

    /// Indicates the error that tells clients that the endpoint they are using
    /// is no longer valid. This error MUST be bound to any operation bound to
    /// the service which is marked with the aws.api#clientDiscoveredEndpoint
    /// trait.
    @idRef(
        failWhenMissing: true
        selector: "structure[trait|error]"
    )
    @recommended
    error: String
}

/// Indicates members of the operation input which should be use to discover
/// endpoints.
@trait(
    selector: """
        operation[trait|aws.api#clientDiscoveredEndpoint] -[input]->
        structure > :test(member[trait|required] > string)"""
)
structure clientEndpointDiscoveryId {}

/// Indicates that a structure member SHOULD be unconditionally generated as
/// optional in clients regardless of if the member is required or has a
/// default value. This trait allows documentation generators to indicate that
/// a member is required by the service, even if it is not reflected in
/// generated client code.
@trait(selector: "structure > member")
structure clientOptional {}

/// Defines a service, resource, or operation as operating on the control plane.
@trait(
    selector: ":test(service, resource, operation)"
    conflicts: [
        "aws.api#dataPlane"
    ]
)
structure controlPlane {}

/// Designates the target as containing data of a known classification level.
@trait(selector: ":test(simpleType, collection, structure, union, member)")
enum data {
    /// Customer content means any software (including machine images), data,
    /// text, audio, video or images that customers or any customer end user
    /// transfers to AWS for processing, storage or hosting by AWS services in
    /// connection with the customer’s accounts and any computational results
    /// that customers or any customer end user derive from the foregoing
    /// through their use of AWS services.
    @enumValue("content")
    CUSTOMER_CONTENT

    /// Account information means information about customers that customers
    /// provide to AWS in connection with the creation or administration of
    /// customers’ accounts.
    @enumValue("account")
    CUSTOMER_ACCOUNT_INFORMATION

    /// Service Attributes means service usage data related to a customer’s
    /// account, such as resource identifiers, metadata tags, security and
    /// access roles, rules, usage policies, permissions, usage statistics,
    /// logging data, and analytics.
    @enumValue("usage")
    SERVICE_ATTRIBUTES

    /// Designates metadata tags applied to AWS resources.
    @enumValue("tagging")
    TAG_DATA

    /// Designates security and access roles, rules, usage policies, and
    /// permissions.
    @enumValue("permissions")
    PERMISSIONS_DATA
}

/// Defines a service, resource, or operation as operating on the data plane.
@trait(
    selector: ":test(service, resource, operation)"
    conflicts: [
        "aws.api#controlPlane"
    ]
)
structure dataPlane {}

@trait(selector: "service")
structure service {
    @required
    sdkId: String

    arnNamespace: ArnNamespace

    cloudFormationName: CloudFormationName

    cloudTrailEventSource: String

    endpointPrefix: String
}

@pattern("^[a-z0-9.\\-]{1,63}$")
@private
string ArnNamespace

@pattern("^[A-Z][A-Za-z0-9]+$")
@private
string CloudFormationName
