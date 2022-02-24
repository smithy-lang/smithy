// This file contains shared types that are used throughout the test cases.
//
// Anything that is generic enough that it could potentially be reused
// should be defined in this file. However, things like input or output
// structures or other test-case specific shapes should be defined closer to
// the test case and in its same file.

$version: "2.0"

metadata suppressions = [
    {
        id: "DeprecatedTrait",
        namespace: "*",
        reason: """
            Some of the AWS protocols make use of deprecated traits, and some are
            themselves deprecated traits. As this package is intended to test those
            protocols, the warnings should be suppressed."""
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
    member: Boolean,
}

list TimestampList {
    member: Timestamp,
}

enum FooEnum {
    @enumValue("Foo")
    FOO

    @enumValue("Baz")
    BAZ

    @enumValue("Bar")
    BAR

    @enumValue("1")
    ONE

    @enumValue("0")
    ZERO
}

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
