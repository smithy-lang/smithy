// This file defines test cases that test HTTP URI label bindings.
// See: https://smithy.io/2.0/spec/http-bindings.html#httplabel-trait

$version: "2.0"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use aws.protocoltests.shared#DateTime
use aws.protocoltests.shared#EpochSeconds
use aws.protocoltests.shared#HttpDate
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// The example tests how requests are serialized when there's no input
/// payload but there are HTTP labels.
@readonly
@http(method: "GET", uri: "/HttpRequestWithLabels/{string}/{short}/{integer}/{long}/{float}/{double}/{boolean}/{timestamp}")
operation HttpRequestWithLabels {
    input: HttpRequestWithLabelsInput
}

apply HttpRequestWithLabels @httpRequestTests([
    {
        id: "InputWithHeadersAndAllParams",
        documentation: "Sends a GET request that uses URI label bindings",
        protocol: restXml,
        method: "GET",
        uri: "/HttpRequestWithLabels/string/1/2/3/4.1/5.1/true/2019-12-16T23%3A48%3A18Z",
        body: "",
        params: {
            string: "string",
            short: 1,
            integer: 2,
            long: 3,
            float: 4.1,
            double: 5.1,
            boolean: true,
            timestamp: 1576540098
        }
    },
    {
        id: "HttpRequestLabelEscaping",
        documentation: "Sends a GET request that uses URI label bindings",
        protocol: restXml,
        method: "GET",
        uri: "/HttpRequestWithLabels/%20%25%3A%2F%3F%23%5B%5D%40%21%24%26%27%28%29%2A%2B%2C%3B%3D%F0%9F%98%B9/1/2/3/4.1/5.1/true/2019-12-16T23%3A48%3A18Z",
        body: "",
        params: {
            string: " %:/?#[]@!$&'()*+,;=😹",
            short: 1,
            integer: 2,
            long: 3,
            float: 4.1,
            double: 5.1,
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
@readonly
@http(method: "GET", uri: "/HttpRequestWithLabelsAndTimestampFormat/{memberEpochSeconds}/{memberHttpDate}/{memberDateTime}/{defaultFormat}/{targetEpochSeconds}/{targetHttpDate}/{targetDateTime}")
operation HttpRequestWithLabelsAndTimestampFormat {
    input: HttpRequestWithLabelsAndTimestampFormatInput
}

apply HttpRequestWithLabelsAndTimestampFormat @httpRequestTests([
    {
        id: "HttpRequestWithLabelsAndTimestampFormat",
        documentation: "Serializes different timestamp formats in URI labels",
        protocol: restXml,
        method: "GET",
        uri: """
             /HttpRequestWithLabelsAndTimestampFormat\
             /1576540098\
             /Mon%2C%2016%20Dec%202019%2023%3A48%3A18%20GMT\
             /2019-12-16T23%3A48%3A18Z\
             /2019-12-16T23%3A48%3A18Z\
             /1576540098\
             /Mon%2C%2016%20Dec%202019%2023%3A48%3A18%20GMT\
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
    targetDateTime: DateTime,
}

// This example uses a greedy label and a normal label.
@readonly
@http(method: "GET", uri: "/HttpRequestWithGreedyLabelInPath/foo/{foo}/baz/{baz+}")
operation HttpRequestWithGreedyLabelInPath {
    input: HttpRequestWithGreedyLabelInPathInput
}

apply HttpRequestWithGreedyLabelInPath @httpRequestTests([
    {
        id: "HttpRequestWithGreedyLabelInPath",
        documentation: "Serializes greedy labels and normal labels",
        protocol: restXml,
        method: "GET",
        uri: "/HttpRequestWithGreedyLabelInPath/foo/hello/baz/there/guy",
        body: "",
        params: {
            foo: "hello",
            baz: "there/guy",
        }
    },
])

structure HttpRequestWithGreedyLabelInPathInput {
    @httpLabel
    @required
    foo: String,

    @httpLabel
    @required
    baz: String,
}

apply HttpRequestWithFloatLabels @httpRequestTests([
    {
        id: "RestXmlSupportsNaNFloatLabels",
        documentation: "Supports handling NaN float label values.",
        protocol: restXml,
        method: "GET",
        uri: "/FloatHttpLabels/NaN/NaN",
        body: "",
        params: {
            float: "NaN",
            double: "NaN",
        }
    },
    {
        id: "RestXmlSupportsInfinityFloatLabels",
        documentation: "Supports handling Infinity float label values.",
        protocol: restXml,
        method: "GET",
        uri: "/FloatHttpLabels/Infinity/Infinity",
        body: "",
        params: {
            float: "Infinity",
            double: "Infinity",
        }
    },
    {
        id: "RestXmlSupportsNegativeInfinityFloatLabels",
        documentation: "Supports handling -Infinity float label values.",
        protocol: restXml,
        method: "GET",
        uri: "/FloatHttpLabels/-Infinity/-Infinity",
        body: "",
        params: {
            float: "-Infinity",
            double: "-Infinity",
        }
    },
])

@readonly
@http(method: "GET", uri: "/FloatHttpLabels/{float}/{double}")
operation HttpRequestWithFloatLabels {
    input: HttpRequestWithFloatLabelsInput
}

structure HttpRequestWithFloatLabelsInput {
    @httpLabel
    @required
    float: Float,
    
    @httpLabel
    @required
    double: Double,
}
