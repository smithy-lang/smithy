// This file defines test cases that test HTTP header bindings.
// See: https://awslabs.github.io/smithy/spec/http.html#httpheader-trait

$version: "0.5.0"

namespace aws.protocols.tests.restxml

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// The example tests how requests and responses are serialized when there is
/// no input or output payload but there are HTTP header bindings.
@http(uri: "/InputAndOutputWithHeaders", method: "POST")
operation InputAndOutputWithHeaders(InputAndOutputWithHeadersIO) -> InputAndOutputWithHeadersIO

apply InputAndOutputWithHeaders @httpRequestTests([
    {
        id: "InputAndOutputWithStringHeaders",
        description: "Tests requests with string header bindings",
        protocol: "aws.rest-xml",
        method: "POST",
        uri: "/InputAndOutputWithHeaders",
        headers: {
            "X-String": "Hello",
            "X-StringList": "a, b, c",
            "X-StringSet": "a, b, c"
        },
        body: "",
        params: {
            headerString: "Hello",
            headerStringList: ["a", "b", "c"],
            headerStringSet: ["a", "b", "c"],
        }
    },
    {
        id: "InputAndOutputWithNumericHeaders",
        description: "Tests requests with numeric header bindings",
        protocol: "aws.rest-xml",
        method: "POST",
        uri: "/InputAndOutputWithHeaders",
        headers: {
            "X-Byte": "1",
            "X-Short": "123",
            "X-Integer": "123",
            "X-Long": "123",
            "X-Float": "1.0",
            "X-Double": "1.0",
            "X-HeaderIntegerList": "1, 2, 3",
        },
        body: "",
        params: {
            headerByte: 1,
            headerShort: 123,
            headerInteger: 123,
            headerLong: 123,
            headerFloat: 1.0,
            headerDouble: 1.0,
            headerIntegerList: [1, 2, 3],
        }
    },
    {
        id: "InputAndOutputWithBooleanHeaders",
        description: "Tests requests with boolean header bindings",
        protocol: "aws.rest-xml",
        method: "POST",
        uri: "/InputAndOutputWithHeaders",
        headers: {
            "X-Boolean1": "true",
            "X-Boolean2": "false",
            "X-HeaderBooleanList": "true, false, true"
        },
        body: "",
        params: {
            headerTrueBool: true,
            headerFalseBool: true,
            headerBooleanList: [true, false, true]
        }
    },
    {
        id: "InputAndOutputWithTimestampHeaders",
        description: "Tests requests with timestamp header bindings",
        protocol: "aws.rest-xml",
        method: "POST",
        uri: "/InputAndOutputWithHeaders",
        headers: {
            "X-HeaderTimestampList": "Mon, 16 Dec 2019 23:48:18 GMT, Mon, 16 Dec 2019 23:48:18 GMT"
        },
        body: "",
        params: {
            headerTimestampList: [1576540098, 1576540098]
        }
    },
    {
        id: "InputAndOutputWithEnumHeaders",
        description: "Tests requests with enum header bindings",
        protocol: "aws.rest-xml",
        method: "POST",
        uri: "/InputAndOutputWithHeaders",
        headers: {
            "X-Enum": "Foo",
            "X-EnumList": "Foo, Baz, Bar"
        },
        body: "",
        params: {
            headerEnum: "Foo",
            headerEnumList: ["Foo", "Bar", "Baz"],
        }
    },
])

apply InputAndOutputWithHeaders @httpResponseTests([
    {
        id: "InputAndOutputWithStringHeaders",
        description: "Tests responses with string header bindings",
        protocol: "aws.rest-xml",
        code: 200,
        headers: {
            "X-String": "Hello",
            "X-StringList": "a, b, c",
            "X-StringSet": "a, b, c"
        },
        body: "",
        params: {
            headerString: "Hello",
            headerStringList: ["a", "b", "c"],
            headerStringSet: ["a", "b", "c"],
        }
    },
    {
        id: "InputAndOutputWithNumericHeaders",
        description: "Tests responses with numeric header bindings",
        protocol: "aws.rest-xml",
        code: 200,
        headers: {
            "X-Byte": "1",
            "X-Short": "123",
            "X-Integer": "123",
            "X-Long": "123",
            "X-Float": "1.0",
            "X-Double": "1.0",
            "X-HeaderIntegerList": "1, 2, 3",
        },
        body: "",
        params: {
            headerByte: 1,
            headerShort: 123,
            headerInteger: 123,
            headerLong: 123,
            headerFloat: 1.0,
            headerDouble: 1.0,
            headerIntegerList: [1, 2, 3],
        }
    },
    {
        id: "InputAndOutputWithBooleanHeaders",
        description: "Tests responses with boolean header bindings",
        protocol: "aws.rest-xml",
        code: 200,
        headers: {
            "X-Boolean1": "true",
            "X-Boolean2": "false",
            "X-HeaderBooleanList": "true, false, true"
        },
        body: "",
        params: {
            headerTrueBool: true,
            headerFalseBool: true,
            headerBooleanList: [true, false, true]
        }
    },
    {
        id: "InputAndOutputWithTimestampHeaders",
        description: "Tests responses with timestamp header bindings",
        protocol: "aws.rest-xml",
        code: 200,
        headers: {
            "X-HeaderTimestampList": "Mon, 16 Dec 2019 23:48:18 GMT, Mon, 16 Dec 2019 23:48:18 GMT"
        },
        body: "",
        params: {
            headerTimestampList: [1576540098, 1576540098]
        }
    },
    {
        id: "InputAndOutputWithEnumHeaders",
        description: "Tests responses with enum header bindings",
        protocol: "aws.rest-xml",
        code: 200,
        headers: {
            "X-Enum": "Foo",
            "X-EnumList": "Foo, Baz, Bar"
        },
        body: "",
        params: {
            headerEnum: "Foo",
            headerEnumList: ["Foo", "Bar", "Baz"],
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

    @httpHeader("X-Enum")
    headerEnum: FooEnum,

    @httpHeader("X-EnumList")
    headerEnumList: FooEnumList,
}

/// Null and empty headers are not sent over the wire.
@readonly
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
