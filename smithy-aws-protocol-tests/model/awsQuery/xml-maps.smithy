// This file defines test cases that serialize maps in XML payloads.

$version: "2.0"

namespace aws.protocoltests.query

use aws.protocols#awsQuery
use aws.protocoltests.shared#FooEnumMap
use aws.protocoltests.shared#GreetingStruct
use smithy.test#httpResponseTests

/// The example tests basic map serialization.
operation XmlMaps {
    output: XmlMapsOutput
}

apply XmlMaps @httpResponseTests([
    {
        id: "QueryXmlMaps",
        documentation: "Tests for XML map serialization",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlMapsResponse xmlns="https://example.com/">
                  <XmlMapsResult>
                      <myMap>
                          <entry>
                              <key>foo</key>
                              <value>
                                  <hi>there</hi>
                              </value>
                          </entry>
                          <entry>
                              <key>baz</key>
                              <value>
                                  <hi>bye</hi>
                              </value>
                          </entry>
                      </myMap>
                  </XmlMapsResult>
              </XmlMapsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            myMap: {
                foo: {
                    hi: "there"
                },
                baz: {
                    hi: "bye"
                }
            }
        }
    }
])

// Operation for client only
@tags(["client-only"])
operation XmlEmptyMaps {
    output: XmlMapsOutput
}

apply XmlEmptyMaps @httpResponseTests([
    {
        id: "QueryXmlEmptyMaps",
        documentation: "Deserializes Empty XML maps",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlEmptyMapsResponse xmlns="https://example.com/">
                  <XmlEmptyMapsResult>
                      <myMap>
                      </myMap>
                  </XmlEmptyMapsResult>
              </XmlEmptyMapsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            myMap: {}
        },
        appliesTo: "client",
    },
    {
        id: "QueryXmlEmptySelfClosedMaps",
        documentation: "Deserializes Self-Closed XML maps",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlEmptyMapsResponse xmlns="https://example.com/">
                  <XmlEmptyMapsResult>
                      <myMap/>
                  </XmlEmptyMapsResult>
              </XmlEmptyMapsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            myMap: {}
        },
        appliesTo: "client",
    }
])


structure XmlMapsOutput {
    myMap: XmlMapsOutputMap,
}

map XmlMapsOutputMap {
    key: String,
    value: GreetingStruct
}

// This example tests maps with @xmlName on members.
operation XmlMapsXmlName {
    output: XmlMapsXmlNameOutput
}

apply XmlMapsXmlName @httpResponseTests([
    {
        id: "QueryQueryXmlMapsXmlName",
        documentation: "Serializes XML lists",
        protocol: awsQuery,
        code: 200,
        body: """
              <XmlMapsXmlNameResponse xmlns="https://example.com/">
                  <XmlMapsXmlNameResult>
                      <myMap>
                          <entry>
                              <Attribute>foo</Attribute>
                              <Setting>
                                  <hi>there</hi>
                              </Setting>
                          </entry>
                          <entry>
                              <Attribute>baz</Attribute>
                              <Setting>
                                  <hi>bye</hi>
                              </Setting>
                          </entry>
                      </myMap>
                  </XmlMapsXmlNameResult>
              </XmlMapsXmlNameResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            myMap: {
                foo: {
                    hi: "there"
                },
                baz: {
                    hi: "bye"
                }
            }
        }
    }
])

structure XmlMapsXmlNameOutput {
    myMap: XmlMapsXmlNameOutputMap,
}

map XmlMapsXmlNameOutputMap {
    @xmlName("Attribute")
    key: String,

    @xmlName("Setting")
    value: GreetingStruct
}

/// Flattened maps
operation FlattenedXmlMap {
    output: FlattenedXmlMapOutput
}

apply FlattenedXmlMap @httpResponseTests([
    {
        id: "QueryQueryFlattenedXmlMap",
        documentation: "Serializes flattened XML maps in responses",
        protocol: awsQuery,
        code: 200,
        body: """
              <FlattenedXmlMapResponse xmlns="https://example.com/">
                  <FlattenedXmlMapResult>
                      <myMap>
                          <key>foo</key>
                          <value>Foo</value>
                      </myMap>
                      <myMap>
                          <key>baz</key>
                          <value>Baz</value>
                      </myMap>
                  </FlattenedXmlMapResult>
              </FlattenedXmlMapResponse>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            myMap: {
                foo: "Foo",
                baz: "Baz"
            }
        }
    }
])

structure FlattenedXmlMapOutput {
    @xmlFlattened
    myMap: FooEnumMap,
}

/// Flattened maps with @xmlName
operation FlattenedXmlMapWithXmlName {
    output: FlattenedXmlMapWithXmlNameOutput
}

apply FlattenedXmlMapWithXmlName @httpResponseTests([
    {
        id: "QueryQueryFlattenedXmlMapWithXmlName",
        documentation: "Serializes flattened XML maps in responses that have xmlName on members",
        protocol: awsQuery,
        code: 200,
        body: """
              <FlattenedXmlMapWithXmlNameResponse xmlns="https://example.com/">
                  <FlattenedXmlMapWithXmlNameResult>
                      <KVP>
                          <K>a</K>
                          <V>A</V>
                      </KVP>
                      <KVP>
                          <K>b</K>
                          <V>B</V>
                      </KVP>
                  </FlattenedXmlMapWithXmlNameResult>
              </FlattenedXmlMapWithXmlNameResponse>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            myMap: {
                a: "A",
                b: "B",
            }
        }
    }
])

structure FlattenedXmlMapWithXmlNameOutput {
    @xmlFlattened
    @xmlName("KVP")
    myMap: FlattenedXmlMapWithXmlNameOutputMap,
}

map FlattenedXmlMapWithXmlNameOutputMap {
    @xmlName("K")
    key: String,

    @xmlName("V")
    value: String,
}

/// Flattened maps with @xmlNamespace and @xmlName
operation FlattenedXmlMapWithXmlNamespace {
    output: FlattenedXmlMapWithXmlNamespaceOutput
}

apply FlattenedXmlMapWithXmlNamespace @httpResponseTests([
    {
        id: "QueryQueryFlattenedXmlMapWithXmlNamespace",
        documentation: "Serializes flattened XML maps in responses that have xmlNamespace and xmlName on members",
        protocol: awsQuery,
        code: 200,
        body: """
              <FlattenedXmlMapWithXmlNamespaceResponse xmlns="https://example.com/">
                  <FlattenedXmlMapWithXmlNamespaceResult>
                      <KVP xmlns="https://the-member.example.com">
                          <K xmlns="https://the-key.example.com">a</K>
                          <V xmlns="https://the-value.example.com">A</V>
                      </KVP>
                      <KVP xmlns="https://the-member.example.com">
                          <K xmlns="https://the-key.example.com">b</K>
                          <V xmlns="https://the-value.example.com">B</V>
                      </KVP>
                  </FlattenedXmlMapWithXmlNamespaceResult>
              </FlattenedXmlMapWithXmlNamespaceResponse>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            myMap: {
                a: "A",
                b: "B",
            }
        }
    }
])

structure FlattenedXmlMapWithXmlNamespaceOutput {
    @xmlFlattened
    @xmlName("KVP")
    @xmlNamespace(uri: "https://the-member.example.com")
    myMap: FlattenedXmlMapWithXmlNamespaceOutputMap,
}

map FlattenedXmlMapWithXmlNamespaceOutputMap {
    @xmlName("K")
    @xmlNamespace(uri: "https://the-key.example.com")
    key: String,

    @xmlName("V")
    @xmlNamespace(uri: "https://the-value.example.com")
    value: String,
}
