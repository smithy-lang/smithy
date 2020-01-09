// This file defines test cases that serialize synthesized XML documents
// in the payload of HTTP requests and responses.

$version: "0.5.0"

namespace aws.protocols.tests.restxml

use aws.protocols.tests.shared#FooEnum
use aws.protocols.tests.shared#FooEnumList
use aws.protocols.tests.shared#FooEnumSet
use aws.protocols.tests.shared#FooEnumMap
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

// This example serializes simple scalar types in the top level XML document.
// Note that headers are not serialized in the payload.
@idempotent
@http(uri: "/SimpleScalarProperties", method: "PUT")
operation SimpleScalarProperties(SimpleScalarPropertiesInputOutput) -> SimpleScalarPropertiesInputOutput

apply SimpleScalarProperties @httpRequestTests([
    {
        id: "SimpleScalarProperties",
        description: "Serializes simple scalar properties",
        protocol: "aws.rest-xml",
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
    }
])

apply SimpleScalarProperties @httpResponseTests([
    {
        id: "SimpleScalarProperties",
        description: "Serializes simple scalar properties",
        protocol: "aws.rest-xml",
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
    }
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

/// Blobs are base64 encoded
@http(uri: "/XmlBlobs", method: "POST")
operation XmlBlobs(XmlBlobsInputOutput) -> XmlBlobsInputOutput

apply XmlBlobs @httpRequestTests([
    {
        id: "XmlBlobs",
        description: "Blobs are base64 encoded",
        protocol: "aws.rest-xml",
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
        description: "Blobs are base64 encoded",
        protocol: "aws.rest-xml",
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

structure XmlBlobsInputOutput {
    data: Blob
}

/// This tests how timestamps are serialized, including using the
/// default format of date-time and various @timestampFormat trait
/// values.
@http(uri: "/XmlTimestamps", method: "POST")
operation XmlTimestamps(XmlTimestampsInputOutput) -> XmlTimestampsInputOutput

apply XmlTimestamps @httpRequestTests([
    {
        id: "XmlTimestamps",
        description: "Tests how normal timestamps are serialized",
        protocol: "aws.rest-xml",
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
        description: "Ensures that the timestampFormat of date-time works like normal timestamps",
        protocol: "aws.rest-xml",
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
        description: "Ensures that the timestampFormat of epoch-seconds works",
        protocol: "aws.rest-xml",
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
        description: "Ensures that the timestampFormat of http-date works",
        protocol: "aws.rest-xml",
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
        description: "Tests how normal timestamps are serialized",
        protocol: "aws.rest-xml",
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
        description: "Ensures that the timestampFormat of date-time works like normal timestamps",
        protocol: "aws.rest-xml",
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
        description: "Ensures that the timestampFormat of epoch-seconds works",
        protocol: "aws.rest-xml",
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
        description: "Ensures that the timestampFormat of http-date works",
        protocol: "aws.rest-xml",
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
operation XmlEnums(XmlEnumsInputOutput) -> XmlEnumsInputOutput

apply XmlEnums @httpRequestTests([
    {
        id: "XmlEnums",
        description: "Serializes simple scalar properties",
        protocol: "aws.rest-xml",
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
        description: "Serializes simple scalar properties",
        protocol: "aws.rest-xml",
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
operation RecursiveShapes(RecursiveShapesInputOutput) -> RecursiveShapesInputOutput

apply RecursiveShapes @httpRequestTests([
    {
        id: "RecursiveShapes",
        description: "Serializes recursive structures",
        protocol: "aws.rest-xml",
        method: "PUT",
        uri: "/XmlEnums",
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
        description: "Serializes recursive structures",
        protocol: "aws.rest-xml",
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
operation XmlNamespaces(XmlNamespacesInputOutput) -> XmlNamespacesInputOutput

apply XmlNamespaces @httpRequestTests([
    {
        id: "XmlNamespaces",
        description: "Serializes XML namespaces",
        protocol: "aws.rest-xml",
        method: "PUT",
        uri: "/XmlNamespaces",
        body: """
              <RecursiveShapesInputOutput xmlns="http://foo.com">
                  <nested>
                      <foo xmlns:baz="http://baz.com">Foo</foo>
                      <values xmlns="http://qux.com">
                          <member xmlns="http://bux.com">Bar</member>
                          <member xmlns="http://bux.com">Baz</member>
                      </values>
                  </nested>
              </RecursiveShapesInputOutput>
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
        description: "Serializes XML namespaces",
        protocol: "aws.rest-xml",
        code: 200,
        body: """
              <RecursiveShapesInputOutput xmlns="http://foo.com">
                  <nested>
                      <foo xmlns:baz="http://baz.com">Foo</foo>
                      <values xmlns="http://qux.com">
                          <member xmlns="http://bux.com">Bar</member>
                          <member xmlns="http://bux.com">Baz</member>
                      </values>
                  </nested>
              </RecursiveShapesInputOutput>
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
