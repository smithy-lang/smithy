// This file defines test cases that test HTTP query string bindings.
// See: https://smithy.io/2.0/spec/http-bindings.html#httpquery-trait and
// https://smithy.io/2.0/spec/http-bindings.html#httpqueryparams-trait

$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use aws.protocoltests.shared#BooleanList
use aws.protocoltests.shared#DoubleList
use aws.protocoltests.shared#FooEnum
use aws.protocoltests.shared#FooEnumList
use aws.protocoltests.shared#IntegerEnum
use aws.protocoltests.shared#IntegerEnumList
use aws.protocoltests.shared#IntegerList
use aws.protocoltests.shared#IntegerSet
use aws.protocoltests.shared#StringList
use aws.protocoltests.shared#StringListMap
use aws.protocoltests.shared#StringMap
use aws.protocoltests.shared#StringSet
use aws.protocoltests.shared#TimestampList
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
        protocol: restJson1,
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
            "Float=1.1",
            "Double=1.1",
            "DoubleList=1.1",
            "DoubleList=2.1",
            "DoubleList=3.1",
            "Boolean=true",
            "BooleanList=true",
            "BooleanList=false",
            "BooleanList=true",
            "Timestamp=1970-01-01T00%3A00%3A01Z",
            "TimestampList=1970-01-01T00%3A00%3A01Z",
            "TimestampList=1970-01-01T00%3A00%3A02Z",
            "TimestampList=1970-01-01T00%3A00%3A03Z",
            "Enum=Foo",
            "EnumList=Foo",
            "EnumList=Baz",
            "EnumList=Bar",
            "IntegerEnum=1",
            "IntegerEnumList=1",
            "IntegerEnumList=2",
            "IntegerEnumList=3",
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
            queryFloat: 1.1,
            queryDouble: 1.1,
            queryDoubleList: [1.1, 2.1, 3.1],
            queryBoolean: true,
            queryBooleanList: [true, false, true],
            queryTimestamp: 1,
            queryTimestampList: [1, 2, 3],
            queryEnum: "Foo",
            queryEnumList: ["Foo", "Baz", "Bar"],
            queryIntegerEnum: 1,
            queryIntegerEnumList: [1, 2, 3],
            queryParamsMapOfStringList: {
                "String": ["Hello there"],
                "StringList": ["a", "b", "c"],
                "StringSet": ["a", "b", "c"],
                "Byte": ["1"],
                "Short": ["2"],
                "Integer": ["3"],
                "IntegerList": ["1", "2", "3"],
                "IntegerSet": ["1", "2", "3"],
                "Long": ["4"],
                "Float": ["1.1"],
                "Double": ["1.1"],
                "DoubleList": ["1.1", "2.1", "3.1"],
                "Boolean": ["true"],
                "BooleanList": ["true", "false", "true"],
                "Timestamp": ["1970-01-01T00:00:01Z"],
                "TimestampList": ["1970-01-01T00:00:01Z", "1970-01-01T00:00:02Z", "1970-01-01T00:00:03Z"],
                "Enum": ["Foo"],
                "EnumList": ["Foo", "Baz", "Bar"],
                "IntegerEnum": ["1"],
                "IntegerEnumList": ["1", "2", "3"]
            },
        }
    },
    {
        id: "RestJsonQueryStringMap",
        documentation: "Handles query string maps",
        protocol: restJson1,
        method: "GET",
        uri: "/AllQueryStringTypesInput",
        body: "",
        queryParams: [
            "QueryParamsStringKeyA=Foo",
            "QueryParamsStringKeyB=Bar",
        ],
        params: {
            queryParamsMapOfStringList: {
                "QueryParamsStringKeyA": ["Foo"],
                "QueryParamsStringKeyB": ["Bar"],
            },
        }
    },
    {
        id: "RestJsonQueryStringEscaping",
        documentation: "Handles escaping all required characters in the query string.",
        protocol: restJson1,
        method: "GET",
        uri: "/AllQueryStringTypesInput",
        body: "",
        queryParams: [
		"String=%20%25%3A%2F%3F%23%5B%5D%40%21%24%26%27%28%29%2A%2B%2C%3B%3D%F0%9F%98%B9",
        ],
        params: {
		queryString: " %:/?#[]@!$&'()*+,;=😹",
		queryParamsMapOfStringList: {
                    "String": [" %:/?#[]@!$&'()*+,;=😹"]
                }
        }
    },
    {
        id: "RestJsonSupportsNaNFloatQueryValues",
        documentation: "Supports handling NaN float query values.",
        protocol: restJson1,
        method: "GET",
        uri: "/AllQueryStringTypesInput",
        body: "",
        queryParams: [
            "Float=NaN",
            "Double=NaN",
        ],
        params: {
            queryFloat: "NaN",
            queryDouble: "NaN",
            queryParamsMapOfStringList: {
                "Float": ["NaN"],
                "Double": ["NaN"],
            }
        }
    },
    {
        id: "RestJsonSupportsInfinityFloatQueryValues",
        documentation: "Supports handling Infinity float query values.",
        protocol: restJson1,
        method: "GET",
        uri: "/AllQueryStringTypesInput",
        body: "",
        queryParams: [
            "Float=Infinity",
            "Double=Infinity",
        ],
        params: {
            queryFloat: "Infinity",
            queryDouble: "Infinity",
            queryParamsMapOfStringList: {
                "Float": ["Infinity"],
                "Double": ["Infinity"],
            }
        }
    },
    {
        id: "RestJsonSupportsNegativeInfinityFloatQueryValues",
        documentation: "Supports handling -Infinity float query values.",
        protocol: restJson1,
        method: "GET",
        uri: "/AllQueryStringTypesInput",
        body: "",
        queryParams: [
            "Float=-Infinity",
            "Double=-Infinity",
        ],
        params: {
            queryFloat: "-Infinity",
            queryDouble: "-Infinity",
            queryParamsMapOfStringList: {
                "Float": ["-Infinity"],
                "Double": ["-Infinity"],
            }
        }
    },
    {
        id: "RestJsonZeroAndFalseQueryValues"
        documentation: "Query values of 0 and false are serialized"
        protocol: restJson1
        method: "GET"
        uri: "/AllQueryStringTypesInput"
        body: ""
        queryParams: [
            "Integer=0"
            "Boolean=false"
        ]
        params: {
            queryInteger: 0
            queryBoolean: false
        }
    }
])

@suppress(["HttpQueryParamsTrait"])
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

    @httpQuery("IntegerEnum")
    queryIntegerEnum: IntegerEnum,

    @httpQuery("IntegerEnumList")
    queryIntegerEnumList: IntegerEnumList,

    @httpQueryParams
    queryParamsMapOfStringList: StringListMap,
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
        protocol: restJson1,
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
        protocol: restJson1,
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
        protocol: restJson1,
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
        documentation: """
                Query parameters must be ignored when serializing the output
                of an operation. As of January 2021, server implementations
                are expected to respond with a JSON object regardless of
                if the output parameters are empty.""",
        protocol: restJson1,
        code: 200,
        headers: {
            "Content-Type": "application/json"
        },
        body: "{}",
        bodyMediaType: "application/json",
        params: {}
    },
    {
        id: "RestJsonIgnoreQueryParamsInResponseNoPayload",
        documentation: """
                This test is similar to RestJsonIgnoreQueryParamsInResponse,
                but it ensures that clients gracefully handle responses from
                the server that do not serialize an empty JSON object.""",
        protocol: restJson1,
        code: 200,
        body: "",
        params: {},
        appliesTo: "client",
    },
])

structure IgnoreQueryParamsInResponseOutput {
    @httpQuery("baz")
    @suppress(["HttpBindingTraitIgnored"])
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
        id: "RestJsonOmitsNullQuery",
        documentation: "Omits null query values",
        protocol: restJson1,
        method: "GET",
        uri: "/OmitsNullSerializesEmptyString",
        body: "",
        params: {
            nullValue: null
        },
        "appliesTo": "client",
    },
    {
        id: "RestJsonSerializesEmptyQueryValue",
        documentation: "Serializes empty query strings",
        protocol: restJson1,
        method: "GET",
        uri: "/OmitsNullSerializesEmptyString",
        body: "",
        queryParams: [
            "Empty=",
        ],
        params: {
            emptyString: "",
        },
    },
    {
        id: "RestJsonServersAcceptStaticQueryParamAsEmptyString",
        documentation: "Servers accept static query params as empty strings.",
        protocol: restJson1,
        method: "GET",
        uri: "/OmitsNullSerializesEmptyString",
        body: "",
        queryParams: [
            "Empty",
        ],
        params: {
            emptyString: "",
        },
        appliesTo: "server"
    },
])

structure OmitsNullSerializesEmptyStringInput {
    @httpQuery("Null")
    nullValue: String,

    @httpQuery("Empty")
    emptyString: String,
}

/// Omits serializing empty lists. Because empty strings are serilized as
/// `Foo=`, empty lists cannot also be serialized as `Foo=` and instead
/// must be omitted.
@http(uri: "/OmitsSerializingEmptyLists", method: "POST")
@tags(["client-only"])
operation OmitsSerializingEmptyLists {
    input: OmitsSerializingEmptyListsInput
}

apply OmitsSerializingEmptyLists @httpRequestTests([
    {
        id: "RestJsonOmitsEmptyListQueryValues",
        documentation: "Supports omitting empty lists.",
        protocol: restJson1,
        method: "POST",
        uri: "/OmitsSerializingEmptyLists",
        body: "",
        queryParams: [],
        params: {
            queryStringList: [],
            queryIntegerList: [],
            queryDoubleList: [],
            queryBooleanList: [],
            queryTimestampList: [],
            queryEnumList: [],
            queryIntegerEnumList: [],
        }
    }
])

structure OmitsSerializingEmptyListsInput {
    @httpQuery("StringList")
    queryStringList: StringList,

    @httpQuery("IntegerList")
    queryIntegerList: IntegerList,

    @httpQuery("DoubleList")
    queryDoubleList: DoubleList,

    @httpQuery("BooleanList")
    queryBooleanList: BooleanList,

    @httpQuery("TimestampList")
    queryTimestampList: TimestampList,

    @httpQuery("EnumList")
    queryEnumList: FooEnumList,

    @httpQuery("IntegerEnumList")
    queryIntegerEnumList: IntegerEnumList,
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
        protocol: restJson1,
        method: "POST",
        uri: "/QueryIdempotencyTokenAutoFill",
        body: "",
        queryParams: [
            "token=00000000-0000-4000-8000-000000000000",
        ],
        appliesTo: "client",
    },
    {
        id: "RestJsonQueryIdempotencyTokenAutoFillIsSet",
        documentation: "Uses the given idempotency token as-is",
        protocol: restJson1,
        method: "POST",
        uri: "/QueryIdempotencyTokenAutoFill",
        body: "",
        queryParams: [
            "token=00000000-0000-4000-8000-000000000000",
        ],
        params: {
            token: "00000000-0000-4000-8000-000000000000"
        },
        appliesTo: "client",
    }
])

structure QueryIdempotencyTokenAutoFillInput {
    @httpQuery("token")
    @idempotencyToken
    token: String,
}

// Clients must make named query members take precedence over unnamed members
// and servers must use all query params in the unnamed map.
@http(uri: "/Precedence", method: "POST")
operation QueryPrecedence {
    input: QueryPrecedenceInput
}

apply QueryPrecedence @httpRequestTests([
    {
        id: "RestJsonQueryPrecedence",
        documentation: "Prefer named query parameters when serializing",
        protocol: restJson1,
        method: "POST",
        uri: "/Precedence",
        body: "",
        queryParams: [
            "bar=named",
            "qux=alsoFromMap"
        ],
        params: {
            foo: "named",
            baz: {
                bar: "fromMap",
                qux: "alsoFromMap"
            }
        },
        appliesTo: "client",
    },
    {
        id: "RestJsonServersPutAllQueryParamsInMap",
        documentation: "Servers put all query params in map",
        protocol: restJson1,
        method: "POST",
        uri: "/Precedence",
        body: "",
        queryParams: [
            "bar=named",
            "qux=fromMap"
        ],
        params: {
            foo: "named",
            baz: {
                bar: "named",
                qux: "fromMap"
            }
        },
        appliesTo: "server",
    }
])

@suppress(["HttpQueryParamsTrait"])
structure QueryPrecedenceInput {
    @httpQuery("bar")
    foo: String,

    @httpQueryParams
    baz: StringMap
}

// httpQueryParams as Map of ListStrings
@http(uri: "/StringListMap", method: "POST")
operation QueryParamsAsStringListMap {
    input: QueryParamsAsStringListMapInput
}

apply QueryParamsAsStringListMap @httpRequestTests([
    {
        id: "RestJsonQueryParamsStringListMap",
        documentation: "Serialize query params from map of list strings",
        protocol: restJson1,
        method: "POST",
        uri: "/StringListMap",
        body: "",
        queryParams: [
            "corge=named",
            "baz=bar",
            "baz=qux"
        ],
        params: {
            qux: "named",
            foo: {
                "baz": ["bar", "qux"]
            }
        },
        appliesTo: "client"
    },
    {
        id: "RestJsonServersQueryParamsStringListMap",
        documentation: "Servers put all query params in map",
        protocol: restJson1,
        method: "POST",
        uri: "/StringListMap",
        body: "",
        queryParams: [
            "corge=named",
            "baz=bar",
            "baz=qux"
        ],
        params: {
            qux: "named",
            foo: {
                "corge": ["named"],
                "baz": ["bar", "qux"]
            }
        },
        appliesTo: "server"
    }
])

@suppress(["HttpQueryParamsTrait"])
structure QueryParamsAsStringListMapInput {
    @httpQuery("corge")
    qux: String,

    @httpQueryParams
    foo: StringListMap
}
