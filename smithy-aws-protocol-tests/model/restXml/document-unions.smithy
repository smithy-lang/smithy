// This file defines test cases that serialize synthesized XML documents
// in the payload of HTTP requests and responses.

$version: "2.0"
$operationInputSuffix: "Request"
$operationOutputSuffix: "Response"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

// This example serializes simple scalar types in the top level XML document.
// Note that headers are not serialized in the payload.
@idempotent
@http(uri: "/XmlUnions", method: "PUT")
operation XmlUnions {
    input := {
        unionValue: XmlUnionShape
    }
    output := {
        unionValue: XmlUnionShape
    }
}

apply XmlUnions @httpRequestTests([
    {
        id: "XmlUnionsWithStructMember",
        documentation: "Serializes union struct member",
        protocol: restXml,
        method: "PUT",
        uri: "/XmlUnions",
        body: """
              <XmlUnionsRequest>
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
              </XmlUnionsRequest>
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
              <XmlUnionsRequest>
                 <unionValue>
                    <stringValue>some string</stringValue>
                 </unionValue>
              </XmlUnionsRequest>
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
              <XmlUnionsRequest>
                 <unionValue>
                    <booleanValue>true</booleanValue>
                 </unionValue>
              </XmlUnionsRequest>
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
              <XmlUnionsRequest>
                 <unionValue>
                    <unionValue>
                       <booleanValue>true</booleanValue>
                    </unionValue>
                 </unionValue>
              </XmlUnionsRequest>
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
              <XmlUnionsResponse>
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
              </XmlUnionsResponse>
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
              <XmlUnionsResponse>
                 <unionValue>
                    <stringValue>some string</stringValue>
                 </unionValue>
              </XmlUnionsResponse>
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
              <XmlUnionsResponse>
                 <unionValue>
                    <booleanValue>true</booleanValue>
                 </unionValue>
              </XmlUnionsResponse>
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
              <XmlUnionsResponse>
                 <unionValue>
                    <unionValue>
                       <booleanValue>true</booleanValue>
                    </unionValue>
                 </unionValue>
              </XmlUnionsResponse>
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
