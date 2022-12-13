$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedTimestampPathDefault/{timestamp}", method: "POST")
operation MalformedTimestampPathDefault {
    input: MalformedTimestampPathDefaultInput
}

@suppress(["UnstableTrait"])
@http(uri: "/MalformedTimestampPathHttpDate/{timestamp}", method: "POST")
operation MalformedTimestampPathHttpDate {
    input: MalformedTimestampPathHttpDateInput
}

@suppress(["UnstableTrait"])
@http(uri: "/MalformedTimestampPathEpoch/{timestamp}", method: "POST")
operation MalformedTimestampPathEpoch {
    input: MalformedTimestampPathEpochInput
}

apply MalformedTimestampPathDefault @httpMalformedRequestTests([
    {
        id: "RestJsonPathTimestampDefaultRejectsHttpDate",
        documentation: """
        By default, IMF-fixdate timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampPathDefault/$value:L"
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["Tue%2C%2029%20Apr%202014%2018%3A30%3A38%20GMT",
                       "Sun%2C%2002%20Jan%202000%2020%3A34%3A56.000%20GMT"]
        },
        tags : ["timestamp"]
    },
    {
        id: "RestJsonPathTimestampDefaultRejectsEpochSeconds",
        documentation: """
        By default, epoch second timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampPathDefault/$value:L"
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["1515531081.1234", "1515531081"]
        },
        tags : ["timestamp"]
    },
    {
        id: "RestJsonPathTimestampDefaultRejectsUTCOffsets",
        documentation: """
        UTC offsets must be rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampPathDefault/1996-12-19T16%3A39%3A57-08%3A00"
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        tags : ["timestamp"]
    },
    {
        id: "RestJsonPathTimestampDefaultRejectsDifferent8601Formats",
        documentation: """
        By default, maybe-valid ISO-8601 date-times not conforming to RFC 3339
        are rejected with a 400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampPathDefault/$value:L"
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["1996-12-19T16%3A39%3A57%2B00",
                       "1996-12-19T16%3A39%3A57%2B00Z",
                       "1996-12-19T16%3A39%3A57",
                       "1996-12-19T163957",
                       "19961219T163957Z",
                       "19961219T163957",
                       "19961219T16%3A39%3A57Z",
                       "19961219T16%3A39%3A57",
                       "1996-12-19T16%3A39Z",
                       "1996-12-19T16%3A39",
                       "1996-12-19T1639",
                       "1996-12-19T16Z",
                       "1996-12-19T16",
                       "1996-12-19%2016%3A39%3A57Z",
                       "2011-12-03T10%3A15%3A30%2B01%3A00%5BEurope%2FParis%5D"]
        },
        tags : ["timestamp"]
    },
])

apply MalformedTimestampPathHttpDate @httpMalformedRequestTests([
    {
        id: "RestJsonPathTimestampHttpDateRejectsDateTime",
        documentation: """
        When the format is http-date, RFC3339 timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampPathHttpDate/$value:L"
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["1985-04-12T23%3A20%3A50.52Z",
                       "1985-04-12T23%3A20%3A50Z",
                       "1996-12-19T16%3A39%3A57-08%3A00"]
        },
        tags : ["timestamp"]
    },
    {
        id: "RestJsonPathTimestampHttpDateRejectsEpochSeconds",
        documentation: """
        When the format is http-date,  epoch second timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampPathHttpDate/$value:L"
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["1515531081.1234", "1515531081"]
        },
        tags : ["timestamp"]
    },
])

apply MalformedTimestampPathEpoch @httpMalformedRequestTests([
    {
        id: "RestJsonPathTimestampEpochRejectsDateTime",
        documentation: """
        When the format is epoch-seconds, RFC3339 timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampPathEpoch/$value:L"
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["1985-04-12T23%3A20%3A50.52Z",
                       "1985-04-12T23%3A20%3A50Z",
                       "1996-12-19T16%3A39%3A57-08%3A00"]
        },
        tags : ["timestamp"]
    },
    {
        id: "RestJsonPathTimestampEpochRejectsHttpDate",
        documentation: """
        When the format is epoch-seconds, IMF-fixdate timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampPathEpoch/$value:L"
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["Tue%2C%2029%20Apr%202014%2018%3A30%3A38%20GMT",
                       "Sun%2C%2002%20Jan%202000%2020%3A34%3A56.000%20GMT"]
        },
        tags : ["timestamp"]
    },
    {
        id: "RestJsonPathTimestampEpochRejectsMalformedValues",
        documentation: """
        Invalid values for epoch seconds are rejected with a 400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampPathEpoch/$value:L"
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["true", "1515531081ABC", "0x42", "1515531081.123.456",
                       "Infinity", "-Infinity", "NaN"]
        },
        tags : ["timestamp"]
    },
])

structure MalformedTimestampPathDefaultInput {
    @httpLabel
    @required
    timestamp: Timestamp,
}

structure MalformedTimestampPathHttpDateInput {
    @httpLabel
    @required
    @timestampFormat("http-date")
    timestamp: Timestamp,
}

structure MalformedTimestampPathEpochInput {
    @httpLabel
    @required
    @timestampFormat("epoch-seconds")
    timestamp: Timestamp,
}

