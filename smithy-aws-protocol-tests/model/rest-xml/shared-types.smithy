// This file contains shared types that are used throughout the rest-xml
// test cases. Anything that is generic enough that it could potentially
// be reused should be defined in this file. However, things like input
// or output structures or other test-case specific shapes should be
// defined closer to the test case and in its same file.

$version: "0.5.0"

namespace aws.protocols.tests.restxml

list StringList {
    member: String,
}

set StringSet {
    member: String,
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

@enum(
    Foo: {},
    Baz: {},
    Bar: {},
    "1": {},
    "0": {},
)
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
