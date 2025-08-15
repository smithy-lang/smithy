$version: "2.0"

namespace smithy.rules

/// Defines an endpoint rule-set used to resolve the client's transport endpoint.
@unstable
@trait(selector: "service")
document endpointRuleSet

/// Defines an endpoint rule-set using a binary decision diagram (BDD).
@unstable
@trait(selector: "service")
structure endpointBdd {
    /// A map of zero or more endpoint parameter names to their parameter configuration.
    @required
    parameters: Parameters

    /// An ordered list of unique conditions used throughout the BDD.
    @required
    conditions: Conditions

    /// An ordered list of results referenced by BDD nodes. The first result is always the terminal node.
    @required
    results: Results

    /// The root node of where to start evaluating the BDD.
    @required
    @range(min: -1)
    root: Integer

    /// The number of nodes contained in the BDD.
    @required
    @range(min: 0)
    nodeCount: Integer

    /// Base64-encoded array of BDD nodes representing the decision graph structure.
    ///
    /// The first node (index 0) is always the terminal node `[-1, 1, -1]` and is included in the nodeCount.
    /// User-defined nodes start at index 1.
    ///
    /// Zig-zag encoding transforms signed integers to unsigned:
    /// - 0 -> 0, -1 → 1, 1 → 2, -2 → 3, 2 → 4, etc.
    /// - Formula: `(n << 1) ^ (n >> 31)`
    /// - This ensures small negative numbers use few bytes
    ///
    /// Each node consists of three varint-encoded integers written sequentially:
    /// 1. variable index
    /// 2. high reference (when condition is true)
    /// 3. low reference (when condition is false)
    ///
    /// Node Structure [variable, high, low]:
    /// - variable: The index of the condition being tested (0 to conditionCount-1)
    /// - high: Reference to follow when the condition evaluates to true
    /// - low: Reference to follow when the condition evaluates to false
    ///
    /// Reference Encoding:
    /// - 0: Invalid/unused reference (never appears in valid BDDs)
    /// - 1: TRUE terminal (treated as "no match" in endpoint resolution)
    /// - -1: FALSE terminal (treated as "no match" in endpoint resolution)
    /// - 2, 3, 4, ...: Node references pointing to nodes[ref-1]
    /// - -2, -3, -4, ...: Complement node references (logical NOT of nodes[abs(ref)-1])
    /// - 2000000+: Result terminals (2000000 + resultIndex)
    ///
    /// Complement edges:
    /// A negative reference represents the logical NOT of the referenced node's entire subgraph. So `-5` means the
    /// complement of node 5 (located in the array at index 4, since `index = |ref| - 1`). In this case, evaluate the
    /// condition referenced by node 4, and if it is TRUE, use the low reference, and if it's FALSE, use the high
    /// reference. This optimization significantly reduces BDD size by allowing a single subgraph to represent both a
    /// boolean function and its complement; instead of creating separate nodes for `condition AND other` and
    /// `NOT(condition AND other)`, we can reuse the same nodes with complement edges. Complement edges cannot be
    /// used on result terminals.
    ///
    /// Example (before encoding):
    /// ```
    /// nodes = [
    ///   [ -1,       1, -1],        // 0: terminal node
    ///   [  0,       3,  2],          // 1: if condition[0] then node 3, else node 2
    ///   [  1, 2000001, -1],   // 2: if condition[1] then result[1], else FALSE
    /// ]
    /// ```
    ///
    /// After zig-zag + varint + base64: `"AQEBAAYEBAGBwOgPAQ=="`
    @required
    nodes: String
}

@private
map Parameters {
    key: String
    value: Parameter
}

/// A rules input parameter.
@private
structure Parameter {
    /// The parameter type.
    @required
    type: ParameterType

    /// True if the parameter is deprecated.
    deprecated: Boolean

    /// Documentation about the parameter.
    documentation: String

    /// Specifies the default value for the parameter if not set.
    /// Parameters with defaults MUST also be marked as required. The type of the provided default MUST match type.
    default: Document

    /// Specifies a named built-in value that is sourced and provided to the endpoint provider by a caller.
    builtIn: String

    /// Specifies that the parameter is required to be provided to the endpoint provider.
    required: Boolean
}

/// The kind of parameter.
enum ParameterType {
    STRING = "string"
    BOOLEAN = "boolean"
    STRING_ARRAY = "stringArray"
}

@private
list Conditions {
    member: Condition
}

@private
structure Condition {
    /// The name of the function to be executed.
    @required
    fn: String

    /// The arguments for the function.
    /// An array of one or more of the following types: string, bool, array, Reference object, or Function object
    @required
    argv: DocumentList

    /// The optional destination variable to assign the functions result to.
    assign: String
}

@private
list DocumentList {
    member: Document
}

@private
list Results {
    member: Result
}

@private
structure Result {
    /// Result type.
    @required
    type: ResultType

    /// An optional description of the result.
    documentation: String

    /// Provided if type is "error".
    error: Document

    /// Provided if type is "endpoint".
    endpoint: EndpointObject

    /// Conditions for the result (only used with decision tree rules).
    conditions: Conditions
}

@private
enum ResultType {
    ENDPOINT = "endpoint"
    ERROR = "error"
}

@private
structure EndpointObject {
    /// The endpoint url. This MUST specify a scheme and hostname and MAY contain port and base path components.
    /// A string value MAY be a Template string. Any value for this property MUST resolve to a string.
    @required
    url: Document

    /// A map containing zero or more key value property pairs. Endpoint properties MAY be arbitrarily deep and
    /// contain other maps and arrays.
    properties: EndpointProperties

    /// A map of transport header names to their respective values. A string value in an array MAY be a
    /// template string.
    headers: EndpointObjectHeaders
}

@private
map EndpointProperties {
    key: String
    value: Document
}

@private
map EndpointObjectHeaders {
    key: String
    value: DocumentList
}

/// Defines endpoint test-cases for validating a client's endpoint rule-set.
@unstable
@trait(selector: "service :is([trait|smithy.rules#endpointRuleSet], [trait|smithy.rules#endpointBdd])")
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
