// This file contains shared types that are used throughout the test cases.
//
// Anything that is generic enough that it could potentially be reused
// should be defined in this file. However, things like input or output
// structures or other test-case specific shapes should be defined closer to
// the test case and in its same file.

$version: "1.0"

metadata suppressions = [
    {
        id: "DeprecatedTrait",
        namespace: "*",
        reason: """
            Some of the AWS protocols make use of deprecated traits, and some are
            themselves deprecated traits. As this package is intended to test those
            protocols, the warnings should be suppressed."""
    },
    // TODO: The following suppressions are temporary until protocol tests are updated
    // to use dedicated input and output shapes, the `@input` and `@output` traits,
    // and suppressions as necessary. This will be easier to do when mixins are
    // launched as part of Smithy IDL 2.0.
    {
        id: "OperationMissingInputTrait",
        namespace: "*",
        reason: """
            This is a temporary solution until we rewrite these tests to define input, output,
            and use the `@input` and `@output` traits."""
    },
    {
        id: "OperationMissingOutputTrait",
        namespace: "*",
        reason: """
            This is a temporary solution until we rewrite these tests to define input, output,
            and use the `@input` and `@output` traits."""
    },
    {
        id: "OperationMissingInput",
        namespace: "*",
        reason: """
            This is a temporary solution until we rewrite these tests to define input, output,
            and use the `@input` and `@output` traits."""
    },
    {
        id: "OperationMissingOutput",
        namespace: "*",
        reason: """
            This is a temporary solution until we rewrite these tests to define input, output,
            and use the `@input` and `@output` traits."""
    }
]

namespace aws.protocoltests.shared

list StringList {
    member: String,
}

@sparse
list SparseStringList {
    member: String
}

set StringSet {
    member: String,
}

map StringMap {
    key: String,
    value: String,
}

map StringListMap {
    key: String,
    value: StringList
}

@sparse
map SparseStringMap {
    key: String,
    value: String,
}

/// A list of lists of strings.
list NestedStringList {
    member: StringList,
}

list IntegerList {
    member: Integer,
}

set IntegerSet {
    member: Integer,
}

list DoubleList {
    member: Double,
}

list BooleanList {
    member: PrimitiveBoolean,
}

list TimestampList {
    member: Timestamp,
}

@enum([
    {
        name: "FOO",
        value: "Foo",
    },
    {
        name: "BAZ",
        value: "Baz",
    },
    {
        name: "BAR",
        value: "Bar",
    },
    {
        name: "ONE",
        value: "1",
    },
    {
        name: "ZERO",
        value: "0",
    },
])
string FooEnum

list FooEnumList {
    member: FooEnum,
}

set FooEnumSet {
    member: FooEnum,
}

map FooEnumMap {
    key: String,
    value: FooEnum,
}

@timestampFormat("date-time")
timestamp DateTime

@timestampFormat("epoch-seconds")
timestamp EpochSeconds

@timestampFormat("http-date")
timestamp HttpDate

@mediaType("text/plain")
blob TextPlainBlob

@mediaType("image/jpeg")
blob JpegBlob

structure GreetingStruct {
    hi: String,
}

list GreetingList {
    member: GreetingStruct
}
