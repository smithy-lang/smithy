$version: "2.0"

namespace aws.protocoltests.restxml.xmlns

use aws.api#service
use aws.auth#sigv4
use aws.protocols#restXml
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// A REST XML service that sends XML requests and responses.
///
/// This service and test case is complementary to the test cases
/// in the `restXml` directory, but the service under test here has
/// the `xmlNamespace` trait applied to it.
///
/// See https://github.com/awslabs/smithy/issues/616
@service(sdkId: "Rest Xml Protocol Namespace")
@sigv4(name: "restxmlwithnamespace")
@xmlNamespace(uri: "https://example.com")
@restXml
@title("Sample Rest Xml Protocol Service With Namespace")
service RestXmlWithNamespace {
    version: "2019-12-16",
    operations: [SimpleScalarProperties]
}

// This example serializes simple scalar types in the top level XML document.
// Note that headers are not serialized in the payload.
//
// This is a partial copy of aws.protocoltests.restxml#SimpleScalarProperties,
// but only includes enough test cases to ensure a namespace is serialized.
@idempotent
@http(uri: "/SimpleScalarProperties", method: "PUT")
operation SimpleScalarProperties {
    input: SimpleScalarPropertiesInputOutput,
    output: SimpleScalarPropertiesInputOutput
}

apply SimpleScalarProperties @httpRequestTests([
    {
        id: "XmlNamespaceSimpleScalarProperties",
        documentation: "Serializes simple scalar properties",
        protocol: restXml,
        method: "PUT",
        uri: "/SimpleScalarProperties",
        body: """
              <SimpleScalarPropertiesInputOutput xmlns="https://example.com">
                  <stringValue>string</stringValue>
                  <trueBooleanValue>true</trueBooleanValue>
                  <falseBooleanValue>false</falseBooleanValue>
                  <byteValue>1</byteValue>
                  <shortValue>2</shortValue>
                  <integerValue>3</integerValue>
                  <longValue>4</longValue>
                  <floatValue>5.5</floatValue>
                  <DoubleDribble>6.5</DoubleDribble>
                  <Nested xmlns:xsi="https://example.com" xsi:someName="nestedAttrValue"></Nested>
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
            Nested: {
                attrField: "nestedAttrValue",
            },
        }
    }
])

apply SimpleScalarProperties @httpResponseTests([
    {
        id: "XmlNamespaceSimpleScalarProperties",
        documentation: "Serializes simple scalar properties",
        protocol: restXml,
        code: 200,
        body: """
              <SimpleScalarPropertiesInputOutput xmlns="https://example.com">
                  <stringValue>string</stringValue>
                  <trueBooleanValue>true</trueBooleanValue>
                  <falseBooleanValue>false</falseBooleanValue>
                  <byteValue>1</byteValue>
                  <shortValue>2</shortValue>
                  <integerValue>3</integerValue>
                  <longValue>4</longValue>
                  <floatValue>5.5</floatValue>
                  <DoubleDribble>6.5</DoubleDribble>
                  <Nested xmlns:xsi="https://example.com" xsi:someName="nestedAttrValue"></Nested>
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
            Nested: {
                attrField: "nestedAttrValue",
            },
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

    @xmlNamespace(prefix: "xsi", uri: "https://example.com")
    Nested: NestedWithNamespace,

    @xmlName("DoubleDribble")
    doubleValue: Double,
}

structure NestedWithNamespace {
    @xmlAttribute
    @xmlName("xsi:someName")
    attrField: String,
}
