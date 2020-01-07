// This file defines test cases that serialize lists in XML documents.

$version: "0.5.0"

namespace aws.protocols.tests.restxml

use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This test case serializes XML lists for the following cases for both
/// input and output:
///
/// 1. Normal XML lists.
/// 2. Normal XML sets.
/// 3. XML lists of lists.
/// 4. XML lists with @xmlName on its members
/// 5. Flattened XML lists.
/// 6. Flattened XML lists with @xmlName.
/// 7. Lists of structures.
@idempotent
@http(uri: "/XmlLists", method: "PUT")
operation XmlLists(XmlListsInputOutput) -> XmlListsInputOutput

apply XmlLists @httpRequestTests([
    {
        id: "XmlLists",
        description: "Serializes XML lists",
        protocol: "aws.rest-xml",
        method: "PUT",
        uri: "/XmlLists",
        body: """
              <XmlListsInputOutput>
                  <stringList>
                      <member>foo</member>
                      <member>bar</member>
                  </stringList>
                  <stringSet>
                      <member>foo</member>
                      <member>bar</member>
                  </stringSet>
                  <integerList>
                      <member>1</member>
                      <member>2</member>
                  </integerList>
                  <booleanList>
                      <member>true</member>
                      <member>false</member>
                  </booleanList>
                  <timestampList>
                      <member>2014-04-29T18:30:38Z</member>
                      <member>2014-04-29T18:30:38Z</member>
                  </timestampList>
                  <enumList>
                      <member>Foo</member>
                      <member>0</member>
                  </enumList>
                  <nestedStringList>
                      <member>
                          <member>foo</member>
                          <member>bar</member>
                      </member>
                      <member>
                          <member>baz</member>
                          <member>qux</member>
                      </member>
                  <nestedStringList>
                  <renamed>
                      <item>foo</item>
                      <item>bar</item>
                  </renamed>
                  <flattenedList>hi</flattenedList>
                  <flattenedList>bye</flattenedList>
                  <customName>yep</customName>
                  <customName>nope</customName>
                  <myStructureList>
                      <item>
                          <value>1</value>
                          <other>2</other>
                      </item>
                      <item>
                          <value>3</value>
                          <other>4</other>
                      </item>
                  </myStructureList>
              </XmlListsInputOutput>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            stringList: ["foo", "bar"],
            stringSet: ["foo", "bar"],
            integerList: [1, 2],
            booleanList: [true, false],
            timestampList: [1398796238, 1398796238],
            enumList: ["Foo", "0"],
            nestedStringList: [["foo", "bar"], ["baz", "qux"]],
            renamedListMembers: ["foo", "bar"],
            flattenedList: ["hi", "bye"],
            flattenedList2: ["yep", "nope"],
            structureList: [
                {
                    a: "1",
                    b: "2",
                },
                {
                    a: "3",
                    b: "4",
                }
            ]
        }
    }
])

apply XmlLists @httpResponseTests([
    {
        id: "XmlLists",
        description: "Serializes XML lists",
        protocol: "aws.rest-xml",
        code: 200,
        body: """
              <XmlListsInputOutput>
                  <stringList>
                      <member>foo</member>
                      <member>bar</member>
                  </stringList>
                  <stringSet>
                      <member>foo</member>
                      <member>bar</member>
                  </stringSet>
                  <integerList>
                      <member>1</member>
                      <member>2</member>
                  </integerList>
                  <booleanList>
                      <member>true</member>
                      <member>false</member>
                  </booleanList>
                  <timestampList>
                      <member>2014-04-29T18:30:38Z</member>
                      <member>2014-04-29T18:30:38Z</member>
                  </timestampList>
                  <enumList>
                      <member>Foo</member>
                      <member>0</member>
                  </enumList>
                  <nestedStringList>
                      <member>
                          <member>foo</member>
                          <member>bar</member>
                      </member>
                      <member>
                          <member>baz</member>
                          <member>qux</member>
                      </member>
                  <nestedStringList>
                  <renamed>
                      <item>foo</item>
                      <item>bar</item>
                  </renamed>
                  <flattenedList>hi</flattenedList>
                  <flattenedList>bye</flattenedList>
                  <customName>yep</customName>
                  <customName>nope</customName>
                  <myStructureList>
                      <item>
                          <value>1</value>
                          <other>2</other>
                      </item>
                      <item>
                          <value>3</value>
                          <other>4</other>
                      </item>
                  </myStructureList>
              </XmlListsInputOutput>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "application/xml"
        },
        params: {
            stringList: ["foo", "bar"],
            stringSet: ["foo", "bar"],
            integerList: [1, 2],
            booleanList: [true, false],
            timestampList: [1398796238, 1398796238],
            enumList: ["Foo", "0"],
            nestedStringList: [["foo", "bar"], ["baz", "qux"]],
            renamedListMembers: ["foo", "bar"],
            flattenedList: ["hi", "bye"],
            flattenedList2: ["yep", "nope"],
            structureList: [
                {
                    a: "1",
                    b: "2",
                },
                {
                    a: "3",
                    b: "4",
                }
            ]
        }
    }
])

structure XmlListsInputOutput {
    stringList: StringList,

    stringSet: StringSet,

    integerList: IntegerList,

    booleanList: BooleanList,

    timestampList: TimestampList,

    enumList: FooEnumList,

    nestedStringList: NestedStringList,

    @xmlName("renamed")
    renamedListMembers: RenamedListMembers,

    @xmlFlattened
    // The xmlname on the targeted list is ignored, and the member name is used.
    flattenedList: RenamedListMembers,

    @xmlName("customName")
    @xmlFlattened
    // the xmlName trait on the targeted list's member is ignored when
    // serializing flattened lists in structures.
    flattenedList2: RenamedListMembers,

    @xmlName("myStructureList")
    structureList: StructureList
}

list RenamedListMembers {
    @xmlName("item")
    member: String,
}

list StructureList {
    @xmlName("item")
    member: StructureListMember,
}

structure StructureListMember {
    @xmlName("value")
    a: String,

    @xmlName("other")
    b: String,
}
