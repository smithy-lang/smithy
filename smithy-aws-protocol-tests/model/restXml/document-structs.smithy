// This file defines test cases that serialize synthesized XML documents
// in the payload of HTTP requests and responses.

$version: "2.0"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use aws.protocoltests.shared#FooEnum
use aws.protocoltests.shared#FooEnumList
use aws.protocoltests.shared#FooEnumSet
use aws.protocoltests.shared#FooEnumMap
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

// This example serializes simple scalar types in the top level XML document.
// Note that headers are not serialized in the payload.
@idempotent
@http(uri: "/SimpleScalarProperties", method: "PUT")
operation SimpleScalarProperties {
    input: SimpleScalarPropertiesInputOutput,
    output: SimpleScalarPropertiesInputOutput
}

apply SimpleScalarProperties @httpRequestTests([
    {
        id: "SimpleScalarProperties",
        documentation: "Serializes simple scalar properties",
        protocol: restXml,
        method: "PUT",
        uri: "/SimpleScalarProperties",
        body: """
              <SimpleScalarPropertiesInputOutput>
                  <stringValue>string</stringValue>
                  <trueBooleanValue>true</trueBooleanValue>
                  <falseBooleanValue>false</falseBooleanValue>
                  <byteValue>1</byteValue>
                  <shortValue>2</shortValue>
                  <integerValue>3</integerValue>
                  <longValue>4</longValue>
                  <floatValue>5.5</floatValue>
                  <DoubleDribble>6.5</DoubleDribble>
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <stringValue>&lt;string&gt;</stringValue>
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <stringValue>  string with white    space  </stringValue>
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <stringValue>   </stringValue>
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <floatValue>NaN</floatValue>
                  <DoubleDribble>NaN</DoubleDribble>
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <floatValue>Infinity</floatValue>
                  <DoubleDribble>Infinity</DoubleDribble>
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <floatValue>-Infinity</floatValue>
                  <DoubleDribble>-Infinity</DoubleDribble>
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <stringValue>string</stringValue>
                  <trueBooleanValue>true</trueBooleanValue>
                  <falseBooleanValue>false</falseBooleanValue>
                  <byteValue>1</byteValue>
                  <shortValue>2</shortValue>
                  <integerValue>3</integerValue>
                  <longValue>4</longValue>
                  <floatValue>5.5</floatValue>
                  <DoubleDribble>6.5</DoubleDribble>
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <stringValue>escaped data: &amp;lt;&#xD;&#10;</stringValue>
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <stringValue>&lt;string&gt;</stringValue>
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <![CDATA[characters representing CDATA]]>
                  <stringValue>string</stringValue>
                  <!--xml comment-->
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <stringValue> string with white    space </stringValue>
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <stringValue>  </stringValue>
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <floatValue>NaN</floatValue>
                  <DoubleDribble>NaN</DoubleDribble>
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <floatValue>Infinity</floatValue>
                  <DoubleDribble>Infinity</DoubleDribble>
              </SimpleScalarPropertiesInputOutput>
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
              <SimpleScalarPropertiesInputOutput>
                  <floatValue>-Infinity</floatValue>
                  <DoubleDribble>-Infinity</DoubleDribble>
              </SimpleScalarPropertiesInputOutput>
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
    input: XmlEmptyStringsInputOutput,
    output: XmlEmptyStringsInputOutput
}

apply XmlEmptyStrings @httpRequestTests([
       {
           id: "XmlEmptyStrings",
           documentation: "Serializes xml empty strings",
           protocol: restXml,
           method: "PUT",
           uri: "/XmlEmptyStrings",
           body: """
                 <XmlEmptyStringsInputOutput>
                     <emptyString></emptyString>
                 </XmlEmptyStringsInputOutput>
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
              <XmlEmptyStringsInputOutput>
                  <emptyString></emptyString>
              </XmlEmptyStringsInputOutput>
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
              <XmlEmptyStringsInputOutput>
                  <emptyString/>
              </XmlEmptyStringsInputOutput>
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

structure XmlEmptyStringsInputOutput {
    emptyString: String
}

/// Blobs are base64 encoded
@http(uri: "/XmlBlobs", method: "POST")
operation XmlBlobs {
    input: XmlBlobsInputOutput,
    output: XmlBlobsInputOutput
}

apply XmlBlobs @httpRequestTests([
    {
        id: "XmlBlobs",
        documentation: "Blobs are base64 encoded",
        protocol: restXml,
        method: "POST",
        uri: "/XmlBlobs",
        body: """
              <XmlBlobsInputOutput>
                  <data>dmFsdWU=</data>
              </XmlBlobsInputOutput>
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
              <XmlBlobsInputOutput>
                  <data>dmFsdWU=</data>
              </XmlBlobsInputOutput>
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
    input: XmlBlobsInputOutput,
    output: XmlBlobsInputOutput
}

apply XmlEmptyBlobs @httpResponseTests([
    {
        id: "XmlEmptyBlobs",
        documentation: "Empty blobs are deserialized as empty string",
        protocol: restXml,
        code: 200,
        body: """
              <XmlBlobsInputOutput>
                  <data></data>
              </XmlBlobsInputOutput>
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
              <XmlBlobsInputOutput>
                  <data/>
              </XmlBlobsInputOutput>
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

structure XmlBlobsInputOutput {
    data: Blob
}

/// This tests how timestamps are serialized, including using the
/// default format of date-time and various @timestampFormat trait
/// values.
@http(uri: "/XmlTimestamps", method: "POST")
operation XmlTimestamps {
    input: XmlTimestampsInputOutput,
    output: XmlTimestampsInputOutput
}

apply XmlTimestamps @httpRequestTests([
    {
        id: "XmlTimestamps",
        documentation: "Tests how normal timestamps are serialized",
        protocol: restXml,
        method: "POST",
        uri: "/XmlTimestamps",
        body: """
              <XmlTimestampsInputOutput>
                  <normal>2014-04-29T18:30:38Z</normal>
              </XmlTimestampsInputOutput>
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
              <XmlTimestampsInputOutput>
                  <dateTime>2014-04-29T18:30:38Z</dateTime>
              </XmlTimestampsInputOutput>
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
        id: "XmlTimestampsWithEpochSecondsFormat",
        documentation: "Ensures that the timestampFormat of epoch-seconds works",
        protocol: restXml,
        method: "POST",
        uri: "/XmlTimestamps",
        body: """
              <XmlTimestampsInputOutput>
                  <epochSeconds>1398796238</epochSeconds>
              </XmlTimestampsInputOutput>
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
        id: "XmlTimestampsWithHttpDateFormat",
        documentation: "Ensures that the timestampFormat of http-date works",
        protocol: restXml,
        method: "POST",
        uri: "/XmlTimestamps",
        body: """
              <XmlTimestampsInputOutput>
                  <httpDate>Tue, 29 Apr 2014 18:30:38 GMT</httpDate>
              </XmlTimestampsInputOutput>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            httpDate: 1398796238
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
              <XmlTimestampsInputOutput>
                  <normal>2014-04-29T18:30:38Z</normal>
              </XmlTimestampsInputOutput>
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
              <XmlTimestampsInputOutput>
                  <dateTime>2014-04-29T18:30:38Z</dateTime>
              </XmlTimestampsInputOutput>
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
        id: "XmlTimestampsWithEpochSecondsFormat",
        documentation: "Ensures that the timestampFormat of epoch-seconds works",
        protocol: restXml,
        code: 200,
        body: """
              <XmlTimestampsInputOutput>
                  <epochSeconds>1398796238</epochSeconds>
              </XmlTimestampsInputOutput>
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
        id: "XmlTimestampsWithHttpDateFormat",
        documentation: "Ensures that the timestampFormat of http-date works",
        protocol: restXml,
        code: 200,
        body: """
              <XmlTimestampsInputOutput>
                  <httpDate>Tue, 29 Apr 2014 18:30:38 GMT</httpDate>
              </XmlTimestampsInputOutput>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            httpDate: 1398796238
        }
    },
])

structure XmlTimestampsInputOutput {
    normal: Timestamp,

    @timestampFormat("date-time")
    dateTime: Timestamp,

    @timestampFormat("epoch-seconds")
    epochSeconds: Timestamp,

    @timestampFormat("http-date")
    httpDate: Timestamp,
}

/// This example serializes enums as top level properties, in lists, sets, and maps.
@idempotent
@http(uri: "/XmlEnums", method: "PUT")
operation XmlEnums {
    input: XmlEnumsInputOutput,
    output: XmlEnumsInputOutput
}

apply XmlEnums @httpRequestTests([
    {
        id: "XmlEnums",
        documentation: "Serializes simple scalar properties",
        protocol: restXml,
        method: "PUT",
        uri: "/XmlEnums",
        body: """
              <XmlEnumsInputOutput>
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
              </XmlEnumsInputOutput>
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
              <XmlEnumsInputOutput>
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
              </XmlEnumsInputOutput>
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

structure XmlEnumsInputOutput {
    fooEnum1: FooEnum,
    fooEnum2: FooEnum,
    fooEnum3: FooEnum,
    fooEnumList: FooEnumList,
    fooEnumSet: FooEnumSet,
    fooEnumMap: FooEnumMap,
}

/// Recursive shapes
@idempotent
@http(uri: "/RecursiveShapes", method: "PUT")
operation RecursiveShapes {
    input: RecursiveShapesInputOutput,
    output: RecursiveShapesInputOutput
}

apply RecursiveShapes @httpRequestTests([
    {
        id: "RecursiveShapes",
        documentation: "Serializes recursive structures",
        protocol: restXml,
        method: "PUT",
        uri: "/RecursiveShapes",
        body: """
              <RecursiveShapesInputOutput>
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
              </RecursiveShapesInputOutput>
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
              <RecursiveShapesInputOutput>
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
              </RecursiveShapesInputOutput>
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

structure RecursiveShapesInputOutput {
    nested: RecursiveShapesInputOutputNested1
}

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
    input: XmlNamespacesInputOutput,
    output: XmlNamespacesInputOutput
}

apply XmlNamespaces @httpRequestTests([
    {
        id: "XmlNamespaces",
        documentation: "Serializes XML namespaces",
        protocol: restXml,
        method: "POST",
        uri: "/XmlNamespaces",
        body: """
              <XmlNamespacesInputOutput xmlns="http://foo.com">
                  <nested>
                      <foo xmlns:baz="http://baz.com">Foo</foo>
                      <values xmlns="http://qux.com">
                          <member xmlns="http://bux.com">Bar</member>
                          <member xmlns="http://bux.com">Baz</member>
                      </values>
                  </nested>
              </XmlNamespacesInputOutput>
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
              <XmlNamespacesInputOutput xmlns="http://foo.com">
                  <nested>
                      <foo xmlns:baz="http://baz.com">Foo</foo>
                      <values xmlns="http://qux.com">
                          <member xmlns="http://bux.com">Bar</member>
                          <member xmlns="http://bux.com">Baz</member>
                      </values>
                  </nested>
              </XmlNamespacesInputOutput>
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

@xmlNamespace(uri: "http://foo.com")
structure XmlNamespacesInputOutput {
    nested: XmlNamespaceNested
}

// Ingored since it's not at the top-level
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
