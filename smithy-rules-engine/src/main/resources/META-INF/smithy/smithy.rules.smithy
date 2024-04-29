$version: "2.0"

namespace smithy.rules

/// Defines an endpoint rule-set used to resolve the client's transport endpoint.
@unstable
@trait(selector: "service")
document endpointRuleSet

/// Defines endpoint test-cases for validating a client's endpoint rule-set.
@unstable
@trait(selector: "service")
structure endpointTests {
    /// The endpoint tests schema version.
    @required
    version: String

    /// List of endpoint test cases.
    testCases: EndpointTestList
}

/// Binds the targeted member of an operation's input structure to the named rule-set parameter.
/// The type of the shape targeted by the trait MUST match the parameter type defined in the rule-set.
@unstable
@trait(selector: "operation -[input]-> structure > member")
structure contextParam {
    /// The rule-set parameter name.
    @required
    name: String
}

/// Binds one or more named rule-set parameters to the defined static value for the targeted operation.
/// The type of the targeted shape targeted by the trait MUST match the parameter type defined in the rule-set.
@unstable
@trait(selector: "operation")
map staticContextParams {
    /// The rule-set parameter name.
    key: String,

    /// The static parameter definition.
    value: StaticContextParamDefinition
}

/// Binds one or more named rule-set parameters to elements contained in the operation's input structure.
/// The type of the shapes targeted by the trait MUST match the parameter types defined in the rule-set.
@unstable
@trait(selector: "operation")
map operationContextParams {
    /// The rule-set parameter name.
    key: String,

    /// The static parameter definition.
    value: OperationContextParamDefinition
}

/// Defines one or more named rule-set parameters to be generated as configurable client parameters.
/// The type specified for the client parameter MUST match the parameter type defined in the rule-set.
@unstable
@trait(selector: "service")
map clientContextParams {
    /// The rule-set parameter name.
    key: String,

    /// The client parameter definition.
    value: ClientContextParamDefinition
}

/// A static context parameter definition.
@unstable
@private
structure StaticContextParamDefinition {
    /// The value to set the associated rule-set parameter to.
    @required
    value: Document
}

/// An operation context parameter definition.
@unstable
@private
structure OperationContextParamDefinition {
    /// a JMESPath expression to select element(s) from the operation input to bind to.
    @required
    path: String
}

/// A client context parameter definition.
@unstable
@private
structure ClientContextParamDefinition {
    /// The Smithy shape type that should be used to generate a client configurable for the rule-set parameter.
    @required
    type: ShapeType,

    /// Documentation string to be generated with the client parameter.
    documentation: String
}

/// An enum representing supported Smithy shape types.
@unstable
@private
enum ShapeType {
    /// Indicates a Smithy string shape type.
    STRING = "string"

    /// Indicates a Smithy boolean shape type.
    BOOLEAN = "boolean"
}

/// A list of endpoint rule-set tests.
@unstable
@private
list EndpointTestList {
    member: EndpointTest
}

/// Describes an endpoint test case for validation of an endpoint rule-set.
@unstable
@private
structure EndpointTest {
    /// Documentation describing the test case.
    documentation: String,

    /// Defines rule-set parameters and values to use for testing rules-engine.
    params: Document,

    /// Defines a set of service operation configurations used for testing the rules-engine.
    operationInputs: OperationInputs,

    /// The expected outcome of the test case.
    @required
    expect: EndpointTestExpectation
}

/// A list of operation input descriptions for an endpoint rule-set test case.
@unstable
@private
list OperationInputs {
    /// The service operation configuration to be used for testing the rules-engine.
    member: OperationInput
}

/// A description of a service operation and input used to verify an endpoint rule-set test case.
@unstable
@private
structure OperationInput {
    /// The name of the service operation targeted by the test.
    @required
    operationName: String,

    /// Defines the input parameters used to generate the operation request.
    /// These parameters MUST be compatible with the input of the operation.
    operationParams: Document,

    /// Defines the set of rule-set built-ins and their corresponding values to be set.
    builtInParams: Document,

    /// Defines the set of client configuration parameters to be set.
    clientParams: Document
}

/// An endpoint rule-set test expectation describing an expected endpoint or error.
@unstable
@private
union EndpointTestExpectation {
    /// A test case expectation resulting in an error.
    error: String,

    /// A test case expectation resulting in an endpoint.
    endpoint: EndpointExpectation
}

/// A description of an expected endpoint to be resolved for an endpoint rule-set test case.
@unstable
@private
structure EndpointExpectation {
    /// The expected endpoint URL to be resolved for this test case.
    url: String,

    /// The transport headers to be set for this test case.
    headers: EndpointHeaders,

    /// The properties for the endpoint for this test case.
    properties: Properties
}

/// A map of header names to list of values.
@unstable
@private
map EndpointHeaders {
    /// The transport header name.
    key: String,

    /// The transport header values.
    value: EndpointHeaderValue
}

/// A list of transport header values.
@unstable
@private
list EndpointHeaderValue {
    /// A transport header value.
    member: String
}

/// A map of strings to document values.
@unstable
@private
map Properties {
    /// The property name.
    key: String,

    /// The property value.
    value: Document
}
