// This file defines test cases that test list query serialization.

$version: "2.0"

namespace aws.protocoltests.query

use aws.protocols#awsQuery
use aws.protocoltests.shared#EpochSeconds
use aws.protocoltests.shared#FooEnum
use aws.protocoltests.shared#GreetingList
use aws.protocoltests.shared#StringList
use smithy.test#httpRequestTests

/// This test serializes simple and complex lists.
operation QueryLists {
    input: QueryListsInput
}

apply QueryLists @httpRequestTests([
    {
        id: "QueryLists",
        documentation: "Serializes query lists",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=QueryLists&Version=2020-01-08&ListArg.member.1=foo&ListArg.member.2=bar&ListArg.member.3=baz&ComplexListArg.member.1.hi=hello&ComplexListArg.member.2.hi=hola",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            ListArg: ["foo", "bar", "baz"],
            ComplexListArg: [
                {
                    hi: "hello"
                },
                {
                    hi: "hola"
                }
            ]
        }
    },
    {
        id: "EmptyQueryLists",
        documentation: "Serializes empty query lists",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=QueryLists&Version=2020-01-08&ListArg=",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            ListArg: []
        }
    },
    {
        id: "FlattenedQueryLists",
        documentation: "Flattens query lists by repeating the member name and removing the member element",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=QueryLists&Version=2020-01-08&FlattenedListArg.1=A&FlattenedListArg.2=B",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            FlattenedListArg: ["A", "B"]
        }
    },
    {
        id: "QueryListArgWithXmlNameMember",
        documentation: "Changes the member of lists using xmlName trait",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=QueryLists&Version=2020-01-08&ListArgWithXmlNameMember.item.1=A&ListArgWithXmlNameMember.item.2=B",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            ListArgWithXmlNameMember: ["A", "B"]
        }
    },
    {
        id: "QueryFlattenedListArgWithXmlName",
        documentation: "Changes the name of flattened lists using xmlName trait on the structure member",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=QueryLists&Version=2020-01-08&Hi.1=A&Hi.2=B",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            FlattenedListArgWithXmlName: ["A", "B"]
        }
    },
    {
        id: "QueryNestedStructWithList",
        documentation: "Nested structure with a list member",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=QueryLists&Version=2020-01-08&NestedWithList.ListArg.member.1=A&NestedWithList.ListArg.member.2=B",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            NestedWithList: {
                ListArg: ["A", "B"]
            }
        }
    },
])

structure QueryListsInput {
    ListArg: StringList,
    ComplexListArg: GreetingList,

    @xmlFlattened
    FlattenedListArg: StringList,

    ListArgWithXmlNameMember: ListWithXmlName,

    // Notice that the xmlName on the targeted list member is ignored.
    @xmlFlattened
    @xmlName("Hi")
    FlattenedListArgWithXmlName: ListWithXmlName,

    NestedWithList: NestedStructWithList
}

list ListWithXmlName {
    @xmlName("item")
    member: String
}

structure NestedStructWithList {
    ListArg: StringList
}
