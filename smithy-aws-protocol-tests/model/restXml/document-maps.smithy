// This file defines test cases that serialize maps in XML payloads.

$version: "2.0"
$operationInputSuffix: "Request"
$operationOutputSuffix: "Response"

namespace aws.protocoltests.restxml

use aws.protocols#restXml
use aws.protocoltests.shared#FooEnumMap
use aws.protocoltests.shared#GreetingStruct
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// The example tests basic map serialization.
@http(uri: "/XmlMaps", method: "POST")
operation XmlMaps {
    input := {
        myMap: XmlMapsInputOutputMap
    }
    output := {
        myMap: XmlMapsInputOutputMap
    }
}

apply XmlMaps @httpRequestTests([
    {
        id: "XmlMaps",
        documentation: "Tests for XML map serialization",
        protocol: restXml,
        method: "POST",
        uri: "/XmlMaps",
        body: """
              <XmlMapsRequest>
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
              </XmlMapsRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
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

apply XmlMaps @httpResponseTests([
    {
        id: "XmlMaps",
        documentation: "Tests for XML map serialization",
        protocol: restXml,
        code: 200,
        body: """
              <XmlMapsResponse>
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
              </XmlMapsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
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

@http(uri: "/XmlEmptyMaps", method: "POST")
@tags(["client-only"])
operation XmlEmptyMaps {
    input := {
        myMap: XmlMapsInputOutputMap
    }
    output := {
        myMap: XmlMapsInputOutputMap
    }
}

apply XmlEmptyMaps @httpRequestTests([
    {
        id: "XmlEmptyMaps",
        documentation: "Serializes Empty XML maps",
        protocol: restXml,
        method: "POST",
        uri: "/XmlEmptyMaps",
        body: """
              <XmlEmptyMapsRequest>
                  <myMap></myMap>
              </XmlEmptyMapsRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            myMap: {}
        },
        appliesTo: "client"
    }
])

apply XmlEmptyMaps @httpResponseTests([
    {
        id: "XmlEmptyMaps",
        documentation: "Deserializes Empty XML maps",
        protocol: restXml,
        code: 200,
        body: """
              <XmlEmptyMapsResponse>
                  <myMap></myMap>
              </XmlEmptyMapsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            myMap: {}
        },
        appliesTo: "client",
    },
    {
        id: "XmlEmptySelfClosedMaps",
        documentation: "Deserializes Empty Self-closed XML maps",
        protocol: restXml,
        code: 200,
        body: """
              <XmlEmptyMapsResponse>
                  <myMap/>
              </XmlEmptyMapsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            myMap: {}
        },
        appliesTo: "client",
    }
])

map XmlMapsInputOutputMap {
    key: String,
    value: GreetingStruct
}

// This example tests maps with @xmlName on members.
@http(uri: "/XmlMapsXmlName", method: "POST")
operation XmlMapsXmlName {
    input := {
        myMap: XmlMapsXmlNameInputOutputMap
    }
    output := {
        myMap: XmlMapsXmlNameInputOutputMap
    }
}

apply XmlMapsXmlName @httpRequestTests([
    {
        id: "XmlMapsXmlName",
        documentation: "Serializes XML maps that have xmlName on members",
        protocol: restXml,
        method: "POST",
        uri: "/XmlMapsXmlName",
        body: """
              <XmlMapsXmlNameRequest>
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
              </XmlMapsXmlNameRequest>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
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

apply XmlMapsXmlName @httpResponseTests([
    {
        id: "XmlMapsXmlName",
        documentation: "Serializes XML lists",
        protocol: restXml,
        code: 200,
        body: """
              <XmlMapsXmlNameResponse>
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
              </XmlMapsXmlNameResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
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

map XmlMapsXmlNameInputOutputMap {
    @xmlName("Attribute")
    key: String,

    @xmlName("Setting")
    value: GreetingStruct
}

/// Flattened maps
@http(uri: "/FlattenedXmlMap", method: "POST")
operation FlattenedXmlMap {
    input := {
        @xmlFlattened
        myMap: FooEnumMap
    }
    output := {
        @xmlFlattened
        myMap: FooEnumMap
    }
}

apply FlattenedXmlMap @httpRequestTests([
    {
        id: "FlattenedXmlMap",
        documentation: "Serializes flattened XML maps in requests",
        protocol: restXml,
        method: "POST",
        uri: "/FlattenedXmlMap",
        body: """
              <FlattenedXmlMapRequest>
                  <myMap>
                      <key>foo</key>
                      <value>Foo</value>
                  </myMap>
                  <myMap>
                      <key>baz</key>
                      <value>Baz</value>
                  </myMap>
              </FlattenedXmlMapRequest>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            myMap: {
                foo: "Foo",
                baz: "Baz"
            }
        }
    }
])

apply FlattenedXmlMap @httpResponseTests([
    {
        id: "FlattenedXmlMap",
        documentation: "Serializes flattened XML maps in responses",
        protocol: restXml,
        code: 200,
        body: """
              <FlattenedXmlMapResponse>
                  <myMap>
                      <key>foo</key>
                      <value>Foo</value>
                  </myMap>
                  <myMap>
                      <key>baz</key>
                      <value>Baz</value>
                  </myMap>
              </FlattenedXmlMapResponse>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            myMap: {
                foo: "Foo",
                baz: "Baz"
            }
        }
    }
])

/// Flattened maps with @xmlName
@http(uri: "/FlattenedXmlMapWithXmlName", method: "POST")
operation FlattenedXmlMapWithXmlName {
    input := {
        @xmlFlattened
        @xmlName("KVP")
        myMap: FlattenedXmlMapWithXmlNameInputOutputMap,
    }
    output := {
        @xmlFlattened
        @xmlName("KVP")
        myMap: FlattenedXmlMapWithXmlNameInputOutputMap
    }
}

apply FlattenedXmlMapWithXmlName @httpRequestTests([
    {
        id: "FlattenedXmlMapWithXmlName",
        documentation: "Serializes flattened XML maps in requests that have xmlName on members",
        protocol: restXml,
        method: "POST",
        uri: "/FlattenedXmlMapWithXmlName",
        body: """
              <FlattenedXmlMapWithXmlNameRequest>
                  <KVP>
                      <K>a</K>
                      <V>A</V>
                  </KVP>
                  <KVP>
                      <K>b</K>
                      <V>B</V>
                  </KVP>
              </FlattenedXmlMapWithXmlNameRequest>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            myMap: {
                a: "A",
                b: "B",
            }
        }
    }
])

apply FlattenedXmlMapWithXmlName @httpResponseTests([
    {
        id: "FlattenedXmlMapWithXmlName",
        documentation: "Serializes flattened XML maps in responses that have xmlName on members",
        protocol: restXml,
        code: 200,
        body: """
              <FlattenedXmlMapWithXmlNameResponse>
                  <KVP>
                      <K>a</K>
                      <V>A</V>
                  </KVP>
                  <KVP>
                      <K>b</K>
                      <V>B</V>
                  </KVP>
              </FlattenedXmlMapWithXmlNameResponse>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            myMap: {
                a: "A",
                b: "B",
            }
        }
    }
])

map FlattenedXmlMapWithXmlNameInputOutputMap {
    @xmlName("K")
    key: String,

    @xmlName("V")
    value: String,
}

/// Flattened maps with @xmlNamespace and @xmlName
@http(uri: "/FlattenedXmlMapWithXmlNamespace", method: "POST")
operation FlattenedXmlMapWithXmlNamespace {
    output: FlattenedXmlMapWithXmlNamespaceOutput
}

apply FlattenedXmlMapWithXmlNamespace @httpResponseTests([
    {
        id: "RestXmlFlattenedXmlMapWithXmlNamespace",
        documentation: "Serializes flattened XML maps in responses that have xmlNamespace and xmlName on members",
        protocol: restXml,
        code: 200,
        body: """
              <FlattenedXmlMapWithXmlNamespaceOutput>
                  <KVP xmlns="https://the-member.example.com">
                      <K xmlns="https://the-key.example.com">a</K>
                      <V xmlns="https://the-value.example.com">A</V>
                  </KVP>
                  <KVP xmlns="https://the-member.example.com">
                      <K xmlns="https://the-key.example.com">b</K>
                      <V xmlns="https://the-value.example.com">B</V>
                  </KVP>
              </FlattenedXmlMapWithXmlNamespaceOutput>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
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

@http(uri: "/NestedXmlMaps", method: "POST")
operation NestedXmlMaps {
    input := with [NestedXmlMapsInputOutput] {}
    output := with [NestedXmlMapsInputOutput] {}
}

@mixin
structure NestedXmlMapsInputOutput {
    nestedMap: NestedMap,

    @xmlFlattened
    flatNestedMap: NestedMap,
}

map NestedMap {
    key: String,
    value: FooEnumMap,
}

apply NestedXmlMaps @httpRequestTests([
    {
        id: "NestedXmlMapRequest",
        documentation: "Tests requests with nested maps.",
        protocol: restXml,
        method: "POST",
        uri: "/NestedXmlMaps",
        body: """
            <NestedXmlMapsRequest>
                <nestedMap>
                    <entry>
                        <key>foo</key>
                        <value>
                            <entry>
                                <key>bar</key>
                                <value>Bar</value>
                            </entry>
                        </value>
                    </entry>
                </nestedMap>
            </NestedXmlMapsRequest>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
        },
        params: {
            nestedMap: {
                foo: {
                    bar: "Bar",
                }
            }
        }
    },
    {
        id: "FlatNestedXmlMapRequest",
        documentation: """
            Tests requests with nested flat maps. Since maps can only be
            flattened when they're structure members, only the outer map is flat.""",
        protocol: restXml,
        method: "POST",
        uri: "/NestedXmlMaps",
        body: """
            <NestedXmlMapsRequest>
                <flatNestedMap>
                    <key>foo</key>
                    <value>
                        <entry>
                            <key>bar</key>
                            <value>Bar</value>
                        </entry>
                    </value>
                </flatNestedMap>
            </NestedXmlMapsRequest>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
        },
        params: {
            flatNestedMap: {
                foo: {
                    bar: "Bar",
                }
            }
        }
    },
])

apply NestedXmlMaps @httpResponseTests([
    {
        id: "NestedXmlMapResponse",
        documentation: "Tests responses with nested maps.",
        protocol: restXml,
        code: 200,
        body: """
            <NestedXmlMapsResponse>
                <nestedMap>
                    <entry>
                        <key>foo</key>
                        <value>
                            <entry>
                                <key>bar</key>
                                <value>Bar</value>
                            </entry>
                        </value>
                    </entry>
                </nestedMap>
            </NestedXmlMapsResponse>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
        },
        params: {
            nestedMap: {
                foo: {
                    bar: "Bar",
                }
            }
        }
    },
    {
        id: "FlatNestedXmlMapResponse",
        documentation: """
            Tests responses with nested flat maps. Since maps can only be
            flattened when they're structure members, only the outer map is flat.""",
        protocol: restXml,
        code: 200,
        body: """
            <NestedXmlMapsResponse>
                <flatNestedMap>
                    <key>foo</key>
                    <value>
                        <entry>
                            <key>bar</key>
                            <value>Bar</value>
                        </entry>
                    </value>
                </flatNestedMap>
            </NestedXmlMapsResponse>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml",
        },
        params: {
            flatNestedMap: {
                foo: {
                    bar: "Bar",
                }
            }
        }
    },
])

/// Maps with @xmlNamespace and @xmlName
@http(uri: "/XmlMapWithXmlNamespace", method: "POST")
operation XmlMapWithXmlNamespace {
    input := with [XmlMapWithXmlNamespaceInputOutput] {}
    output := with [XmlMapWithXmlNamespaceInputOutput] {}
}

apply XmlMapWithXmlNamespace @httpRequestTests([
    {
        id: "RestXmlXmlMapWithXmlNamespace",
        documentation: "Serializes XML maps in requests that have xmlNamespace and xmlName on members",
        protocol: restXml,
        method: "POST",
        uri: "/XmlMapWithXmlNamespace",
        body: """
              <XmlMapWithXmlNamespaceRequest>
                  <KVP xmlns="https://the-member.example.com">
                      <entry>
                          <K xmlns="https://the-key.example.com">a</K>
                          <V xmlns="https://the-value.example.com">A</V>
                      </entry>
                      <entry>
                          <K xmlns="https://the-key.example.com">b</K>
                          <V xmlns="https://the-value.example.com">B</V>
                      </entry>
                  </KVP>
              </XmlMapWithXmlNamespaceRequest>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            myMap: {
                a: "A",
                b: "B",
            }
        }
    }
])

apply XmlMapWithXmlNamespace @httpResponseTests([
    {
        id: "RestXmlXmlMapWithXmlNamespace",
        documentation: "Serializes XML maps in responses that have xmlNamespace and xmlName on members",
        protocol: restXml,
        code: 200,
        body: """
              <XmlMapWithXmlNamespaceResponse>
                  <KVP xmlns="https://the-member.example.com">
                      <entry>
                          <K xmlns="https://the-key.example.com">a</K>
                          <V xmlns="https://the-value.example.com">A</V>
                      </entry>
                      <entry>
                          <K xmlns="https://the-key.example.com">b</K>
                          <V xmlns="https://the-value.example.com">B</V>
                      </entry>
                  </KVP>
              </XmlMapWithXmlNamespaceResponse>""",
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            myMap: {
                a: "A",
                b: "B",
            }
        }
    }
])

@mixin
structure XmlMapWithXmlNamespaceInputOutput {
    @xmlName("KVP")
    @xmlNamespace(uri: "https://the-member.example.com")
    myMap: XmlMapWithXmlNamespaceInputOutputMap,
}

map XmlMapWithXmlNamespaceInputOutputMap {
    @xmlName("K")
    @xmlNamespace(uri: "https://the-key.example.com")
    key: String,

    @xmlName("V")
    @xmlNamespace(uri: "https://the-value.example.com")
    value: String,
}
