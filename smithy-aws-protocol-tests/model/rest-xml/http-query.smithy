// This file defines test cases that test HTTP query string bindings.
// See: https://awslabs.github.io/smithy/spec/http.html#httpquery-trait

$version: "0.5.0"

namespace aws.protocols.tests.restxml

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This example uses all query string types.
@readonly
@http(uri: "/AllQueryStringTypesInput", method: "GET")
operation AllQueryStringTypes(AllQueryStringTypesInput)

apply AllQueryStringTypes @httpRequestTests([
    {
        id: "AllQueryStringTypes",
        description: "Serializes query string parameters with all supported types",
        protocol: "aws.rest-xml",
        method: "GET",
        uri: "/AllQueryStringTypes",
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
            "TimestampList=1",
            "TimestampList=2",
            "TimestampList=3",
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
        id: "ConstantQueryString",
        description: "Includes constant query string parameters",
        protocol: "aws.rest-xml",
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
operation ConstantQueryString(ConstantQueryStringInput)

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
operation ConstantAndVariableQueryString(ConstantAndVariableQueryStringInput)

apply ConstantAndVariableQueryString @httpRequestTests([
    {
        id: "ConstantAndVariableQueryStringMissingOneValue",
        description: "Mixes constant and variable query string parameters",
        protocol: "aws.rest-xml",
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
        id: "ConstantAndVariableQueryStringAllValues",
        description: "Mixes constant and variable query string parameters",
        protocol: "aws.rest-xml",
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
operation IgnoreQueryParamsInResponse() -> IgnoreQueryParamsInResponseOutput

apply IgnoreQueryParamsInResponse @httpResponseTests([
    {
        id: "IgnoreQueryParamsInResponse",
        description: "Query parameters must be ignored when serializing the output of an operation",
        protocol: "aws.rest-xml",
        code: 200,
        headers: {
            "Content-Type": "application/xml"
        },
        body: "<IgnoreQueryParamsInResponseInputOutput><baz>bam</baz></IgnoreQueryParamsInResponseInputOutput>",
        bodyMediaType: "xml",
        params: {
            baz: "bam"
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
operation OmitsNullSerializesEmptyString(OmitsNullSerializesEmptyStringInput)

apply OmitsNullSerializesEmptyString @httpRequestTests([
    {
        id: "OmitsNullSerializesEmptyString",
        description: "Serializes empty query strings but omits null",
        protocol: "aws.rest-xml",
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
operation QueryIdempotencyTokenAutoFill(QueryIdempotencyTokenAutoFillInput)

apply QueryIdempotencyTokenAutoFill @httpRequestTests([
    {
        id: "QueryIdempotencyTokenAutoFill",
        description: "Automatically adds idempotency token when not set",
        protocol: "aws.rest-xml",
        method: "POST",
        uri: "/QueryIdempotencyTokenAutoFill",
        body: "",
        queryParams: [
            "token=00000000-0000-4000-8000-000000000000",
        ]
    },
    {
        id: "QueryIdempotencyTokenAutoFillIsSet",
        description: "Uses the given idempotency token as-is",
        protocol: "aws.rest-xml",
        method: "POST",
        uri: "/QueryIdempotencyTokenAutoFill",
        body: "",
        queryParams: [
            "token=00000000-0000-4000-8000-000000000123",
        ],
        params: {
            token: "00000000-0000-4000-8000-000000000123"
        }
    }
])

structure QueryIdempotencyTokenAutoFillInput {
    @httpQuery("token")
    @idempotencyToken
    token: String,
}
