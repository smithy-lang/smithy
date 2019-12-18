$version: "0.5.0"

namespace aws.protocols.tests.restxml

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// A REST XML service that sends XML requests and responses.
@protocols([{"name": "aws.rest-xml"}])
service RestXml {
    version: "2019-12-16",
    operations: [
        NoInputAndNoOutput,
        NoInputAndOutput,
        EmptyInputAndEmptyOutput,
    ]
}

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has no input or output.
/// While this should be rare, code generators must support this.
@http(uri: "/NoInputAndNoOutput", method: "POST")
operation NoInputAndNoOutput()

apply NoInputAndNoOutput @httpRequestTests([
    {
        id: "NoInputAndNoOutput",
        description: "No input serializes no payload",
        protocol: "aws.rest-xml",
        method: "POST",
        uri: "/NoInputAndOutput",
        body: ""
    }
])

apply NoInputAndNoOutput @httpResponseTests([
   {
       id: "NoInputAndNoOutput",
       description: "No output serializes no payload",
       protocol: "aws.rest-xml",
       code: 200,
       body: ""
   }
])

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has no input and the
/// output is empty. While this should be rare, code generators must support
/// this.
@http(uri: "/NoInputAndOutputOutput", method: "POST")
operation NoInputAndOutput() -> NoInputAndOutputOutput

apply NoInputAndOutput @httpRequestTests([
    {
        id: "NoInputAndOutput",
        description: "No input serializes no payload",
        protocol: "aws.rest-xml",
        method: "POST",
        uri: "/NoInputAndOutput",
        body: ""
    }
])

apply NoInputAndOutput @httpResponseTests([
    {
        id: "NoInputAndOutput",
        description: "Empty output serializes no payload",
        protocol: "aws.rest-xml",
        code: 200,
        body: ""
    }
])

structure NoInputAndOutputOutput {}

/// The example tests how requests and responses are serialized when there's
/// no request or response payload because the operation has an empty input
/// and empty output structure that reuses the same shape. While this should
/// be rare, code generators must support this.
@http(uri: "/EmptyInputAndEmptyOutput", method: "POST")
operation EmptyInputAndEmptyOutput(EmptyInputAndEmptyOutputInput) -> EmptyInputAndEmptyOutputOutput

apply EmptyInputAndEmptyOutput @httpRequestTests([
    {
        id: "EmptyInputAndEmptyOutput",
        description: "Empty input serializes no payload",
        protocol: "aws.rest-xml",
        method: "POST",
        uri: "/EmptyInputAndEmptyOutput",
        body: ""
    },
])

apply EmptyInputAndEmptyOutput @httpResponseTests([
    {
        id: "EmptyInputAndEmptyOutput",
        description: "Empty output serializes no payload",
        protocol: "aws.rest-xml",
        code: 200,
        body: ""
    },
])

structure EmptyInputAndEmptyOutputInput {}
structure EmptyInputAndEmptyOutputOutput {}

// ============================================================================
// Header tests
// ============================================================================

/// The example tests how requests and responses are serialized when there is
/// no input or output payload but there are HTTP header bindings.
@http(uri: "/InputAndOutputWithHeaders", method: "POST")
operation InputAndOutputWithHeaders(InputAndOutputWithHeadersIO) -> InputAndOutputWithHeadersIO

apply InputAndOutputWithHeaders @httpRequestTests([
    {
        id: "InputAndOutputWithHeaders",
        description: "Tests requests with header bindings",
        protocol: "aws.rest-xml",
        method: "POST",
        uri: "/InputAndOutputWithHeaders",
        headers: {
            "X-String": "Hello",
            "X-Byte": "1",
            "X-Short": "123",
            "X-Integer": "123",
            "X-Long": "123",
            "X-Float": "1.0",
            "X-Double": "1.0",
            "X-Boolean1": "true",
            "X-Boolean2": "false",
            "X-StringList": "a, b, c",
            "X-StringSet": "a, b, c",
            "X-HeaderIntegerList": "1, 2, 3",
            "X-HeaderBooleanList": "true, false, true",
            "X-HeaderTimestampList": "Mon, 16 Dec 2019 23:48:18 GMT, Mon, 16 Dec 2019 23:48:18 GMT"
        },
        body: "",
        params: {
            headerString: "Hello",
            headerByte: 1,
            headerShort: 123,
            headerInteger: 123,
            headerLong: 123,
            headerFloat: 1.0,
            headerDouble: 1.0,
            headerTrueBool: true,
            headerFalseBool: true,
            headerStringList: ["a", "b", "c"],
            headerStringSet: ["a", "b", "c"],
            headerIntegerList: [1, 2, 3],
            headerBooleanList: [true, false, true],
            headerTimestampList: [1576540098, 1576540098]
        }
    },
])

apply InputAndOutputWithHeaders @httpResponseTests([
    {
        id: "InputAndOutputWithHeaders",
        description: "Tests responses with header bindings",
        protocol: "aws.rest-xml",
        code: 200,
        headers: {
            "X-String": "Hello",
            "X-Byte": "1",
            "X-Short": "123",
            "X-Integer": "123",
            "X-Long": "123",
            "X-Float": "1.0",
            "X-Double": "1.0",
            "X-Boolean1": "true",
            "X-Boolean2": "false",
            "X-StringList": "a, b, c",
            "X-StringSet": "a, b, c",
            "X-HeaderIntegerList": "1, 2, 3",
            "X-HeaderBooleanList": "true, false, true",
            "X-HeaderTimestampList": "Mon, 16 Dec 2019 23:48:18 GMT, Mon, 16 Dec 2019 23:48:18 GMT"
        },
        body: "",
        params: {
            headerString: "Hello",
            headerByte: 1,
            headerShort: 123,
            headerInteger: 123,
            headerLong: 123,
            headerFloat: 1.0,
            headerDouble: 1.0,
            headerTrueBool: true,
            headerFalseBool: true,
            headerStringList: ["a", "b", "c"],
            headerStringSet: ["a", "b", "c"],
            headerIntegerList: [1, 2, 3],
            headerBooleanList: [true, false, true],
            headerTimestampList: [1576540098, 1576540098]
        }
    },
])

structure InputAndOutputWithHeadersIO {
    @httpHeader("X-String")
    headerString: String,

    @httpHeader("X-Byte")
    headerByte: Byte,

    @httpHeader("X-Short")
    headerShort: Short,

    @httpHeader("X-Integer")
    headerInteger: Integer,

    @httpHeader("X-Long")
    headerLong: Long,

    @httpHeader("X-Float")
    headerFloat: Float,

    @httpHeader("X-Double")
    headerDouble: Double,

    @httpHeader("X-Boolean1")
    headerTrueBool: Boolean,

    @httpHeader("X-Boolean2")
    headerFalseBool: Boolean,

    @httpHeader("X-StringList")
    headerStringList: StringList,

    @httpHeader("X-StringSet")
    headerStringSet: StringSet,

    @httpHeader("X-IntegerList")
    headerIntegerList: IntegerList,

    @httpHeader("X-BooleanList")
    headerBooleanList: BooleanList,

    @httpHeader("X-TimestampList")
    headerTimestampList: TimestampList,
}

list StringList {
    member: String,
}

set StringSet {
    member: String,
}

list IntegerList {
    member: Integer,
}

list BooleanList {
    member: PrimitiveBoolean,
}

list TimestampList {
    member: Timestamp,
}

/// Null and empty headers are not sent over the wire.
@http(uri: "/NullAndEmptyHeaders", method: "GET")
operation NullAndEmptyHeaders(NullAndEmptyHeadersIO) -> NullAndEmptyHeadersIO

apply NullAndEmptyHeaders @httpRequestTests([
    {
        id: "NullAndEmptyHeaders",
        description: "Do not send null values, empty strings, or empty lists over the wire in headers",
        protocol: "aws.rest-xml",
        method: "GET",
        uri: "/NullAndEmptyHeaders",
        forbidHeaders: ["X-A", "X-B", "X-C"],
        body: "",
        params: {
            a: null,
            b: "",
            c: [],
        }
    },
])

apply NullAndEmptyHeaders @httpResponseTests([
    {
        id: "NullAndEmptyHeaders",
        description: "Do not send null or empty headers",
        protocol: "aws.rest-xml",
        code: 200,
        forbidHeaders: ["X-A", "X-B", "X-C"],
        body: "",
        params: {
            a: null,
            b: "",
            c: [],
        }
    },
])

structure NullAndEmptyHeadersIO {
    @httpHeader("X-A")
    a: String,

    @httpHeader("X-B")
    b: String,

    @httpHeader("X-C")
    c: StringList,
}

/// The example tests how timestamp request and response headers are serialized.
@http(uri: "/TimestampFormatHeaders", method: "POST")
operation TimestampFormatHeaders(TimestampFormatHeadersIO) -> TimestampFormatHeadersIO

apply TimestampFormatHeaders @httpRequestTests([
    {
        id: "TimestampFormatHeaders",
        description: "Tests how timestamp request headers are serialized",
        protocol: "aws.rest-xml",
        method: "POST",
        uri: "/TimestampFormatHeaders",
        headers: {
            "X-memberEpochSeconds": "1576540098",
            "X-memberHttpDate": "Mon, 16 Dec 2019 23:48:18 GMT",
            "X-memberDateTime": "2019-12-16T23:48:18Z",
            "X-defaultFormat": "Mon, 16 Dec 2019 23:48:18 GMT",
            "X-targetEpochSeconds": "1576540098",
            "X-targetHttpDate": "Mon, 16 Dec 2019 23:48:18 GMT",
            "X-targetDateTime": "2019-12-16T23:48:18Z",
        },
        body: "",
        params: {
            memberEpochSeconds: 1576540098,
            memberHttpDate: 1576540098,
            memberDateTime: 1576540098,
            defaultFormat: 1576540098,
            targetEpochSeconds: 1576540098,
            targetHttpDate: 1576540098,
            targetDateTime: 1576540098,
        }
    },
])

apply TimestampFormatHeaders @httpResponseTests([
    {
        id: "TimestampFormatHeaders",
        description: "Tests how timestamp response headers are serialized",
        protocol: "aws.rest-xml",
        code: 200,
        headers: {
            "X-memberEpochSeconds": "1576540098",
            "X-memberHttpDate": "Mon, 16 Dec 2019 23:48:18 GMT",
            "X-memberDateTime": "2019-12-16T23:48:18Z",
            "X-defaultFormat": "Mon, 16 Dec 2019 23:48:18 GMT",
            "X-targetEpochSeconds": "1576540098",
            "X-targetHttpDate": "Mon, 16 Dec 2019 23:48:18 GMT",
            "X-targetDateTime": "2019-12-16T23:48:18Z",
        },
        body: "",
        params: {
            memberEpochSeconds: 1576540098,
            memberHttpDate: 1576540098,
            memberDateTime: 1576540098,
            defaultFormat: 1576540098,
            targetEpochSeconds: 1576540098,
            targetHttpDate: 1576540098,
            targetDateTime: 1576540098,
        }
    },
])

structure TimestampFormatHeadersIO {
    @httpHeader("X-memberEpochSeconds")
    @timestampFormat("epoch-seconds")
    memberEpochSeconds: Timestamp,

    @httpHeader("X-memberHttpDate")
    @timestampFormat("http-date")
    memberHttpDate: Timestamp,

    @httpHeader("X-memberDateTime")
    @timestampFormat("date-time")
    memberDateTime: Timestamp,

    @httpHeader("X-defaultFormat")
    defaultFormat: Timestamp,

    @httpHeader("X-targetEpochSeconds")
    targetEpochSeconds: EpochSeconds,

    @httpHeader("X-targetHttpDate")
    targetHttpDate: HttpDate,

    @httpHeader("X-targetDateTime")
    targetDateTime: HttpDate,
}

// ============================================================================
// Label tests
// ============================================================================

/// The example tests how requests are serialized when there's no input
/// payload but there are HTTP labels.
@http(method: "GET", uri: "/HttpRequestWithLabels/{string}/{short}/{integer}/{long}/{float}/{double}/{boolean}/{timestamp}")
operation HttpRequestWithLabels(HttpRequestWithLabelsInput)

apply HttpRequestWithLabels @httpRequestTests([
    {
        id: "InputWithHeadersAndAllParams",
        description: "Sends a GET request that uses URI label bindings",
        protocol: "aws.rest-xml",
        method: "GET",
        uri: "/HttpRequestWithLabels/string/1/2/3/4.0/5.0/true/2019-12-16T23%3A48%3A18Z",
        body: "",
        params: {
            string: "string",
            short: 1,
            integer: 2,
            long: 3,
            float: 4.0,
            double: 5.0,
            boolean: true,
            timestamp: 1576540098
        }
    },
])

structure HttpRequestWithLabelsInput {
    @httpLabel
    @required
    string: String,

    @httpLabel
    @required
    short: Short,

    @httpLabel
    @required
    integer: Integer,

    @httpLabel
    @required
    long: Long,

    @httpLabel
    @required
    float: Float,

    @httpLabel
    @required
    double: Double,

    /// Serialized in the path as true or false.
    @httpLabel
    @required
    boolean: Boolean,

    /// Note that this member has no format, so it's serialized as an RFC 3399 date-time.
    @httpLabel
    @required
    timestamp: Timestamp,
}

/// The example tests how requests serialize different timestamp formats in the
/// URI path.
@http(method: "GET", uri: "/HttpRequestWithLabelsAndTimestampFormat/{memberEpochSeconds}/{memberHttpDate}/{memberDateTime}/{defaultFormat}/{targetEpochSeconds}/{targetHttpDate}/{targetDateTime}")
operation HttpRequestWithLabelsAndTimestampFormat(HttpRequestWithLabelsAndTimestampFormatInput)

apply HttpRequestWithLabelsAndTimestampFormat @httpRequestTests([
    {
        id: "HttpRequestWithLabelsAndTimestampFormat",
        description: "Serializes different timestamp formats in URI labels",
        protocol: "aws.rest-xml",
        method: "GET",
        uri: """
             /HttpRequestWithLabelsAndTimestampFormat\
             /1576540098\
             /Mon%2C+16+Dec+2019+23%3A48%3A18+GMT\
             /2019-12-16T23%3A48%3A18Z\
             /2019-12-16T23%3A48%3A18Z\
             /1576540098\
             /Mon%2C+16+Dec+2019+23%3A48%3A18+GMT\
             /2019-12-16T23%3A48%3A18Z""",
        body: "",
        params: {
            memberEpochSeconds: 1576540098,
            memberHttpDate: 1576540098,
            memberDateTime: 1576540098,
            defaultFormat: 1576540098,
            targetEpochSeconds: 1576540098,
            targetHttpDate: 1576540098,
            targetDateTime: 1576540098,
        }
    },
])

structure HttpRequestWithLabelsAndTimestampFormatInput {
    @httpLabel
    @required
    @timestampFormat("epoch-seconds")
    memberEpochSeconds: Timestamp,

    @httpLabel
    @required
    @timestampFormat("http-date")
    memberHttpDate: Timestamp,

    @httpLabel
    @required
    @timestampFormat("date-time")
    memberDateTime: Timestamp,

    @httpLabel
    @required
    defaultFormat: Timestamp,

    @httpLabel
    @required
    targetEpochSeconds: EpochSeconds,

    @httpLabel
    @required
    targetHttpDate: HttpDate,

    @httpLabel
    @required
    targetDateTime: HttpDate,
}

@timestampFormat("date-time")
timestamp DateTime

@timestampFormat("epoch-seconds")
timestamp EpochSeconds

@timestampFormat("http-date")
timestamp HttpDate

// ============================================================================
// Query string tests
// ============================================================================

/// This example uses simple query string parameters.
@http(uri: "/SimpleQueryParams", method: "GET")
operation SimpleQueryParams(SimpleQueryParamsInput)

apply SimpleQueryParams @httpRequestTests([
    {
        id: "SimpleQueryParams",
        description: """
                Uses two simple query string parameters. Note also that the query string parameter \
                name differs from the bound member name.""",
        protocol: "aws.rest-xml",
        method: "GET",
        uri: "/SimpleQueryParams",
        queryParams: {
            FOO: "bar",
            baz: "bam",
        },
        body: "",
        params: {
            foo: "bar",
            baz: "bam"
        }
    },
])

structure SimpleQueryParamsInput {
    @httpQuery("FOO")
    @required
    foo: String,

    @httpQuery("baz")
    @required
    baz: String,
}

// TODO: This example supports all query string types.
// ...

/// This example uses a constant query string parameters and a label.
@http(uri: "/ConstantQueryString/{hello}?foo=bar&baz", method: "GET")
@httpRequestTests([
    {
        id: "ConstantQueryString",
        description: "Includes constant query string parameters",
        protocol: "aws.rest-xml",
        method: "GET",
        uri: "/ConstantQueryString/hi",
        queryParams: {
            foo: "bar",
            baz: "",
        },
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
@http(uri: "/ConstantAndVariableQueryString?foo=bar", method: "GET")
operation ConstantAndVariableQueryString(ConstantAndVariableQueryStringInput)

apply ConstantAndVariableQueryString @httpRequestTests([
    {
        id: "ConstantAndVariableQueryStringMissingOneValue",
        description: "Mixes constant and variable query string parameters",
        protocol: "aws.rest-xml",
        method: "GET",
        uri: "/ConstantAndVariableQueryString",
        queryParams: {
            foo: "bar",
            baz: "bam",
        },
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
        queryParams: {
            foo: "bar",
            baz: "bam",
            maybeSet: "yes"
        },
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

// ============================================================================
// HTTP Prefix headers tests
// ============================================================================

// TODO

// ============================================================================
// HTTP payload tests
// ============================================================================

// TODO

// ============================================================================
// HTTP error tests
// ============================================================================

// TODO

// ============================================================================
// XML payload tests
// ============================================================================

// TODO
