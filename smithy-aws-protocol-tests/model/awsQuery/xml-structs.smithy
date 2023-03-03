// This file defines test cases that serialize XML output structures.

$version: "2.0"

namespace aws.protocoltests.query

use aws.protocols#awsQuery
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
use smithy.test#httpResponseTests

// This example serializes simple scalar types in the top level XML document.
// Note that headers are not serialized in the payload.
operation SimpleScalarXmlProperties {
    output: SimpleScalarXmlPropertiesOutput
}

apply SimpleScalarXmlProperties @httpResponseTests([
    {
        id: "QuerySimpleScalarProperties",
        documentation: "Serializes simple scalar properties",
        protocol: awsQuery,
        code: 200,
        body: """
              <SimpleScalarXmlPropertiesResponse xmlns="https://example.com/">
                  <SimpleScalarXmlPropertiesResult>
                      <stringValue>string</stringValue>
                      <emptyStringValue/>
                      <trueBooleanValue>true</trueBooleanValue>
                      <falseBooleanValue>false</falseBooleanValue>
                      <byteValue>1</byteValue>
                      <shortValue>2</shortValue>
                      <integerValue>3</integerValue>
                      <longValue>4</longValue>
                      <floatValue>5.5</floatValue>
                      <DoubleDribble>6.5</DoubleDribble>
                  </SimpleScalarXmlPropertiesResult>
              </SimpleScalarXmlPropertiesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            stringValue: "string",
            emptyStringValue: "",
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
        id: "AwsQuerySupportsNaNFloatOutputs",
        documentation: "Supports handling NaN float values.",
        protocol: awsQuery,
        code: 200,
        body: """
              <SimpleScalarXmlPropertiesResponse xmlns="https://example.com/">
                  <SimpleScalarXmlPropertiesResult>
                      <floatValue>NaN</floatValue>
                      <DoubleDribble>NaN</DoubleDribble>
                  </SimpleScalarXmlPropertiesResult>
              </SimpleScalarXmlPropertiesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            floatValue: "NaN",
            doubleValue: "NaN",
        }
    },
    {
        id: "AwsQuerySupportsInfinityFloatOutputs",
        documentation: "Supports handling Infinity float values.",
        protocol: awsQuery,
        code: 200,
        body: """
              <SimpleScalarXmlPropertiesResponse xmlns="https://example.com/">
                  <SimpleScalarXmlPropertiesResult>
                      <floatValue>Infinity</floatValue>
                      <DoubleDribble>Infinity</DoubleDribble>
                  </SimpleScalarXmlPropertiesResult>
              </SimpleScalarXmlPropertiesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            floatValue: "Infinity",
            doubleValue: "Infinity",
        }
    },
    {
        id: "AwsQuerySupportsNegativeInfinityFloatOutputs",
        documentation: "Supports handling -Infinity float values.",
        protocol: awsQuery,
        code: 200,
        body: """
              <SimpleScalarXmlPropertiesResponse xmlns="https://example.com/">
                  <SimpleScalarXmlPropertiesResult>
                      <floatValue>-Infinity</floatValue>
                      <DoubleDribble>-Infinity</DoubleDribble>
                  </SimpleScalarXmlPropertiesResult>
              </SimpleScalarXmlPropertiesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            floatValue: "-Infinity",
            doubleValue: "-Infinity",
        }
    },
])

structure SimpleScalarXmlPropertiesOutput {
    stringValue: String,
    emptyStringValue: String,
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

/// Blobs are base64 encoded
operation XmlBlobs {
    output: XmlBlobsOutput
}

apply XmlBlobs @httpResponseTests([
    {
        id: "QueryXmlBlobs",
        documentation: "Blobs are base64 encoded",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlBlobsResponse xmlns="https://example.com/">
                  <XmlBlobsResult>
                      <data>dmFsdWU=</data>
                  </XmlBlobsResult>
              </XmlBlobsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            data: "value"
        }
    }
])

// Operation for client only
@tags(["client-only"])
operation XmlEmptyBlobs {
    output: XmlBlobsOutput
}

apply XmlEmptyBlobs @httpResponseTests([
    {
        id: "QueryXmlEmptyBlobs",
        documentation: "Empty blobs are deserialized as empty string",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlEmptyBlobsResponse xmlns="https://example.com/">
                  <XmlEmptyBlobsResult>
                      <data></data>
                  </XmlEmptyBlobsResult>
              </XmlEmptyBlobsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            data: ""
        },
        appliesTo: "client",
    },
    {
        id: "QueryXmlEmptySelfClosedBlobs",
        documentation: "Empty self closed blobs are deserialized as empty string",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlEmptyBlobsResponse xmlns="https://example.com/">
                  <XmlEmptyBlobsResult>
                      <data/>
                  </XmlEmptyBlobsResult>
              </XmlEmptyBlobsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            data: ""
        },
        appliesTo: "client",
    }
])

structure XmlBlobsOutput {
    data: Blob
}

/// This tests how timestamps are serialized, including using the
/// default format of date-time and various @timestampFormat trait
/// values.
operation XmlTimestamps {
    output: XmlTimestampsOutput
}

apply XmlTimestamps @httpResponseTests([
    {
        id: "QueryXmlTimestamps",
        documentation: "Tests how normal timestamps are serialized",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlTimestampsResponse xmlns="https://example.com/">
                  <XmlTimestampsResult>
                      <normal>2014-04-29T18:30:38Z</normal>
                  </XmlTimestampsResult>
              </XmlTimestampsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            normal: 1398796238
        }
    },
    {
        id: "QueryXmlTimestampsWithDateTimeFormat",
        documentation: "Ensures that the timestampFormat of date-time works like normal timestamps",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlTimestampsResponse xmlns="https://example.com/">
                  <XmlTimestampsResult>
                      <dateTime>2014-04-29T18:30:38Z</dateTime>
                  </XmlTimestampsResult>
              </XmlTimestampsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            dateTime: 1398796238
        }
    },
    {
        id: "QueryXmlTimestampsWithDateTimeOnTargetFormat",
        documentation: "Ensures that the timestampFormat of date-time on the target shape works like normal timestamps",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlTimestampsResponse xmlns="https://example.com/">
                  <XmlTimestampsResult>
                      <dateTimeOnTarget>2014-04-29T18:30:38Z</dateTimeOnTarget>
                  </XmlTimestampsResult>
              </XmlTimestampsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            dateTimeOnTarget: 1398796238
        }
    },
    {
        id: "QueryXmlTimestampsWithEpochSecondsFormat",
        documentation: "Ensures that the timestampFormat of epoch-seconds works",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlTimestampsResponse xmlns="https://example.com/">
                  <XmlTimestampsResult>
                      <epochSeconds>1398796238</epochSeconds>
                  </XmlTimestampsResult>
              </XmlTimestampsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            epochSeconds: 1398796238
        }
    },
    {
        id: "QueryXmlTimestampsWithEpochSecondsOnTargetFormat",
        documentation: "Ensures that the timestampFormat of epoch-seconds on the target shape works",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlTimestampsResponse xmlns="https://example.com/">
                  <XmlTimestampsResult>
                      <epochSecondsOnTarget>1398796238</epochSecondsOnTarget>
                  </XmlTimestampsResult>
              </XmlTimestampsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            epochSecondsOnTarget: 1398796238
        }
    },
    {
        id: "QueryXmlTimestampsWithHttpDateFormat",
        documentation: "Ensures that the timestampFormat of http-date works",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlTimestampsResponse xmlns="https://example.com/">
                  <XmlTimestampsResult>
                      <httpDate>Tue, 29 Apr 2014 18:30:38 GMT</httpDate>
                  </XmlTimestampsResult>
              </XmlTimestampsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            httpDate: 1398796238
        }
    },
    {
        id: "QueryXmlTimestampsWithHttpDateOnTargetFormat",
        documentation: "Ensures that the timestampFormat of http-date on the target shape works",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlTimestampsResponse xmlns="https://example.com/">
                  <XmlTimestampsResult>
                      <httpDateOnTarget>Tue, 29 Apr 2014 18:30:38 GMT</httpDateOnTarget>
                  </XmlTimestampsResult>
              </XmlTimestampsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            httpDateOnTarget: 1398796238
        }
    },
])

structure XmlTimestampsOutput {
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
operation XmlEnums {
    output: XmlEnumsOutput
}

apply XmlEnums @httpResponseTests([
    {
        id: "QueryXmlEnums",
        documentation: "Serializes simple scalar properties",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlEnumsResponse xmlns="https://example.com/">
                  <XmlEnumsResult>
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
                  </XmlEnumsResult>
              </XmlEnumsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
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

structure XmlEnumsOutput {
    fooEnum1: FooEnum,
    fooEnum2: FooEnum,
    fooEnum3: FooEnum,
    fooEnumList: FooEnumList,
    fooEnumSet: FooEnumSet,
    fooEnumMap: FooEnumMap,
}

/// This example serializes enums as top level properties, in lists, sets, and maps.
operation XmlIntEnums {
    output: XmlIntEnumsOutput
}

apply XmlIntEnums @httpResponseTests([
    {
        id: "QueryXmlIntEnums",
        documentation: "Serializes simple scalar properties",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlIntEnumsResponse xmlns="https://example.com/">
                  <XmlIntEnumsResult>
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
                  </XmlIntEnumsResult>
              </XmlIntEnumsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
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

structure XmlIntEnumsOutput {
    intEnum1: IntegerEnum,
    intEnum2: IntegerEnum,
    intEnum3: IntegerEnum,
    intEnumList: IntegerEnumList,
    intEnumSet: IntegerEnumSet,
    intEnumMap: IntegerEnumMap,
}

/// Recursive shapes
operation RecursiveXmlShapes {
    output: RecursiveXmlShapesOutput
}

apply RecursiveXmlShapes @httpResponseTests([
    {
        id: "QueryRecursiveShapes",
        documentation: "Serializes recursive structures",
        protocol: awsQuery,
        code: 200,
        body: """
              <RecursiveXmlShapesResponse xmlns="https://example.com/">
                  <RecursiveXmlShapesResult>
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
                  </RecursiveXmlShapesResult>
              </RecursiveXmlShapesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
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

structure RecursiveXmlShapesOutput {
    nested: RecursiveXmlShapesOutputNested1
}

structure RecursiveXmlShapesOutputNested1 {
    foo: String,
    nested: RecursiveXmlShapesOutputNested2
}

structure RecursiveXmlShapesOutputNested2 {
    bar: String,
    recursiveMember: RecursiveXmlShapesOutputNested1,
}

// XML namespace
operation XmlNamespaces {
    output: XmlNamespacesOutput
}

apply XmlNamespaces @httpResponseTests([
    {
        id: "QueryXmlNamespaces",
        documentation: "Serializes XML namespaces",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlNamespacesResponse xmlns="https://example.com/">
                  <XmlNamespacesResult>
                      <nested>
                          <foo xmlns:baz="http://baz.com">Foo</foo>
                          <values xmlns="http://qux.com">
                              <member xmlns="http://bux.com">Bar</member>
                              <member xmlns="http://bux.com">Baz</member>
                          </values>
                      </nested>
                  </XmlNamespacesResult>
              </XmlNamespacesResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
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

// The xmlNamespace trait is *ignored* on the outer structure when using AWS Query.
// It only honors the xmlNamespace set on the service shape.
@xmlNamespace(uri: "http://foo.com")
structure XmlNamespacesOutput {
    nested: XmlNamespaceNested
}

// Ignored since it's not at the top-level
@xmlNamespace(uri: "http://boo.com")
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

/// The xmlName trait on the output structure is ignored in AWS Query.
///
/// The wrapping element is always operation name + "Response", and
/// inside of that wrapper is another wrapper named operation name + "Result".
operation IgnoresWrappingXmlName {
    output: IgnoresWrappingXmlNameOutput
}

apply IgnoresWrappingXmlName @httpResponseTests([
    {
        id: "QueryIgnoresWrappingXmlName",
        documentation: "The xmlName trait on the output structure is ignored in AWS Query",
        protocol: awsQuery,
        code: 200,
        body: """
              <IgnoresWrappingXmlNameResponse xmlns="https://example.com/">
                  <IgnoresWrappingXmlNameResult>
                      <foo>bar</foo>
                  </IgnoresWrappingXmlNameResult>
              </IgnoresWrappingXmlNameResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            foo: "bar"
        }
    }
])

@xmlName("IgnoreMe")
structure IgnoresWrappingXmlNameOutput {
    foo: String
}
