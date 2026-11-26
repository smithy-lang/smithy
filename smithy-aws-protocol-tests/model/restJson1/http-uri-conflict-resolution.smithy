// This file defines test cases that test the resolution of HTTP URI patterns
// using specificity routing. The operations in this file define URI patterns
// that are ambiguous with each other, and the request test cases define the
// operation that each ambiguous request must be routed to.
// See: https://smithy.io/2.0/spec/http-bindings.html#specificity-routing
$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpRequestTests

/// A literal segment is more specific than a label in the same position, so
/// requests like /abc/bcd/cde are routed to this operation over operations
/// with a label in the second segment.
@readonly
@http(method: "GET", uri: "/abc/bcd/{xyz}")
operation UriConflictAbcBcdLabel {
    input: UriConflictLabelInput
}

apply UriConflictAbcBcdLabel @httpRequestTests([
    {
        id: "RestJsonUriConflictLiteralBeatsLabel"
        documentation: """
            Routes the request to the operation with a literal segment over
            operations with a label in the same segment position. The literal
            segment `bcd` makes this operation the most specific match for the
            ambiguous request path."""
        protocol: restJson1
        method: "GET"
        uri: "/abc/bcd/cde"
        body: ""
        params: { xyz: "cde" }
    }
    {
        id: "RestJsonUriConflictPathSpecificityBeatsQueryLiteral"
        documentation: """
            Routes the request to the operation with the most specific URI path
            when the request also matches a pattern with a query string literal.
            Path specificity wins over query string literals."""
        protocol: restJson1
        method: "GET"
        uri: "/abc/bcd/cde?def=efg"
        body: ""
        params: { xyz: "cde" }
        appliesTo: "server"
    }
])

/// The literal segment `abc` is more specific than a label in the same
/// position, so requests like /abc/foo/cde are routed to this operation over
/// operations with a label in the first segment.
@readonly
@http(method: "GET", uri: "/abc/{xyz}/cde")
operation UriConflictAbcLabelCde {
    input: UriConflictLabelInput
}

apply UriConflictAbcLabelCde @httpRequestTests([
    {
        id: "RestJsonUriConflictLiteralPrefixBeatsLabel"
        documentation: """
            Routes the request to the operation with a literal prefix over
            operations with a label in the same segment position."""
        protocol: restJson1
        method: "GET"
        uri: "/abc/foo/cde"
        body: ""
        params: { xyz: "foo" }
    }
    {
        id: "RestJsonUriConflictQueryLiteralDoesNotOverridePathSpecificity"
        documentation: """
            Routes the request to the operation with the most specific URI path
            even when a query string literal is present in the request."""
        protocol: restJson1
        method: "GET"
        uri: "/abc/foo/cde?def=efg"
        body: ""
        params: { xyz: "foo" }
        appliesTo: "server"
    }
])

/// An operation with a label in the first segment that only matches when no
/// more specific pattern with a literal in that position does.
@readonly
@http(method: "GET", uri: "/{xyz}/bcd/cde")
operation UriConflictLabelBcdCde {
    input: UriConflictLabelInput
}

apply UriConflictLabelBcdCde @httpRequestTests([
    {
        id: "RestJsonUriConflictRoutesToLabelWhenLiteralsDoNotMatch"
        documentation: """
            Routes the request to the operation with a label-only pattern when
            no more specific pattern with a literal segment matches."""
        protocol: restJson1
        method: "GET"
        uri: "/foo/bcd/cde"
        body: ""
        params: { xyz: "foo" }
    }
])

/// An operation with a query string literal that disambiguates its pattern
/// from the path-equivalent /{xyz}/bcd/cde pattern.
@readonly
@http(method: "GET", uri: "/{xyz}/bcd/cde?def=efg")
operation UriConflictLabelBcdCdeQuery {
    input: UriConflictLabelInput
}

apply UriConflictLabelBcdCdeQuery @httpRequestTests([
    {
        id: "RestJsonUriConflictQueryLiteralResolvesEquivalentPath"
        documentation: """
            Routes the request to the operation whose query string literal is
            satisfied by the request. The query string literal disambiguates
            this pattern from the path-equivalent /{xyz}/bcd/cde pattern."""
        protocol: restJson1
        method: "GET"
        uri: "/foo/bcd/cde?def=efg"
        body: ""
        params: { xyz: "foo" }
    }
])

/// An operation with an all-literal pattern that is the most specific match
/// for requests like /abc/def, where the /abc/{xyz} and /abc/{xyz+} patterns
/// also match.
@readonly
@http(method: "GET", uri: "/abc/def")
operation UriConflictAbcLiteral {}

apply UriConflictAbcLiteral @httpRequestTests([
    {
        id: "RestJsonUriConflictLiteralBeatsLabelAndGreedyLabel"
        documentation: """
            Routes the request to the operation with an all-literal pattern
            over operations with a label or a greedy label in the same segment
            position."""
        protocol: restJson1
        method: "GET"
        uri: "/abc/def"
        body: ""
    }
])

/// An operation with a non-greedy label that is more specific than a greedy
/// label in the same segment position.
@readonly
@http(method: "GET", uri: "/abc/{xyz}")
operation UriConflictAbcLabel {
    input: UriConflictLabelInput
}

apply UriConflictAbcLabel @httpRequestTests([
    {
        id: "RestJsonUriConflictLabelBeatsGreedyLabel"
        documentation: """
            Routes the request to the operation with a non-greedy label over an
            operation with a greedy label in the same segment position."""
        protocol: restJson1
        method: "GET"
        uri: "/abc/foo"
        body: ""
        params: { xyz: "foo" }
    }
])

/// An operation with a greedy label that matches requests with more path
/// segments than any other pattern in this file.
@readonly
@http(method: "GET", uri: "/abc/{xyz+}")
operation UriConflictAbcGreedyLabel {
    input: UriConflictLabelInput
}

apply UriConflictAbcGreedyLabel @httpRequestTests([
    {
        id: "RestJsonUriConflictGreedyLabelMatchesMoreSegments"
        documentation: """
            Routes the request to the operation with a greedy label when the
            request path contains more segments than the non-greedy patterns
            can match."""
        protocol: restJson1
        method: "GET"
        uri: "/abc/foo/bar"
        body: ""
        params: { xyz: "foo/bar" }
    }
])

/// The input of operations that bind the `xyz` URI label.
structure UriConflictLabelInput {
    @httpLabel
    @required
    xyz: String
}
