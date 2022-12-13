$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedTimestampQueryDefault", method: "POST")
operation MalformedTimestampQueryDefault {
    input: MalformedTimestampQueryDefaultInput
}

@suppress(["UnstableTrait"])
@http(uri: "/MalformedTimestampQueryHttpDate", method: "POST")
operation MalformedTimestampQueryHttpDate {
    input: MalformedTimestampQueryHttpDateInput
}

@suppress(["UnstableTrait"])
@http(uri: "/MalformedTimestampQueryEpoch", method: "POST")
operation MalformedTimestampQueryEpoch {
    input: MalformedTimestampQueryEpochInput
}

apply MalformedTimestampQueryDefault @httpMalformedRequestTests([
    {
        id: "RestJsonQueryTimestampDefaultRejectsHttpDate",
        documentation: """
        By default, IMF-fixdate timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampQueryDefault",
            queryParams: [
                "timestamp=$value:L"
            ]
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
        id: "RestJsonQueryTimestampDefaultRejectsEpochSeconds",
        documentation: """
        By default, epoch second timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampQueryDefault",
            queryParams: [
                "timestamp=$value:L"
            ]
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
        id: "RestJsonQueryTimestampDefaultRejectsUTCOffsets",
        documentation: """
        UTC offsets must be rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampQueryDefault",
            queryParams: [
                "timestamp=$value:L"
            ]
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
        id: "RestJsonQueryTimestampDefaultRejectsDifferent8601Formats",
        documentation: """
        By default, maybe-valid ISO-8601 date-times not conforming to RFC 3339
        are rejected with a 400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampQueryDefault",
            queryParams: [
                "timestamp=$value:L"
            ]
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["1996-12-19T16:39:57+00",
                       "1996-12-19T16:39:57+00Z",
                       "1996-12-19T16:39:57",
                       "1996-12-19T163957",
                       "19961219T163957Z",
                       "19961219T163957",
                       "19961219T16:39:57Z",
                       "19961219T16:39:57",
                       "1996-12-19T16:39Z",
                       "1996-12-19T16:39",
                       "1996-12-19T1639",
                       "1996-12-19T16Z",
                       "1996-12-19T16",
                       "1996-12-19%2016:39:57Z",
                       "2011-12-03T10:15:30+01:00[Europe/Paris]"]
        },
        tags : ["timestamp"]
    },
])

apply MalformedTimestampQueryHttpDate @httpMalformedRequestTests([
    {
        id: "RestJsonQueryTimestampHttpDateRejectsDateTime",
        documentation: """
        When the format is http-date, RFC3339 timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampQueryHttpDate",
            queryParams: [
                "timestamp=$value:L"
            ]
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
        id: "RestJsonQueryTimestampHttpDateRejectsEpochSeconds",
        documentation: """
        When the format is http-date, epoch second timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampQueryHttpDate",
            queryParams: [
                "timestamp=$value:L"
            ]
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

apply MalformedTimestampQueryEpoch @httpMalformedRequestTests([
    {
        id: "RestJsonQueryTimestampEpochRejectsDateTime",
        documentation: """
        When the format is epoch-seconds, RFC3339 timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampQueryEpoch",
            queryParams: [
                "timestamp=$value:L"
            ]
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
        id: "RestJsonQueryTimestampEpochRejectsHttpDate",
        documentation: """
        When the format is epoch-seconds, IMF-fixdate timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampQueryEpoch",
            queryParams: [
                "timestamp=$value:L"
            ]
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
        id: "RestJsonQueryTimestampEpochRejectsMalformedValues",
        documentation: """
        Invalid values for epoch seconds are rejected with a 400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampQueryEpoch",
            queryParams: [
                "timestamp=$value:L"
            ]
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

structure MalformedTimestampQueryDefaultInput {
    @httpQuery("timestamp")
    @required
    timestamp: Timestamp,
}

structure MalformedTimestampQueryHttpDateInput {
    @httpQuery("timestamp")
    @required
    @timestampFormat("http-date")
    timestamp: Timestamp,
}

structure MalformedTimestampQueryEpochInput {
    @httpQuery("timestamp")
    @required
    @timestampFormat("epoch-seconds")
    timestamp: Timestamp,
}

