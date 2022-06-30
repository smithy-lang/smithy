$version: "2.0"

namespace smithy.waiters

/// Indicates that an operation has various named "waiters" that can be used
/// to poll a resource until it enters a desired state.
@trait(selector: "operation :not(-[input, output]-> structure > member > union[trait|streaming])")
@length(min: 1)
map waitable {
    key: WaiterName,
    value: Waiter,
}

@pattern("^[A-Z]+[A-Za-z0-9]*$")
string WaiterName

/// Defines an individual operation waiter.
@private
structure Waiter {
    /// Documentation about the waiter. Can use CommonMark.
    documentation: String,

    /// An ordered array of acceptors to check after executing an operation.
    @required
    acceptors: Acceptors,

    /// The minimum amount of time in seconds to delay between each retry.
    /// This value defaults to 2 if not specified. If specified, this value
    /// MUST be greater than or equal to 1 and less than or equal to
    /// `maxDelay`.
    minDelay: WaiterDelay = 2,

    /// The maximum amount of time in seconds to delay between each retry.
    /// This value defaults to 120 if not specified (or, 2 minutes). If
    /// specified, this value MUST be greater than or equal to 1.
    maxDelay: WaiterDelay = 120,

    /// Indicates if the waiter is considered deprecated. A waiter SHOULD
    /// be marked as deprecated if it has been replaced by another waiter or
    /// if it is no longer needed (for example, if a resource changes from
    /// eventually consistent to strongly consistent).
    deprecated: Boolean,

    /// A list of tags associated with the waiter that allow waiters to be
    /// categorized and grouped.
    tags: NonEmptyStringList,
}

@range(min: 1)
integer WaiterDelay

@private
@length(min: 1)
list Acceptors {
    member: Acceptor
}

/// Represents an acceptor in a waiter's state machine.
@private
structure Acceptor {
    /// The state the acceptor transitions to when matched.
    @required
    state: AcceptorState,

    /// The matcher used to test if the resource is in a given state.
    @required
    matcher: Matcher,
}

/// The transition state of a waiter.
@private
enum AcceptorState {
    /// The waiter successfully finished waiting. This is a terminal
    /// state that causes the waiter to stop.
    SUCCESS = "success"

    /// The waiter failed to enter into the desired state. This is a
    /// terminal state that causes the waiter to stop.
    FAILURE = "failure"

    /// The waiter will retry the operation. This state transition is
    /// implicit if no accepter causes a state transition.
    RETRY = "retry"
}

/// Defines how an acceptor determines if it matches the current state of
/// a resource.
@private
union Matcher {
    /// Matches on the successful output of an operation using a
    /// JMESPath expression.
    output: PathMatcher,

    /// Matches on both the input and output of an operation using a JMESPath
    /// expression. Input parameters are available through the top-level
    /// `input` field, and output data is available through the top-level
    /// `output` field. This matcher can only be used on operations that
    /// define both input and output. This matcher is checked only if an
    /// operation completes successfully.
    inputOutput: PathMatcher,

    /// Matches if an operation returns an error and the error matches
    /// the expected error type. If an absolute shape ID is provided, the
    /// error is matched exactly on the shape ID. A shape name can be
    /// provided to match an error in any namespace with the given name.
    errorType: String,

    /// When set to `true`, matches when an operation returns a successful
    /// response. When set to `false`, matches when an operation fails with
    /// any error.
    success: Boolean,
}

/// Defines how to test the result of a JMESPath expression against
/// an expected value.
@private
structure PathMatcher {
    /// A JMESPath expression applied to the input or output of an operation.
    @required
    path: String,

    /// The expected return value of the expression.
    @required
    expected: String,

    /// The comparator used to compare the result of the expression with the
    /// expected value.
    @required
    comparator: PathComparator,
}

/// Defines a comparison to perform in a PathMatcher.
@private
enum PathComparator {
    /// Matches if the return value is a string that is equal to the expected string.
    STRING_EQUALS = "stringEquals"

    /// Matches if the return value is a boolean that is equal to the string literal 'true' or 'false'.
    BOOLEAN_EQUALS = "booleanEquals"

    /// Matches if all values in the list matches the expected string.
    ALL_STRING_EQUALS = "allStringEquals"

    /// Matches if any value in the list matches the expected string.
    ANY_STRING_EQUALS = "anyStringEquals"
}

@private
list NonEmptyStringList {
    member: NonEmptyString,
}

@private
@length(min: 1)
string NonEmptyString
