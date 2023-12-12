// This file defines test cases that serialize synthesized XML documents
// in the payload of HTTP requests and responses.

$version: "2.0"
$operationInputSuffix: "Request"
$operationOutputSuffix: "Response"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use aws.protocoltests.shared#DateTime
use aws.protocoltests.shared#EpochSeconds
use aws.protocoltests.shared#FooEnum
use aws.protocoltests.shared#FooEnumList
use aws.protocoltests.shared#FooEnumSet
use aws.protocoltests.shared#FooEnumMap
use aws.protocoltests.shared#IntegerEnum
use aws.protocoltests.shared#IntegerEnumList
use aws.protocoltests.shared#IntegerEnumSet
use aws.protocoltests.shared#IntegerEnumMap
use aws.protocoltests.shared#HttpDate
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

// This example serializes simple scalar types in the top level XML document.
// Note that headers are not serialized in the payload.
@idempotent
@http(uri: "/SimpleScalarProperties", method: "PUT")
operation SimpleScalarProperties {
    input := with [SimpleScalarPropertiesInputOutput] {}
    output := with [SimpleScalarPropertiesInputOutput] {}
}

apply SimpleScalarProperties @httpRequestTests([
    {
        id: "SimpleScalarProperties",
        documentation: "Serializes simple scalar properties",
        protocol: restXml,
        method: "PUT",
        uri: "/SimpleScalarProperties",
        body: """
              <SimpleScalarPropertiesRequest>
                  <stringValue>string</stringValue>
                  <trueBooleanValue>true</trueBooleanValue>
                  <falseBooleanValue>false</falseBooleanValue>
                  <byteValue>1</byteValue>
                  <shortValue>2</shortValue>
                  <integerValue>3</integerValue>
                  <longValue>4</longValue>
                  <floatValue>5.5</floatValue>
                  <DoubleDribble>6.5</DoubleDribble>
              </SimpleScalarPropertiesRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
            "X-Foo": "Foo",
        },
        params: {
            foo: "Foo",
            stringValue: "string",
            trueBooleanValue: true,
            falseBooleanValue: false,
            byteValue: 1,
            shortValue: 2,
            integerValue: 3,
            longValue: 4,
            floatValue: 5.5,
            doubleValue: 6.5,
        }
    },
    {
        id: "SimpleScalarPropertiesWithEscapedCharacter",
        documentation: "Serializes string with escaping",
        protocol: restXml,
        method: "PUT",
        uri: "/SimpleScalarProperties",
        body: """
              <SimpleScalarPropertiesRequest>
                  <stringValue>&lt;string&gt;</stringValue>
              </SimpleScalarPropertiesRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
            "X-Foo": "Foo",
        },
        params: {
            foo: "Foo",
            stringValue: "<string>",
        }
    },
    {
        id: "SimpleScalarPropertiesWithWhiteSpace",
        documentation: "Serializes string containing white space",
        protocol: restXml,
        method: "PUT",
        uri: "/SimpleScalarProperties",
        body: """
              <SimpleScalarPropertiesRequest>
                  <stringValue>  string with white    space  </stringValue>
              </SimpleScalarPropertiesRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
            "X-Foo": "Foo",
        },
        params: {
            foo: "Foo",
            stringValue: "  string with white    space  ",
        }
    },
    {
        id: "SimpleScalarPropertiesPureWhiteSpace",
        documentation: "Serializes string containing exclusively whitespace",
        protocol: restXml,
        method: "PUT",
        uri: "/SimpleScalarProperties",
        body: """
              <SimpleScalarPropertiesRequest>
                  <stringValue>   </stringValue>
              </SimpleScalarPropertiesRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
            "X-Foo": "Foo",
        },
        params: {
            foo: "Foo",
            stringValue: "   ",
        }
    },
    {
        id: "RestXmlSupportsNaNFloatInputs",
        documentation: "Supports handling NaN float values.",
        protocol: restXml,
        method: "PUT",
        uri: "/SimpleScalarProperties",
        body: """
              <SimpleScalarPropertiesRequest>
                  <floatValue>NaN</floatValue>
                  <DoubleDribble>NaN</DoubleDribble>
              </SimpleScalarPropertiesRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            floatValue: "NaN",
            doubleValue: "NaN",
        }
    },
    {
        id: "RestXmlSupportsInfinityFloatInputs",
        documentation: "Supports handling Infinity float values.",
        protocol: restXml,
        method: "PUT",
        uri: "/SimpleScalarProperties",
        body: """
              <SimpleScalarPropertiesRequest>
                  <floatValue>Infinity</floatValue>
                  <DoubleDribble>Infinity</DoubleDribble>
              </SimpleScalarPropertiesRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            floatValue: "Infinity",
            doubleValue: "Infinity",
        }
    },
    {
        id: "RestXmlSupportsNegativeInfinityFloatInputs",
        documentation: "Supports handling -Infinity float values.",
        protocol: restXml,
        method: "PUT",
        uri: "/SimpleScalarProperties",
        body: """
              <SimpleScalarPropertiesRequest>
                  <floatValue>-Infinity</floatValue>
                  <DoubleDribble>-Infinity</DoubleDribble>
              </SimpleScalarPropertiesRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            floatValue: "-Infinity",
            doubleValue: "-Infinity",
        }
    },
])

apply SimpleScalarProperties @httpResponseTests([
    {
        id: "SimpleScalarProperties",
        documentation: "Serializes simple scalar properties",
        protocol: restXml,
        code: 200,
        body: """
              <SimpleScalarPropertiesResponse>
                  <stringValue>string</stringValue>
                  <trueBooleanValue>true</trueBooleanValue>
                  <falseBooleanValue>false</falseBooleanValue>
                  <byteValue>1</byteValue>
                  <shortValue>2</shortValue>
                  <integerValue>3</integerValue>
                  <longValue>4</longValue>
                  <floatValue>5.5</floatValue>
                  <DoubleDribble>6.5</DoubleDribble>
              </SimpleScalarPropertiesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
            "X-Foo": "Foo",
        },
        params: {
            foo: "Foo",
            stringValue: "string",
            trueBooleanValue: true,
            falseBooleanValue: false,
            byteValue: 1,
            shortValue: 2,
            integerValue: 3,
            longValue: 4,
            floatValue: 5.5,
            doubleValue: 6.5,
        }
    },
    {
        id: "SimpleScalarPropertiesComplexEscapes",
        documentation: """
        Serializes string with escaping.

        This validates the three escape types: literal, decimal and hexadecimal. It also validates that unescaping properly
        handles the case where unescaping an & produces a newly formed escape sequence (this should not be re-unescaped).

        Servers may produce different output, this test is designed different unescapes clients must handle
        """,
        protocol: restXml,
        code: 200,
        body: """
              <SimpleScalarPropertiesResponse>
                  <stringValue>escaped data: &amp;lt;&#xD;&#10;</stringValue>
              </SimpleScalarPropertiesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
            "X-Foo": "Foo",
        },
        params: {
            foo: "Foo",
            stringValue: "escaped data: &lt;\r\n",
        },
        appliesTo: "client",
    },
    {
        id: "SimpleScalarPropertiesWithEscapedCharacter",
        documentation: "Serializes string with escaping",
        protocol: restXml,
        code: 200,
        body: """
              <SimpleScalarPropertiesResponse>
                  <stringValue>&lt;string&gt;</stringValue>
              </SimpleScalarPropertiesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
            "X-Foo": "Foo",
        },
        params: {
            foo: "Foo",
            stringValue: "<string>",
        }
    },
    {
        id: "SimpleScalarPropertiesWithXMLPreamble",
        documentation: "Serializes simple scalar properties with xml preamble, comments and CDATA",
        protocol: restXml,
        code: 200,
        body: """
              <?xml version = "1.0" encoding = "UTF-8"?>
              <SimpleScalarPropertiesResponse>
                  <![CDATA[characters representing CDATA]]>
                  <stringValue>string</stringValue>
                  <!--xml comment-->
              </SimpleScalarPropertiesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
            "X-Foo": "Foo",
        },
        params: {
            foo: "Foo",
            stringValue: "string",
        }
    },
    {
        id: "SimpleScalarPropertiesWithWhiteSpace",
        documentation: "Serializes string containing white space",
        protocol: restXml,
        code: 200,
        body: """
              <?xml version = "1.0" encoding = "UTF-8"?>
              <SimpleScalarPropertiesResponse>
                  <stringValue> string with white    space </stringValue>
              </SimpleScalarPropertiesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
            "X-Foo": "Foo",
        },
        params: {
            foo: "Foo",
            stringValue: " string with white    space ",
        }
    },
    {
        id: "SimpleScalarPropertiesPureWhiteSpace",
        documentation: "Serializes string containing white space",
        protocol: restXml,
        code: 200,
        body: """
              <?xml version = "1.0" encoding = "UTF-8"?>
              <SimpleScalarPropertiesResponse>
                  <stringValue>  </stringValue>
              </SimpleScalarPropertiesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
            "X-Foo": "Foo",
        },
        params: {
            foo: "Foo",
            stringValue: "  ",
        }
    },
    {
        id: "RestXmlSupportsNaNFloatOutputs",
        documentation: "Supports handling NaN float values.",
        protocol: restXml,
        code: 200,
        body: """
              <SimpleScalarPropertiesResponse>
                  <floatValue>NaN</floatValue>
                  <DoubleDribble>NaN</DoubleDribble>
              </SimpleScalarPropertiesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            floatValue: "NaN",
            doubleValue: "NaN",
        }
    },
    {
        id: "RestXmlSupportsInfinityFloatOutputs",
        documentation: "Supports handling Infinity float values.",
        protocol: restXml,
        code: 200,
        body: """
              <SimpleScalarPropertiesResponse>
                  <floatValue>Infinity</floatValue>
                  <DoubleDribble>Infinity</DoubleDribble>
              </SimpleScalarPropertiesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            floatValue: "Infinity",
            doubleValue: "Infinity",
        }
    },
    {
        id: "RestXmlSupportsNegativeInfinityFloatOutputs",
        documentation: "Supports handling -Infinity float values.",
        protocol: restXml,
        code: 200,
        body: """
              <SimpleScalarPropertiesResponse>
                  <floatValue>-Infinity</floatValue>
                  <DoubleDribble>-Infinity</DoubleDribble>
              </SimpleScalarPropertiesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            floatValue: "-Infinity",
            doubleValue: "-Infinity",
        }
    },
])

@mixin
structure SimpleScalarPropertiesInputOutput {
    @httpHeader("X-Foo")
    foo: String,

    stringValue: String,
    trueBooleanValue: Boolean,
    falseBooleanValue: Boolean,
    byteValue: Byte,
    shortValue: Short,
    integerValue: Integer,
    longValue: Long,
    floatValue: Float,

    @xmlName("DoubleDribble")
    doubleValue: Double,
}

// This example serializes empty string in the top level XML document.
@idempotent
@http(uri: "/XmlEmptyStrings", method: "PUT")
@tags(["client-only"])
operation XmlEmptyStrings {
    input := {
        emptyString: String
    }
    output := {
        emptyString: String
    }
}

apply XmlEmptyStrings @httpRequestTests([
       {
           id: "XmlEmptyStrings",
           documentation: "Serializes xml empty strings",
           protocol: restXml,
           method: "PUT",
           uri: "/XmlEmptyStrings",
           body: """
                 <XmlEmptyStringsRequest>
                     <emptyString></emptyString>
                 </XmlEmptyStringsRequest>
                 """,
           bodyMediaType: "application/xml",
           headers: {
               "Content-Type": "application/xml",
           },
           params: {
               emptyString: "",
           },
           appliesTo: "client",
       }
])

apply XmlEmptyStrings @httpResponseTests([
    {
        id: "XmlEmptyStrings",
        documentation: "Deserializes xml empty strings",
        protocol: restXml,
        code: 200,
        body: """
              <XmlEmptyStringsResponse>
                  <emptyString></emptyString>
              </XmlEmptyStringsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            emptyString: ""
        },
        appliesTo: "client",
    },
    {
        id: "XmlEmptySelfClosedStrings",
        documentation: "Empty self closed string are deserialized as empty string",
        protocol: restXml,
        code: 200,
        body: """
              <XmlEmptyStringsResponse>
                  <emptyString/>
              </XmlEmptyStringsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            emptyString: ""
        },
        appliesTo: "client",
    }
])

/// Blobs are base64 encoded
@http(uri: "/XmlBlobs", method: "POST")
operation XmlBlobs {
    input := {
        data: Blob
    }
    output := {
        data: Blob
    }
}

apply XmlBlobs @httpRequestTests([
    {
        id: "XmlBlobs",
        documentation: "Blobs are base64 encoded",
        protocol: restXml,
        method: "POST",
        uri: "/XmlBlobs",
        body: """
              <XmlBlobsRequest>
                  <data>dmFsdWU=</data>
              </XmlBlobsRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            data: "value"
        }
    }
])

apply XmlBlobs @httpResponseTests([
    {
        id: "XmlBlobs",
        documentation: "Blobs are base64 encoded",
        protocol: restXml,
        code: 200,
        body: """
              <XmlBlobsResponse>
                  <data>dmFsdWU=</data>
              </XmlBlobsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            data: "value"
        }
    }
])

/// Blobs are base64 encoded
@http(uri: "/XmlEmptyBlobs", method: "POST")
@tags(["client-only"])
operation XmlEmptyBlobs {
    input := {
        data: Blob
    }
    output := {
        data: Blob
    }
}

apply XmlEmptyBlobs @httpResponseTests([
    {
        id: "XmlEmptyBlobs",
        documentation: "Empty blobs are deserialized as empty string",
        protocol: restXml,
        code: 200,
        body: """
              <XmlEmptyBlobsResponse>
                  <data></data>
              </XmlEmptyBlobsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            data: ""
        },
        appliesTo: "client",
    },
    {
        id: "XmlEmptySelfClosedBlobs",
        documentation: "Empty self closed blobs are deserialized as empty string",
        protocol: restXml,
        code: 200,
        body: """
              <XmlEmptyBlobsResponse>
                  <data/>
              </XmlEmptyBlobsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            data: ""
        },
        appliesTo: "client",
    }
])

/// This tests how timestamps are serialized, including using the
/// default format of date-time and various @timestampFormat trait
/// values.
@http(uri: "/XmlTimestamps", method: "POST")
operation XmlTimestamps {
    input := with [XmlTimestampsInputOutput] {}
    output := with [XmlTimestampsInputOutput] {}
}

apply XmlTimestamps @httpRequestTests([
    {
        id: "XmlTimestamps",
        documentation: "Tests how normal timestamps are serialized",
        protocol: restXml,
        method: "POST",
        uri: "/XmlTimestamps",
        body: """
              <XmlTimestampsRequest>
                  <normal>2014-04-29T18:30:38Z</normal>
              </XmlTimestampsRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            normal: 1398796238
        }
    },
    {
        id: "XmlTimestampsWithDateTimeFormat",
        documentation: "Ensures that the timestampFormat of date-time works like normal timestamps",
        protocol: restXml,
        method: "POST",
        uri: "/XmlTimestamps",
        body: """
              <XmlTimestampsRequest>
                  <dateTime>2014-04-29T18:30:38Z</dateTime>
              </XmlTimestampsRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            dateTime: 1398796238
        }
    },
    {
        id: "XmlTimestampsWithDateTimeOnTargetFormat",
        documentation: "Ensures that the timestampFormat of date-time on the target shape works like normal timestamps",
        protocol: restXml,
        method: "POST",
        uri: "/XmlTimestamps",
        body: """
              <XmlTimestampsRequest>
                  <dateTimeOnTarget>2014-04-29T18:30:38Z</dateTimeOnTarget>
              </XmlTimestampsRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            dateTimeOnTarget: 1398796238
        }
    },
    {
        id: "XmlTimestampsWithEpochSecondsFormat",
        documentation: "Ensures that the timestampFormat of epoch-seconds works",
        protocol: restXml,
        method: "POST",
        uri: "/XmlTimestamps",
        body: """
              <XmlTimestampsRequest>
                  <epochSeconds>1398796238</epochSeconds>
              </XmlTimestampsRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            epochSeconds: 1398796238
        }
    },
    {
        id: "XmlTimestampsWithEpochSecondsOnTargetFormat",
        documentation: "Ensures that the timestampFormat of epoch-seconds on the target shape works",
        protocol: restXml,
        method: "POST",
        uri: "/XmlTimestamps",
        body: """
              <XmlTimestampsRequest>
                  <epochSecondsOnTarget>1398796238</epochSecondsOnTarget>
              </XmlTimestampsRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            epochSecondsOnTarget: 1398796238
        }
    },
    {
        id: "XmlTimestampsWithHttpDateFormat",
        documentation: "Ensures that the timestampFormat of http-date works",
        protocol: restXml,
        method: "POST",
        uri: "/XmlTimestamps",
        body: """
              <XmlTimestampsRequest>
                  <httpDate>Tue, 29 Apr 2014 18:30:38 GMT</httpDate>
              </XmlTimestampsRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            httpDate: 1398796238
        }
    },
    {
        id: "XmlTimestampsWithHttpDateOnTargetFormat",
        documentation: "Ensures that the timestampFormat of http-date on the target shape works",
        protocol: restXml,
        method: "POST",
        uri: "/XmlTimestamps",
        body: """
              <XmlTimestampsRequest>
                  <httpDateOnTarget>Tue, 29 Apr 2014 18:30:38 GMT</httpDateOnTarget>
              </XmlTimestampsRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            httpDateOnTarget: 1398796238
        }
    },
])

apply XmlTimestamps @httpResponseTests([
    {
        id: "XmlTimestamps",
        documentation: "Tests how normal timestamps are serialized",
        protocol: restXml,
        code: 200,
        body: """
              <XmlTimestampsResponse>
                  <normal>2014-04-29T18:30:38Z</normal>
              </XmlTimestampsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            normal: 1398796238
        }
    },
    {
        id: "XmlTimestampsWithDateTimeFormat",
        documentation: "Ensures that the timestampFormat of date-time works like normal timestamps",
        protocol: restXml,
        code: 200,
        body: """
              <XmlTimestampsResponse>
                  <dateTime>2014-04-29T18:30:38Z</dateTime>
              </XmlTimestampsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            dateTime: 1398796238
        }
    },
    {
        id: "XmlTimestampsWithDateTimeOnTargetFormat",
        documentation: "Ensures that the timestampFormat of date-time on the target shape works like normal timestamps",
        protocol: restXml,
        code: 200,
        body: """
              <XmlTimestampsResponse>
                  <dateTimeOnTarget>2014-04-29T18:30:38Z</dateTimeOnTarget>
              </XmlTimestampsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            dateTimeOnTarget: 1398796238
        }
    },
    {
        id: "XmlTimestampsWithEpochSecondsFormat",
        documentation: "Ensures that the timestampFormat of epoch-seconds works",
        protocol: restXml,
        code: 200,
        body: """
              <XmlTimestampsResponse>
                  <epochSeconds>1398796238</epochSeconds>
              </XmlTimestampsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            epochSeconds: 1398796238
        }
    },
    {
        id: "XmlTimestampsWithEpochSecondsOnTargetFormat",
        documentation: "Ensures that the timestampFormat of epoch-seconds on the target shape works",
        protocol: restXml,
        code: 200,
        body: """
              <XmlTimestampsResponse>
                  <epochSecondsOnTarget>1398796238</epochSecondsOnTarget>
              </XmlTimestampsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            epochSecondsOnTarget: 1398796238
        }
    },
    {
        id: "XmlTimestampsWithHttpDateFormat",
        documentation: "Ensures that the timestampFormat of http-date works",
        protocol: restXml,
        code: 200,
        body: """
              <XmlTimestampsResponse>
                  <httpDate>Tue, 29 Apr 2014 18:30:38 GMT</httpDate>
              </XmlTimestampsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            httpDate: 1398796238
        }
    },
    {
        id: "XmlTimestampsWithHttpDateOnTargetFormat",
        documentation: "Ensures that the timestampFormat of http-date on the target shape works",
        protocol: restXml,
        code: 200,
        body: """
              <XmlTimestampsResponse>
                  <httpDateOnTarget>Tue, 29 Apr 2014 18:30:38 GMT</httpDateOnTarget>
              </XmlTimestampsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            httpDateOnTarget: 1398796238
        }
    },
])

@mixin
structure XmlTimestampsInputOutput {
    normal: Timestamp,

    @timestampFormat("date-time")
    dateTime: Timestamp,

    dateTimeOnTarget: DateTime,

    @timestampFormat("epoch-seconds")
    epochSeconds: Timestamp,

    epochSecondsOnTarget: EpochSeconds,

    @timestampFormat("http-date")
    httpDate: Timestamp,

    httpDateOnTarget: HttpDate,
}

/// This example serializes enums as top level properties, in lists, sets, and maps.
@idempotent
@http(uri: "/XmlEnums", method: "PUT")
operation XmlEnums {
    input := with [XmlEnumsInputOutput] {}
    output := with [XmlEnumsInputOutput] {}
}

apply XmlEnums @httpRequestTests([
    {
        id: "XmlEnums",
        documentation: "Serializes simple scalar properties",
        protocol: restXml,
        method: "PUT",
        uri: "/XmlEnums",
        body: """
              <XmlEnumsRequest>
                  <fooEnum1>Foo</fooEnum1>
                  <fooEnum2>0</fooEnum2>
                  <fooEnum3>1</fooEnum3>
                  <fooEnumList>
                      <member>Foo</member>
                      <member>0</member>
                  </fooEnumList>
                  <fooEnumSet>
                      <member>Foo</member>
                      <member>0</member>
                  </fooEnumSet>
                  <fooEnumMap>
                      <entry>
                          <key>hi</key>
                          <value>Foo</value>
                      </entry>
                      <entry>
                          <key>zero</key>
                          <value>0</value>
                      </entry>
                  </fooEnumMap>
              </XmlEnumsRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            fooEnum1: "Foo",
            fooEnum2: "0",
            fooEnum3: "1",
            fooEnumList: ["Foo", "0"],
            fooEnumSet: ["Foo", "0"],
            fooEnumMap: {
                "hi": "Foo",
                "zero": "0"
            }
        }
    }
])

apply XmlEnums @httpResponseTests([
    {
        id: "XmlEnums",
        documentation: "Serializes simple scalar properties",
        protocol: restXml,
        code: 200,
        body: """
              <XmlEnumsResponse>
                  <fooEnum1>Foo</fooEnum1>
                  <fooEnum2>0</fooEnum2>
                  <fooEnum3>1</fooEnum3>
                  <fooEnumList>
                      <member>Foo</member>
                      <member>0</member>
                  </fooEnumList>
                  <fooEnumSet>
                      <member>Foo</member>
                      <member>0</member>
                  </fooEnumSet>
                  <fooEnumMap>
                      <entry>
                          <key>hi</key>
                          <value>Foo</value>
                      </entry>
                      <entry>
                          <key>zero</key>
                          <value>0</value>
                      </entry>
                  </fooEnumMap>
              </XmlEnumsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            fooEnum1: "Foo",
            fooEnum2: "0",
            fooEnum3: "1",
            fooEnumList: ["Foo", "0"],
            fooEnumSet: ["Foo", "0"],
            fooEnumMap: {
                "hi": "Foo",
                "zero": "0"
            }
        }
    }
])

@mixin
structure XmlEnumsInputOutput {
    fooEnum1: FooEnum,
    fooEnum2: FooEnum,
    fooEnum3: FooEnum,
    fooEnumList: FooEnumList,
    fooEnumSet: FooEnumSet,
    fooEnumMap: FooEnumMap,
}

/// This example serializes enums as top level properties, in lists, sets, and maps.
@idempotent
@http(uri: "/XmlIntEnums", method: "PUT")
operation XmlIntEnums {
    input := with [XmlIntEnumsInputOutput] {}
    output := with [XmlIntEnumsInputOutput] {}
}

apply XmlIntEnums @httpRequestTests([
    {
        id: "XmlIntEnums",
        documentation: "Serializes simple scalar properties",
        protocol: restXml,
        method: "PUT",
        uri: "/XmlIntEnums",
        body: """
              <XmlIntEnumsRequest>
                  <intEnum1>1</intEnum1>
                  <intEnum2>2</intEnum2>
                  <intEnum3>3</intEnum3>
                  <intEnumList>
                      <member>1</member>
                      <member>2</member>
                  </intEnumList>
                  <intEnumSet>
                      <member>1</member>
                      <member>2</member>
                  </intEnumSet>
                  <intEnumMap>
                      <entry>
                          <key>a</key>
                          <value>1</value>
                      </entry>
                      <entry>
                          <key>b</key>
                          <value>2</value>
                      </entry>
                  </intEnumMap>
              </XmlIntEnumsRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            intEnum1: 1,
            intEnum2: 2,
            intEnum3: 3,
            intEnumList: [1, 2],
            intEnumSet: [1, 2],
            intEnumMap: {
                "a": 1,
                "b": 2
            }
        }
    }
])

apply XmlIntEnums @httpResponseTests([
    {
        id: "XmlIntEnums",
        documentation: "Serializes simple scalar properties",
        protocol: restXml,
        code: 200,
        body: """
              <XmlIntEnumsResponse>
                  <intEnum1>1</intEnum1>
                  <intEnum2>2</intEnum2>
                  <intEnum3>3</intEnum3>
                  <intEnumList>
                      <member>1</member>
                      <member>2</member>
                  </intEnumList>
                  <intEnumSet>
                      <member>1</member>
                      <member>2</member>
                  </intEnumSet>
                  <intEnumMap>
                      <entry>
                          <key>a</key>
                          <value>1</value>
                      </entry>
                      <entry>
                          <key>b</key>
                          <value>2</value>
                      </entry>
                  </intEnumMap>
              </XmlIntEnumsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            intEnum1: 1,
            intEnum2: 2,
            intEnum3: 3,
            intEnumList: [1, 2],
            intEnumSet: [1, 2],
            intEnumMap: {
                "a": 1,
                "b": 2
            }
        }
    }
])

@mixin
structure XmlIntEnumsInputOutput {
    intEnum1: IntegerEnum,
    intEnum2: IntegerEnum,
    intEnum3: IntegerEnum,
    intEnumList: IntegerEnumList,
    intEnumSet: IntegerEnumSet,
    intEnumMap: IntegerEnumMap,
}

/// Recursive shapes
@idempotent
@http(uri: "/RecursiveShapes", method: "PUT")
operation RecursiveShapes {
    input := {
        nested: RecursiveShapesInputOutputNested1
    }
    output := {
        nested: RecursiveShapesInputOutputNested1
    }
}

apply RecursiveShapes @httpRequestTests([
    {
        id: "RecursiveShapes",
        documentation: "Serializes recursive structures",
        protocol: restXml,
        method: "PUT",
        uri: "/RecursiveShapes",
        body: """
              <RecursiveShapesRequest>
                  <nested>
                      <foo>Foo1</foo>
                      <nested>
                          <bar>Bar1</bar>
                          <recursiveMember>
                              <foo>Foo2</foo>
                              <nested>
                                  <bar>Bar2</bar>
                              </nested>
                          </recursiveMember>
                      </nested>
                  </nested>
              </RecursiveShapesRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            nested: {
                foo: "Foo1",
                nested: {
                    bar: "Bar1",
                    recursiveMember: {
                        foo: "Foo2",
                        nested: {
                            bar: "Bar2"
                        }
                    }
                }
            }
        }
    }
])

apply RecursiveShapes @httpResponseTests([
    {
        id: "RecursiveShapes",
        documentation: "Serializes recursive structures",
        protocol: restXml,
        code: 200,
        body: """
              <RecursiveShapesResponse>
                  <nested>
                      <foo>Foo1</foo>
                      <nested>
                          <bar>Bar1</bar>
                          <recursiveMember>
                              <foo>Foo2</foo>
                              <nested>
                                  <bar>Bar2</bar>
                              </nested>
                          </recursiveMember>
                      </nested>
                  </nested>
              </RecursiveShapesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            nested: {
                foo: "Foo1",
                nested: {
                    bar: "Bar1",
                    recursiveMember: {
                        foo: "Foo2",
                        nested: {
                            bar: "Bar2"
                        }
                    }
                }
            }
        }
    }
])

structure RecursiveShapesInputOutputNested1 {
    foo: String,
    nested: RecursiveShapesInputOutputNested2
}

structure RecursiveShapesInputOutputNested2 {
    bar: String,
    recursiveMember: RecursiveShapesInputOutputNested1,
}

// XML namespace
@http(uri: "/XmlNamespaces", method: "POST")
operation XmlNamespaces {
    input := with [XmlNamespacesInputOutput] {}
    output := with [XmlNamespacesInputOutput] {}
}

apply XmlNamespaces @httpRequestTests([
    {
        id: "XmlNamespaces",
        documentation: "Serializes XML namespaces",
        protocol: restXml,
        method: "POST",
        uri: "/XmlNamespaces",
        body: """
              <XmlNamespacesRequest xmlns="http://foo.com">
                  <nested>
                      <foo xmlns:baz="http://baz.com">Foo</foo>
                      <values xmlns="http://qux.com">
                          <member xmlns="http://bux.com">Bar</member>
                          <member xmlns="http://bux.com">Baz</member>
                      </values>
                  </nested>
              </XmlNamespacesRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            nested: {
                foo: "Foo",
                values: [
                    "Bar",
                    "Baz"
                ]
            }
        }
    }
])

apply XmlNamespaces @httpResponseTests([
    {
        id: "XmlNamespaces",
        documentation: "Serializes XML namespaces",
        protocol: restXml,
        code: 200,
        body: """
              <XmlNamespacesResponse xmlns="http://foo.com">
                  <nested>
                      <foo xmlns:baz="http://baz.com">Foo</foo>
                      <values xmlns="http://qux.com">
                          <member xmlns="http://bux.com">Bar</member>
                          <member xmlns="http://bux.com">Baz</member>
                      </values>
                  </nested>
              </XmlNamespacesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            nested: {
                foo: "Foo",
                values: [
                    "Bar",
                    "Baz"
                ]
            }
        }
    }
])

@mixin
@xmlNamespace(uri: "http://foo.com")
structure XmlNamespacesInputOutput {
    nested: XmlNamespaceNested
}

// Ignored since it's not at the top-level
@xmlNamespace(uri: "http://foo.com")
structure XmlNamespaceNested {
    @xmlNamespace(uri: "http://baz.com", prefix: "baz")
    foo: String,

    @xmlNamespace(uri: "http://qux.com")
    values: XmlNamespacedList
}

list XmlNamespacedList {
    @xmlNamespace(uri: "http://bux.com")
    member: String,
}
