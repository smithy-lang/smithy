// This file defines test cases that serialize synthesized XML documents
// in the payload of HTTP requests and responses.

$version: "0.5.0"

namespace aws.protocols.tests.query

use aws.protocols.tests.shared#FooEnum
use aws.protocols.tests.shared#FooEnumList
use aws.protocols.tests.shared#FooEnumSet
use aws.protocols.tests.shared#FooEnumMap
use smithy.test#httpResponseTests

// This example serializes simple scalar types in the top level XML document.
// Note that headers are not serialized in the payload.
operation SimpleScalarXmlProperties() -> SimpleScalarXmlPropertiesOutput

apply SimpleScalarXmlProperties @httpResponseTests([
    {
        id: "QuerySimpleScalarProperties",
        description: "Serializes simple scalar properties",
        protocol: "aws.query",
        code: 200,
        body: """
              <SimpleScalarXmlPropertiesResponse xmlns="https://example.com/">
                  <SimpleScalarXmlPropertiesResult>
                      <stringValue>string</stringValue>
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
            trueBooleanValue: true,
            falseBooleanValue: false,
            byteValue: 1,
            shortValue: 2,
            integerValue: 3,
            longValue: 4,
            floatValue: 5.5,
            doubleValue: 6.5,
        }
    }
])

structure SimpleScalarXmlPropertiesOutput {
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

/// Blobs are base64 encoded
operation XmlBlobs() -> XmlBlobsOutput

apply XmlBlobs @httpResponseTests([
    {
        id: "QueryXmlBlobs",
        description: "Blobs are base64 encoded",
        protocol: "aws.query",
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

structure XmlBlobsOutput {
    data: Blob
}

/// This tests how timestamps are serialized, including using the
/// default format of date-time and various @timestampFormat trait
/// values.
operation XmlTimestamps() -> XmlTimestampsOutput

apply XmlTimestamps @httpResponseTests([
    {
        id: "QueryXmlTimestamps",
        description: "Tests how normal timestamps are serialized",
        protocol: "aws.query",
        code: 200,
        body: """
              <XmlTimestampsResponse xmlns="https://example.com/">
                  <QueryXmlTimestampsResult>
                      <normal>2014-04-29T18:30:38Z</normal>
                  </QueryXmlTimestampsResult>
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
        description: "Ensures that the timestampFormat of date-time works like normal timestamps",
        protocol: "aws.query",
        code: 200,
        body: """
              <XmlTimestampsResponse xmlns="https://example.com/">
                  <QueryXmlTimestampsResult>
                      <dateTime>2014-04-29T18:30:38Z</dateTime>
                  </QueryXmlTimestampsResult>
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
        id: "QueryXmlTimestampsWithEpochSecondsFormat",
        description: "Ensures that the timestampFormat of epoch-seconds works",
        protocol: "aws.query",
        code: 200,
        body: """
              <XmlTimestampsResponse xmlns="https://example.com/">
                  <QueryXmlTimestampsResult>
                      <epochSeconds>1398796238</epochSeconds>
                  </QueryXmlTimestampsResult>
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
        id: "QueryXmlTimestampsWithHttpDateFormat",
        description: "Ensures that the timestampFormat of http-date works",
        protocol: "aws.query",
        code: 200,
        body: """
              <XmlTimestampsResponse xmlns="https://example.com/">
                  <QueryXmlTimestampsResult>
                      <httpDate>Tue, 29 Apr 2014 18:30:38 GMT</httpDate>
                  </QueryXmlTimestampsResult>
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
])

structure XmlTimestampsOutput {
    normal: Timestamp,

    @timestampFormat("date-time")
    dateTime: Timestamp,

    @timestampFormat("epoch-seconds")
    epochSeconds: Timestamp,

    @timestampFormat("http-date")
    httpDate: Timestamp,
}

/// This example serializes enums as top level properties, in lists, sets, and maps.
operation XmlEnums() -> XmlEnumsOutput

apply XmlEnums @httpResponseTests([
    {
        id: "QueryXmlEnums",
        description: "Serializes simple scalar properties",
        protocol: "aws.query",
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

/// Recursive shapes
operation RecursiveXmlShapes() -> RecursiveXmlShapesOutput

apply RecursiveXmlShapes @httpResponseTests([
    {
        id: "QueryRecursiveShapes",
        description: "Serializes recursive structures",
        protocol: "aws.query",
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
operation XmlNamespaces() -> XmlNamespacesOutput

apply XmlNamespaces @httpResponseTests([
    {
        id: "QueryXmlNamespaces",
        description: "Serializes XML namespaces",
        protocol: "aws.query",
        code: 200,
        body: """
              <XmlNamespacesResponse xmlns="http://foo.com" xmlns="https://example.com/">
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

/// The xmlName trait on the output structure is ignored in AWS Query.
///
/// The wrapping element is always operation name + "Response", and
/// inside of that wrapper is another wrapper named operation name + "Result".
operation IgnoresWrappingXmlName() -> IgnoresWrappingXmlNameOutput

apply IgnoresWrappingXmlName @httpResponseTests([
    {
        id: "QueryIgnoresWrappingXmlName",
        description: "The xmlName trait on the output structure is ignored in AWS Query",
        protocol: "aws.query",
        code: 200,
        body: """
              <IgnoresWrappingXmlNameResponse xmlns="http://foo.com" xmlns="https://example.com/">
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
