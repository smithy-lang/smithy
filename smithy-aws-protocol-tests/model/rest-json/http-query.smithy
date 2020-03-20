// This file defines test cases that test HTTP query string bindings.
// See: https://awslabs.github.io/smithy/spec/http.html#httpquery-trait

$version: "0.5.0"

namespace aws.protocols.tests.restjson

use aws.protocols.tests.shared#BooleanList
use aws.protocols.tests.shared#DoubleList
use aws.protocols.tests.shared#FooEnum
use aws.protocols.tests.shared#FooEnumList
use aws.protocols.tests.shared#IntegerList
use aws.protocols.tests.shared#IntegerSet
use aws.protocols.tests.shared#StringList
use aws.protocols.tests.shared#StringSet
use aws.protocols.tests.shared#TimestampList
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This example uses all query string types.
@readonly
@http(uri: "/AllQueryStringTypesInput", method: "GET")
operation AllQueryStringTypes {
    input: AllQueryStringTypesInput
}

apply AllQueryStringTypes @httpRequestTests([
    {
        id: "RestJsonAllQueryStringTypes",
        documentation: "Serializes query string parameters with all supported types",
        protocol: "aws.rest-json-1.1",
        method: "GET",
        uri: "/AllQueryStringTypesInput",
        body: "",
        queryParams: [
            "String=Hello%20there",
            "StringList=a",
            "StringList=b",
            "StringList=c",
            "StringSet=a",
            "StringSet=b",
            "StringSet=c",
            "Byte=1",
            "Short=2",
            "Integer=3",
            "IntegerList=1",
            "IntegerList=2",
            "IntegerList=3",
            "IntegerSet=1",
            "IntegerSet=2",
            "IntegerSet=3",
            "Long=4",
            "Float=1",
            "Double=1",
            "DoubleList=1.0",
            "DoubleList=2.0",
            "DoubleList=3.0",
            "Boolean=true",
            "BooleanList=true",
            "BooleanList=false",
            "BooleanList=true",
            "Timestamp=1",
            "TimestampList=1970-01-01T00%3A00%3A01Z",
            "TimestampList=1970-01-01T00%3A00%3A02Z",
            "TimestampList=1970-01-01T00%3A00%3A03Z",
            "Enum=Foo",
            "EnumList=Foo",
            "EnumList=Baz",
            "EnumList=Bar",
        ],
        params: {
            queryString: "Hello there",
            queryStringList: ["a", "b", "c"],
            queryStringSet: ["a", "b", "c"],
            queryByte: 1,
            queryShort: 2,
            queryInteger: 3,
            queryIntegerList: [1, 2, 3],
            queryIntegerSet: [1, 2, 3],
            queryLong: 4,
            queryFloat: 1,
            queryDouble: 1,
            queryDoubleList: [1.0, 2.0, 3.0],
            queryBoolean: true,
            queryBooleanList: [true, false, true],
            queryTimestamp: 1,
            queryTimestampList: [1, 2, 3],
            queryEnum: "Foo",
            queryEnumList: ["Foo", "Baz", "Bar"],
        }
    }
])

structure AllQueryStringTypesInput {
    @httpQuery("String")
    queryString: String,

    @httpQuery("StringList")
    queryStringList: StringList,

    @httpQuery("StringSet")
    queryStringSet: StringSet,

    @httpQuery("Byte")
    queryByte: Byte,

    @httpQuery("Short")
    queryShort: Short,

    @httpQuery("Integer")
    queryInteger: Integer,

    @httpQuery("IntegerList")
    queryIntegerList: IntegerList,

    @httpQuery("IntegerSet")
    queryIntegerSet: IntegerSet,

    @httpQuery("Long")
    queryLong: Long,

    @httpQuery("Float")
    queryFloat: Float,

    @httpQuery("Double")
    queryDouble: Double,

    @httpQuery("DoubleList")
    queryDoubleList: DoubleList,

    @httpQuery("Boolean")
    queryBoolean: Boolean,

    @httpQuery("BooleanList")
    queryBooleanList: BooleanList,

    @httpQuery("Timestamp")
    queryTimestamp: Timestamp,

    @httpQuery("TimestampList")
    queryTimestampList: TimestampList,

    @httpQuery("Enum")
    queryEnum: FooEnum,

    @httpQuery("EnumList")
    queryEnumList: FooEnumList,
}

/// This example uses a constant query string parameters and a label.
/// This simply tests that labels and query string parameters are
/// compatible. The fixed query string parameter named "hello" should
/// in no way conflict with the label, `{hello}`.
@readonly
@http(uri: "/ConstantQueryString/{hello}?foo=bar&hello", method: "GET")
@httpRequestTests([
    {
        id: "RestJsonConstantQueryString",
        documentation: "Includes constant query string parameters",
        protocol: "aws.rest-json-1.1",
        method: "GET",
        uri: "/ConstantQueryString/hi",
        queryParams: [
            "foo=bar",
            "hello",
        ],
        body: "",
        params: {
            hello: "hi"
        }
    },
])
operation ConstantQueryString {
    input: ConstantQueryStringInput
}

structure ConstantQueryStringInput {
    @httpLabel
    @required
    hello: String,
}

/// This example uses fixed query string params and variable query string params.
/// The fixed query string parameters and variable parameters must both be
/// serialized (implementations may need to merge them together).
@readonly
@http(uri: "/ConstantAndVariableQueryString?foo=bar", method: "GET")
operation ConstantAndVariableQueryString {
    input: ConstantAndVariableQueryStringInput
}

apply ConstantAndVariableQueryString @httpRequestTests([
    {
        id: "RestJsonConstantAndVariableQueryStringMissingOneValue",
        documentation: "Mixes constant and variable query string parameters",
        protocol: "aws.rest-json-1.1",
        method: "GET",
        uri: "/ConstantAndVariableQueryString",
        queryParams: [
            "foo=bar",
            "baz=bam",
        ],
        forbidQueryParams: ["maybeSet"],
        body: "",
        params: {
            baz: "bam"
        }
    },
    {
        id: "RestJsonConstantAndVariableQueryStringAllValues",
        documentation: "Mixes constant and variable query string parameters",
        protocol: "aws.rest-json-1.1",
        method: "GET",
        uri: "/ConstantAndVariableQueryString",
        queryParams: [
            "foo=bar",
            "baz=bam",
            "maybeSet=yes"
        ],
        body: "",
        params: {
            baz: "bam",
            maybeSet: "yes"
        }
    },
])

structure ConstantAndVariableQueryStringInput {
    @httpQuery("baz")
    baz: String,

    @httpQuery("maybeSet")
    maybeSet: String,
}

/// This example ensures that query string bound request parameters are
/// serialized in the body of responses if the structure is used in both
/// the request and response.
@readonly
@http(uri: "/IgnoreQueryParamsInResponse", method: "GET")
operation IgnoreQueryParamsInResponse {
    output: IgnoreQueryParamsInResponseOutput
}

apply IgnoreQueryParamsInResponse @httpResponseTests([
    {
        id: "RestJsonIgnoreQueryParamsInResponse",
        documentation: "Query parameters must be ignored when serializing the output of an operation",
        protocol: "aws.rest-json-1.1",
        code: 200,
        headers: {
            "Content-Type": "application/json"
        },
        body: "",
        bodyMediaType: "json",
        params: {
        }
    }
])

structure IgnoreQueryParamsInResponseOutput {
    @httpQuery("baz")
    baz: String
}

/// Omits null, but serializes empty string value.
@readonly
@http(uri: "/OmitsNullSerializesEmptyString", method: "GET")
operation OmitsNullSerializesEmptyString {
    input: OmitsNullSerializesEmptyStringInput
}

apply OmitsNullSerializesEmptyString @httpRequestTests([
    {
        id: "RestJsonOmitsNullSerializesEmptyString",
        documentation: "Serializes empty query strings but omits null",
        protocol: "aws.rest-json-1.1",
        method: "GET",
        uri: "/OmitsNullSerializesEmptyString",
        body: "",
        queryParams: [
            "Empty=",
        ],
        params: {
            nullValue: null,
            emptyString: "",
        }
    }
])

structure OmitsNullSerializesEmptyStringInput {
    @httpQuery("Null")
    nullValue: String,

    @httpQuery("Empty")
    emptyString: String,
}

/// Automatically adds idempotency tokens.
@http(uri: "/QueryIdempotencyTokenAutoFill", method: "POST")
@tags(["client-only"])
operation QueryIdempotencyTokenAutoFill {
    input: QueryIdempotencyTokenAutoFillInput
}

apply QueryIdempotencyTokenAutoFill @httpRequestTests([
    {
        id: "RestJsonQueryIdempotencyTokenAutoFill",
        documentation: "Automatically adds idempotency token when not set",
        protocol: "aws.rest-json-1.1",
        method: "POST",
        uri: "/QueryIdempotencyTokenAutoFill",
        body: "",
        queryParams: [
            "token=00000000-0000-4000-8000-000000000000",
        ]
    },
    {
        id: "RestJsonQueryIdempotencyTokenAutoFillIsSet",
        documentation: "Uses the given idempotency token as-is",
        protocol: "aws.rest-json-1.1",
        method: "POST",
        uri: "/QueryIdempotencyTokenAutoFill",
        body: "",
        queryParams: [
            "token=00000000-0000-4000-8000-000000000000",
        ],
        params: {
            token: "00000000-0000-4000-8000-000000000000"
        }
    }
])

structure QueryIdempotencyTokenAutoFillInput {
    @httpQuery("token")
    @idempotencyToken
    token: String,
}
