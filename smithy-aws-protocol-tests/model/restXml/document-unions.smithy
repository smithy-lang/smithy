// This file defines test cases that serialize synthesized XML documents
// in the payload of HTTP requests and responses.

$version: "2.0"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

// This example serializes simple scalar types in the top level XML document.
// Note that headers are not serialized in the payload.
@idempotent
@http(uri: "/XmlUnions", method: "PUT")
operation XmlUnions {
    input: XmlUnionsInputOutput,
    output: XmlUnionsInputOutput
}

apply XmlUnions @httpRequestTests([
    {
        id: "XmlUnionsWithStructMember",
        documentation: "Serializes union struct member",
        protocol: restXml,
        method: "PUT",
        uri: "/XmlUnions",
        body: """
              <XmlUnionsInputOutput>
                  <unionValue>
                     <structValue>
                        <stringValue>string</stringValue>
                        <booleanValue>true</booleanValue>
                        <byteValue>1</byteValue>
                        <shortValue>2</shortValue>
                        <integerValue>3</integerValue>
                        <longValue>4</longValue>
                        <floatValue>5.5</floatValue>
                        <doubleValue>6.5</doubleValue>
                     </structValue>
                  </unionValue>
              </XmlUnionsInputOutput>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
        },
        params: {
            unionValue: {
                structValue: {
                    stringValue: "string",
                    booleanValue: true,
                    byteValue: 1,
                    shortValue: 2,
                    integerValue: 3,
                    longValue: 4,
                    floatValue: 5.5,
                    doubleValue: 6.5,
                },
            },
        }
    },
    {
        id: "XmlUnionsWithStringMember",
        documentation: "serialize union string member",
        protocol: restXml,
        method: "PUT",
        uri: "/XmlUnions",
        body: """
              <XmlUnionsInputOutput>
                 <unionValue>
                    <stringValue>some string</stringValue>
                 </unionValue>
              </XmlUnionsInputOutput>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
        },
        params: {
            unionValue: {
                stringValue: "some string",
            },
        }
    },
    {
        id: "XmlUnionsWithBooleanMember",
        documentation: "Serializes union boolean member",
        protocol: restXml,
        method: "PUT",
        uri: "/XmlUnions",
        body: """
              <XmlUnionsInputOutput>
                 <unionValue>
                    <booleanValue>true</booleanValue>
                 </unionValue>
              </XmlUnionsInputOutput>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
        },
        params: {
            unionValue: {
                booleanValue: true,
            },
        }
    },
    {
        id: "XmlUnionsWithUnionMember",
        documentation: "Serializes union member",
        protocol: restXml,
        method: "PUT",
        uri: "/XmlUnions",
        body: """
              <XmlUnionsInputOutput>
                 <unionValue>
                    <unionValue>
                       <booleanValue>true</booleanValue>
                    </unionValue>
                 </unionValue>
              </XmlUnionsInputOutput>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
        },
        params: {
            unionValue: {
                unionValue: {
                    booleanValue: true,
                },
            },
        }
    },
])

apply XmlUnions @httpResponseTests([
    {
        id: "XmlUnionsWithStructMember",
        documentation: "Serializes union struct member",
        protocol: restXml,
        code: 200,
        body: """
              <XmlUnionsInputOutput>
                  <unionValue>
                     <structValue>
                        <stringValue>string</stringValue>
                        <booleanValue>true</booleanValue>
                        <byteValue>1</byteValue>
                        <shortValue>2</shortValue>
                        <integerValue>3</integerValue>
                        <longValue>4</longValue>
                        <floatValue>5.5</floatValue>
                        <doubleValue>6.5</doubleValue>
                     </structValue>
                  </unionValue>
              </XmlUnionsInputOutput>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
        },
        params: {
            unionValue: {
                structValue: {
                    stringValue: "string",
                    booleanValue: true,
                    byteValue: 1,
                    shortValue: 2,
                    integerValue: 3,
                    longValue: 4,
                    floatValue: 5.5,
                    doubleValue: 6.5,
                },
            },
        }
    },
    {
        id: "XmlUnionsWithStringMember",
        documentation: "Serializes union string member",
        protocol: restXml,
        code: 200,
        body: """
              <XmlUnionsInputOutput>
                 <unionValue>
                    <stringValue>some string</stringValue>
                 </unionValue>
              </XmlUnionsInputOutput>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
        },
        params: {
            unionValue: {
                stringValue: "some string",
            },
        }
    },
    {
        id: "XmlUnionsWithBooleanMember",
        documentation: "Serializes union boolean member",
        protocol: restXml,
        code: 200,
        body: """
              <XmlUnionsInputOutput>
                 <unionValue>
                    <booleanValue>true</booleanValue>
                 </unionValue>
              </XmlUnionsInputOutput>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
        },
        params: {
            unionValue: {
                booleanValue: true,
            },
        }
    },
    {
        id: "XmlUnionsWithUnionMember",
        documentation: "Serializes union member",
        protocol: restXml,
        code: 200,
        body: """
              <XmlUnionsInputOutput>
                 <unionValue>
                    <unionValue>
                       <booleanValue>true</booleanValue>
                    </unionValue>
                 </unionValue>
              </XmlUnionsInputOutput>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
        },
        params: {
            unionValue: {
                unionValue: {
                    booleanValue: true,
                },
            },
        }
    },
])

structure XmlUnionsInputOutput {
    unionValue: XmlUnionShape,
}

union XmlUnionShape {
    stringValue: String,
    booleanValue: Boolean,
    byteValue: Byte,
    shortValue: Short,
    integerValue: Integer,
    longValue: Long,
    floatValue: Float,
    doubleValue: Double,

    unionValue: XmlUnionShape,
    structValue: XmlNestedUnionStruct,
}

structure XmlNestedUnionStruct {
    stringValue: String,
    booleanValue: Boolean,
    byteValue: Byte,
    shortValue: Short,
    integerValue: Integer,
    longValue: Long,
    floatValue: Float,
    doubleValue: Double,
}
