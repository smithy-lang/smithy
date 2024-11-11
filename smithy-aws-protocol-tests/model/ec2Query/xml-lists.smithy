// This file defines test cases that serialize lists in XML documents.

$version: "2.0"

namespace aws.protocoltests.ec2

use aws.protocols#ec2Query
use aws.protocoltests.shared#BooleanList
use aws.protocoltests.shared#FooEnumList
use aws.protocoltests.shared#IntegerEnumList
use aws.protocoltests.shared#IntegerList
use aws.protocoltests.shared#NestedStringList
use aws.protocoltests.shared#StringList
use aws.protocoltests.shared#StringSet
use aws.protocoltests.shared#TimestampList
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
/// 7. Flattened XML lists with @xmlNamespace.
/// 8. Lists of structures.
operation XmlLists {
    output: XmlListsOutput
}

apply XmlLists @httpResponseTests([
    {
        id: "Ec2XmlLists",
        documentation: "Tests for XML list serialization",
        protocol: ec2Query,
        code: 200,
        body: """
              <XmlListsResponse xmlns="https://example.com/">
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
                  <intEnumList>
                      <member>1</member>
                      <member>2</member>
                  </intEnumList>
                  <nestedStringList>
                      <member>
                          <member>foo</member>
                          <member>bar</member>
                      </member>
                      <member>
                          <member>baz</member>
                          <member>qux</member>
                      </member>
                  </nestedStringList>
                  <renamed>
                      <item>foo</item>
                      <item>bar</item>
                  </renamed>
                  <flattenedList>hi</flattenedList>
                  <flattenedList>bye</flattenedList>
                  <customName>yep</customName>
                  <customName>nope</customName>
                  <flattenedListWithMemberNamespace xmlns="https://xml-member.example.com">a</flattenedListWithMemberNamespace>
                  <flattenedListWithMemberNamespace xmlns="https://xml-member.example.com">b</flattenedListWithMemberNamespace>
                  <flattenedListWithNamespace>a</flattenedListWithNamespace>
                  <flattenedListWithNamespace>b</flattenedListWithNamespace>
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
                  <requestId>requestid</requestId>
              </XmlListsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml;charset=UTF-8"
        },
        params: {
            stringList: ["foo", "bar"],
            stringSet: ["foo", "bar"],
            integerList: [1, 2],
            booleanList: [true, false],
            timestampList: [1398796238, 1398796238],
            enumList: ["Foo", "0"],
            intEnumList: [1, 2],
            nestedStringList: [["foo", "bar"], ["baz", "qux"]],
            renamedListMembers: ["foo", "bar"],
            flattenedList: ["hi", "bye"],
            flattenedList2: ["yep", "nope"],
            flattenedListWithMemberNamespace: ["a", "b"],
            flattenedListWithNamespace: ["a", "b"],
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

// Operation for client only
@tags(["client-only"])
operation XmlEmptyLists {
    output: XmlListsOutput
}

apply XmlEmptyLists @httpResponseTests([
    {
        id: "Ec2XmlEmptyLists",
        documentation: "Deserializes empty XML lists",
        protocol: ec2Query,
        code: 200,
        body: """
              <XmlEmptyListsResponse xmlns="https://example.com/">
                <stringList/>
                <stringSet></stringSet>
              </XmlEmptyListsResponse>
              """,
        bodyMediaType: "application/xml",
        headers: {
            "Content-Type": "text/xml"
        },
        params: {
            stringList: [],
            stringSet: [],
        },
        appliesTo: "client",
    }
])

structure XmlListsOutput {
    stringList: StringList,

    stringSet: StringSet,

    integerList: IntegerList,

    booleanList: BooleanList,

    timestampList: TimestampList,

    enumList: FooEnumList,

    intEnumList :IntegerEnumList,

    nestedStringList: NestedStringList,

    @xmlName("renamed")
    renamedListMembers: RenamedListMembers,

    @suppress(["XmlFlattenedTrait"])
    @xmlFlattened
    // The xmlname on the targeted list is ignored, and the member name is used.
    // The validation that flags this is suppressed so we can test this behavior.
    flattenedList: RenamedListMembers,

    @xmlName("customName")
    @xmlFlattened
    // the xmlName trait on the targeted list's member is ignored when
    // serializing flattened lists in structures.
    flattenedList2: RenamedListMembers,

    // The XML namespace of the flattened list's member is used, and
    // list's XML namespace is disregarded.
    @xmlFlattened
    flattenedListWithMemberNamespace: ListWithMemberNamespace,

    // Again, the XML namespace of the flattened list is ignored.
    // The namespace of the member is used, which is empty, so
    // no xmlns attribute appears on the serialized XML.
    @xmlFlattened
    flattenedListWithNamespace: ListWithNamespace,

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

@xmlNamespace(uri: "https://xml-list.example.com")
list ListWithMemberNamespace {
    @xmlNamespace(uri: "https://xml-member.example.com")
    member: String,
}

@xmlNamespace(uri: "https://xml-list.example.com")
list ListWithNamespace {
    member: String,
}
