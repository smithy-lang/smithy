$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpMalformedRequestTests

@suppress(["UnstableTrait"])
@http(uri: "/MalformedTimestampHeaderDefault", method: "POST")
operation MalformedTimestampHeaderDefault {
    input: MalformedTimestampHeaderDefaultInput
}

@suppress(["UnstableTrait"])
@http(uri: "/MalformedTimestampHeaderDateTime", method: "POST")
operation MalformedTimestampHeaderDateTime {
    input: MalformedTimestampHeaderDateTimeInput
}

@suppress(["UnstableTrait"])
@http(uri: "/MalformedTimestampHeaderEpoch", method: "POST")
operation MalformedTimestampHeaderEpoch {
    input: MalformedTimestampHeaderEpochInput
}

apply MalformedTimestampHeaderDefault @httpMalformedRequestTests([
    {
        id: "RestJsonHeaderTimestampDefaultRejectsDateTime",
        documentation: """
        By default, RFC3339 timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampHeaderDefault",
            headers: {
                "timestamp": "$value:L"
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
        id: "RestJsonHeaderTimestampDefaultRejectsEpochSeconds",
        documentation: """
        By default, epoch second timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampHeaderDefault",
            headers: {
                "timestamp": "$value:L"
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

apply MalformedTimestampHeaderDateTime @httpMalformedRequestTests([
    {
        id: "RestJsonHeaderTimestampDateTimeRejectsHttpDate",
        documentation: """
        When the format is date-time, IMF-fixdate timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampHeaderDateTime",
            headers: {
                "timestamp": "$value:L"
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
        id: "RestJsonHeaderTimestampDateTimeRejectsEpochSeconds",
        documentation: """
        When the format is date-time, epoch-seconds timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampHeaderDateTime",
            headers: {
                "timestamp": "$value:L"
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
        id: "RestJsonHeaderTimestampDateTimeRejectsDifferent8601Formats",
        documentation: """
        When the format is date-time, maybe-valid ISO-8601 date-times not conforming to RFC 3339
        are rejected with a 400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampHeaderDateTime",
            headers: {
                "timestamp": "$value:L"
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

apply MalformedTimestampHeaderEpoch @httpMalformedRequestTests([
    {
        id: "RestJsonHeaderTimestampEpochRejectsDateTime",
        documentation: """
        When the format is epoch-seconds, RFC3339 timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampHeaderEpoch",
            headers: {
                "timestamp": "$value:L"
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
        id: "RestJsonHeaderTimestampEpochRejectsHttpDate",
        documentation: """
        When the format is epoch-seconds, IMF-fixdate timestamps are rejected with a
        400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampHeaderEpoch",
            headers: {
                "timestamp": "$value:L"
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
        id: "RestJsonHeaderTimestampEpochRejectsMalformedValues",
        documentation: """
        Invalid values for epoch seconds are rejected with a 400 SerializationException""",
        protocol: restJson1,
        request: {
            method: "POST",
            uri: "/MalformedTimestampHeaderEpoch",
            headers: {
                "timestamp": "$value:L"
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
                       "Infinity", "-Infinity", "NaN"]
        },
        tags : ["timestamp"]
    },
])

structure MalformedTimestampHeaderDefaultInput {
    @httpHeader("timestamp")
    @required
    timestamp: Timestamp,
}

structure MalformedTimestampHeaderDateTimeInput {
    @httpHeader("timestamp")
    @required
    @timestampFormat("date-time")
    timestamp: Timestamp,
}

structure MalformedTimestampHeaderEpochInput {
    @httpHeader("timestamp")
    @required
    @timestampFormat("epoch-seconds")
    timestamp: Timestamp,
}
