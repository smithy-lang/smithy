$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedTimestampBodyDefault", method: "POST")
operation MalformedTimestampBodyDefault {
    input: MalformedTimestampBodyDefaultInput
}

@suppress(["UnstableTrait"])
@http(uri: "/MalformedTimestampBodyDateTime", method: "POST")
operation MalformedTimestampBodyDateTime {
    input: MalformedTimestampBodyDateTimeInput
}

@suppress(["UnstableTrait"])
@http(uri: "/MalformedTimestampBodyHttpDate", method: "POST")
operation MalformedTimestampBodyHttpDate {
    input: MalformedTimestampBodyHttpDateInput
}

apply MalformedTimestampBodyDefault @httpMalformedRequestTests([
    {
        id: "RestJsonBodyTimestampDefaultRejectsDateTime",
        documentation: """
        By default, RFC3339 timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampBodyDefault",
            body: """
                  { "timestamp": $value:S }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["1985-04-12T23:20:50.52Z",
                       "1985-04-12T23:20:50Z",
                       "1996-12-19T16:39:57-08:00"]
        },
        tags : ["timestamp"]
    },
    {
        id: "RestJsonBodyTimestampDefaultRejectsStringifiedEpochSeconds",
        documentation: """
        By default, epoch second timestamps as strings are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampBodyDefault",
            body: """
                  { "timestamp": $value:S }""",
            headers: {
                "content-type": "application/json"
            }
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
        id: "RestJsonBodyTimestampDefaultRejectsMalformedEpochSeconds",
        documentation: """
        Invalid values for epoch seconds are rejected with a 400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampBodyDefault",
            body: """
                  { "timestamp": $value:L }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["true", "1515531081ABC", "0x42", "1515531081.123.456",
                       "Infinity", "\"Infinity\"", "-Infinity", "\"-Infinity\"", "NaN", "\"NaN\""]
        },
        tags : ["timestamp"]
    },
    {
        id: "RestJsonBodyTimestampDefaultRejectsHttpDate",
        documentation: """
        By default, IMF-fixdate timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampBodyDefault",
            body: """
                  { "timestamp": $value:S }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["Tue, 29 Apr 2014 18:30:38 GMT"]
        },
        tags : ["timestamp"]
    },
])

apply MalformedTimestampBodyDateTime @httpMalformedRequestTests([
    {
        id: "RestJsonBodyTimestampDateTimeRejectsHttpDate",
        documentation: """
        When the format is date-time, IMF-fixdate timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampBodyDateTime",
            body: """
                  { "timestamp": $value:S }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["Tue, 29 Apr 2014 18:30:38 GMT"]
        },
        tags : ["timestamp"]
    },
    {
        id: "RestJsonBodyTimestampDateTimeRejectsEpochSeconds",
        documentation: """
        When the format is date-time, epoch-seconds timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampBodyDateTime",
            body: """
                  { "timestamp": $value:L }""",
            headers: {
                "content-type": "application/json"
            }
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
        id: "RestJsonBodyTimestampDateTimeRejectsUTCOffsets",
        documentation: """
        When the format is date-time, RFC 3339 timestamps with a UTC offset are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampBodyDateTime",
            body: """
                  { "timestamp": $value:S }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["1996-12-19T16:39:57-08:00"]
        },
        tags : ["timestamp"]
    },
    {
        id: "RestJsonBodyTimestampDateTimeRejectsDifferent8601Formats",
        documentation: """
        When the format is date-time, maybe-valid ISO-8601 date-times not conforming to RFC 3339
        are rejected with a 400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampBodyDateTime",
            body: """
                  { "timestamp": $value:S }""",
            headers: {
                "content-type": "application/json"
            }
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
                       "1996-12-19 16:39:57Z",
                       "2011-12-03T10:15:30+01:00[Europe/Paris]"]
        },
        tags : ["timestamp"]
    },
])

apply MalformedTimestampBodyHttpDate @httpMalformedRequestTests([
    {
        id: "RestJsonBodyTimestampHttpDateRejectsDateTime",
        documentation: """
        When the format is http-date, RFC3339 timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampBodyHttpDate",
            body: """
                  { "timestamp": $value:S }""",
            headers: {
                "content-type": "application/json"
            }
        },
        response: {
            code: 400,
            headers: {
                "x-amzn-errortype": "SerializationException"
            }
        },
        testParameters: {
            "value" : ["1985-04-12T23:20:50.52Z",
                       "1985-04-12T23:20:50Z",
                       "1996-12-19T16:39:57-08:00"]
        },
        tags : ["timestamp"]
    },
    {
        id: "RestJsonBodyTimestampHttpDateRejectsEpoch",
        documentation: """
        When the format is http-date, epoch-seconds timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampBodyHttpDate",
            body: """
                  { "timestamp": $value:L }""",
            headers: {
                "content-type": "application/json"
            }
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

structure MalformedTimestampBodyDefaultInput {
    @required
    timestamp: Timestamp,
}

structure MalformedTimestampBodyDateTimeInput {
    @required
    @timestampFormat("date-time")
    timestamp: Timestamp,
}

structure MalformedTimestampBodyHttpDateInput {
    @required
    @timestampFormat("http-date")
    timestamp: Timestamp,
}
