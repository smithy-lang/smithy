$version: "2.0"

namespace smithy.test

/// Defines a set of test cases to send to a live service to ensure that a
/// client can successfully connect to a service and get the right kind of
/// response.
@trait(selector: "operation")
list smokeTests {
    /// A list of smoke tests to perform on the operation
    member: SmokeTestCase
}

/// A single smoke test case definition.
@private
structure SmokeTestCase {
    /// The identifier of the test case. This identifier may be used by
    /// smoke test implementations to generate test case names. The provided
    /// `id` MUST match Smithy's `IDENTIFIER` ABNF. No two test cases can
    /// share the same ID, including test cases defined for other operations
    /// bound to the same service.
    @required
    @pattern("^[A-Za-z_][A-Za-z0-9_]+$")
    id: String

    /// Defines the input parameters used to generate the request. These
    /// parameters MUST be compatible with the input of the operation.
    ///
    /// Parameter values that contain binary data MUST be defined using values
    /// that can be represented in plain text as the plain text representation
    /// (for example, use "foo" and not "Zm9vCg==").
    params: Document

    /// Defines vendor-specific parameters that are used to influence the
    /// request. For example, some vendors might utilize environment variables,
    /// configuration files on disk, or other means to influence the
    /// serialization formats used by clients or servers.
    ///
    /// If a `vendorParamsShape` is set, these parameters MUST be compatible with
    /// that shape's definition.
    vendorParams: Document

    /// A shape to be used to validate the `vendorParams` member contents.
    ///
    /// If set, the parameters in `vendorParams` MUST be compatible with this
    /// shape's definition.
    @idRef(failWhenMissing: true)
    vendorParamsShape: String

    /// Defines the response that is expected from the service call. This can
    /// be either a successful response, an error message, or a specific error
    /// response.
    @required
    expect: Expectation

    /// Attaches a list of tags that can be used to categorize and group
    /// test cases. If a test case uses a feature that requires special
    /// configuration, it should be tagged.
    tags: TagList
}

/// The different kinds of expectations that can be made for a test case.
@private
union Expectation {
    /// Indicates that the call is expeted to not throw an error. No other
    /// assertions are made about the response.
    success: Unit

    /// Indicates that the call is expected to throw an error.
    failure: FailureExpectation
}

@private
structure FailureExpectation {
    /// Indicates that the call is expected to throw a specific type of error
    /// matching the targeted shape. If not specified, the error can be of
    /// any type.
    @idRef(failWhenMissing: true, selector: "[trait|error]")
    errorId: String
}

@private
list TagList {
    member: Tag
}

@private
@pattern("^[A-Za-z][A-Za-z0-9_\\-]+$")
string Tag
