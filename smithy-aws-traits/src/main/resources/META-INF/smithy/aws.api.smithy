$version: "2.0"

namespace aws.api

/// Specifies an ARN template for the resource.
@externalDocumentation(
    Reference: "https://docs.aws.amazon.com/general/latest/gr/aws-arns-and-namespaces.html"
)
@trait(selector: "resource")
structure arn {
    /// Defines the ARN template. The provided string contains URI-template
    /// style label placeholders that contain the name of one of the identifiers
    /// defined in the `identifiers` property of the resource. These labels can
    /// be substituted at runtime with the actual identifiers of the resource.
    /// Every identifier name of the resource MUST have corresponding label of
    /// the same name. Note that percent-encoding **is not** performed on these
    /// placeholder values; they are to be replaced literally. For relative ARN
    /// templates that have not set `absolute` to `true`, the template string
    /// contains only the resource part of the ARN (for example,
    /// `foo/{MyResourceId}`). Relative ARNs MUST NOT start with "/".
    @required
    template: String

    /// Set to true to indicate that the ARN template contains a fully-formed
    /// ARN that does not need to be merged with the service. This type of ARN
    /// MUST be used when the identifier of a resource is an ARN or is based on
    /// the ARN identifier of a parent resource.
    absolute: Boolean

    /// Set to true to specify that the ARN does not contain a region. If not
    /// set, or if set to false, the resolved ARN will contain a placeholder
    /// for the region. This can only be set to true if `absolute` is not set
    /// or is false.
    noRegion: Boolean

    /// Set to true to specify that the ARN does not contain an account ID. If
    /// not set, or if set to false, the resolved ARN will contain a placeholder
    /// for the customer account ID. This can only be set to true if absolute
    /// is not set or is false.
    noAccount: Boolean
}

/// Marks a string as containing an ARN.
@trait(selector: "string")
structure arnReference {
    /// The AWS CloudFormation resource type contained in the ARN.
    type: String

    /// An absolute shape ID that references the Smithy resource type contained
    /// in the ARN (e.g., `com.foo#SomeResource`). The targeted resource is not
    /// required to be found in the model, allowing for external shapes to be
    /// referenced without needing to take on an additional dependency. If the
    /// shape is found in the model, it MUST target a resource shape, and the
    /// resource MUST be found within the closure of the referenced service
    /// shape.
    resource: String

    /// The Smithy service absolute shape ID that is referenced by the ARN. The
    /// targeted service is not required to be found in the model, allowing for
    /// external shapes to be referenced without needing to take on an
    /// additional dependency.
    service: String
}

/// Indicates that the target operation should use the client's endpoint
/// discovery logic.
@trait(selector: "operation")
structure clientDiscoveredEndpoint {
    /// This field denotes whether or not this operation requires the use of a
    /// specific endpoint. If this field is false, the standard regional
    /// endpoint for a service can handle this request. The client will start
    /// sending requests to the standard regional endpoint while working to
    /// discover a more specific endpoint.
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

/// Defines a service, resource, or operation as operating on the control plane.
@trait(
    selector: ":test(service, resource, operation)"
    conflicts: [
        "aws.api#dataPlane"
    ]
)
structure controlPlane {}

/// Designates the target as containing data of a known classification level.
@trait(selector: ":test(simpleType, list, structure, union, member)")
enum data {
    /// Customer content means any software (including machine images), data,
    /// text, audio, video or images that customers or any customer end user
    /// transfers to AWS for processing, storage or hosting by AWS services in
    /// connection with the customer’s accounts and any computational results
    /// that customers or any customer end user derive from the foregoing
    /// through their use of AWS services.
    CUSTOMER_CONTENT = "content"

    /// Account information means information about customers that customers
    /// provide to AWS in connection with the creation or administration of
    /// customers’ accounts.
    CUSTOMER_ACCOUNT_INFORMATION = "account"

    /// Service Attributes means service usage data related to a customer’s
    /// account, such as resource identifiers, metadata tags, security and
    /// access roles, rules, usage policies, permissions, usage statistics,
    /// logging data, and analytics.
    SERVICE_ATTRIBUTES = "usage"

    /// Designates metadata tags applied to AWS resources.
    TAG_DATA = "tagging"

    /// Designates security and access roles, rules, usage policies, and
    /// permissions.
    PERMISSIONS_DATA = "permissions"
}

/// Defines a service, resource, or operation as operating on the data plane.
@trait(
    selector: ":test(service, resource, operation)"
    conflicts: [
        "aws.api#controlPlane"
    ]
)
structure dataPlane {}

/// An AWS service is defined using the `aws.api#service` trait. This trait
/// provides information about the service like the name used to generate AWS
/// SDK client classes and the namespace used in ARNs.
@trait(selector: "service")
structure service {
    /// The `sdkId` property is a required string value that specifies the AWS
    /// SDK service ID (e.g., "API Gateway"). This value is used for generating
    /// client names in SDKs and for linking between services.
    @required
    sdkId: String

    /// The `arnNamespace` property is a string value that defines the ARN service
    /// namespace of the service (e.g., "apigateway"). This value is used in
    /// ARNs assigned to resources in the service. If not set, this value
    /// defaults to the lowercase name of the service shape.
    arnNamespace: ArnNamespace

    /// The `cloudFormationName` property is a string value that specifies the
    /// AWS CloudFormation service name (e.g., `ApiGateway`). When not set,
    /// this value defaults to the name of the service shape. This value is
    /// part of the CloudFormation resource type name that is automatically
    /// assigned to resources in the service (e.g., `AWS::<NAME>::resourceName`).
    cloudFormationName: CloudFormationName

    /// The `cloudTrailEventSource` property is a string value that defines the
    /// AWS customer-facing eventSource property contained in CloudTrail event
    /// records emitted by the service. If not specified, this value defaults
    /// to the `arnNamespace` plus `.amazonaws.com`.
    cloudTrailEventSource: String

    /// The `docId` property is a string value that defines the identifier
    /// used to implemention linking between service and SDK documentation for
    /// AWS services. If not specified, this value defaults to the `sdkId` in
    /// lower case plus the service `version` property, separated by dashes.
    docId: String

    /// The `endpointPrefix` property is a string value that identifies which
    /// endpoint in a given region should be used to connect to the service.
    /// For example, most services in the AWS standard partition have endpoints
    /// which follow the format: `{endpointPrefix}.{region}.amazonaws.com`. A
    /// service with the endpoint prefix example in the region us-west-2 might
    /// have the endpoint example.us-west-2.amazonaws.com.
    ///
    /// This value is not unique across services and is subject to change.
    /// Therefore, it MUST NOT be used for client naming or for any other
    /// purpose that requires a static, unique identifier. sdkId should be used
    /// for those purposes. Additionally, this value can be used to attempt to
    /// resolve endpoints.
    endpointPrefix: String
}

/// Annotates a service as having tagging on 1 or more resources and associated
/// APIs to perform CRUD operations on those tags
@trait(selector: "service")
@unstable
structure tagEnabled {
    /// The `disableDefaultOperations` property is a boolean value that specifies
    /// if the service does not have the standard tag operations supporting all
    /// resources on the service. Default value is `false`
    disableDefaultOperations: Boolean
}

/// Points to an operation designated for a tagging APi
@idRef(
    failWhenMissing: true
    selector: "resource > operation"
)
string TagOperationReference

/// Structure representing the configuration of resource specific tagging APIs
structure TaggableApiConfig {
    /// The `tagApi` property is a string value that references a non-instance
    /// or create operation that creates or updates tags on the resource.
    @required
    tagApi: TagOperationReference

    /// The `untagApi` property is a string value that references a non-instance
    /// operation that removes tags on the resource.
    @required
    untagApi: TagOperationReference

    /// The `listTagsApi` property is a string value that references a non-
    /// instance operation which gets the current tags on the resource.
    @required
    listTagsApi: TagOperationReference
}

/// Indicates a resource supports CRUD operations for tags. Either through
/// resource lifecycle or instance operations or tagging operations on the
/// service.
@trait(selector: "resource")
@unstable
structure taggable {
    /// The `property` property is a string value that identifies which
    /// resource property represents tags for the resource.
    property: String

    /// Specifies configuration for resource specific tagging APIs if the
    /// resource has them.
    apiConfig: TaggableApiConfig

    /// Flag indicating if the resource is not able to carry AWS system level.
    /// Used by service principals. Default value is `false`
    disableSystemTags: Boolean
}

/// A string representing a service's ARN namespace.
@pattern("^[a-z0-9.\\-]{1,63}$")
@private
string ArnNamespace

/// A string representing a CloudFormation service name.
@pattern("^[A-Z][A-Za-z0-9]+$")
@private
string CloudFormationName
