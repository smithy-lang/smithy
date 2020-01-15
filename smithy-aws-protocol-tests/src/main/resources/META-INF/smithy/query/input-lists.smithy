// This file defines test cases that test list query serialization.

$version: "0.5.0"

namespace aws.protocols.tests.query

use aws.protocols.tests.shared#EpochSeconds
use aws.protocols.tests.shared#FooEnum
use aws.protocols.tests.shared#GreetingList
use aws.protocols.tests.shared#StringList
use smithy.test#httpRequestTests

/// This test serializes simple and complex lists.
operation QueryLists {
    input: QueryListsInput
}

apply QueryLists @httpRequestTests([
    {
        id: "QueryLists",
        description: "Serializes query lists",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryLists
              &Version=2020-01-08
              &ListArg.member.1=foo
              &ListArg.member.2=bar
              &ListArg.member.3=baz
              &ComplexListArg.member.1.hi=hello
              &ComplexListArg.member.2.hi=hola""",
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
        description: "Does not serialize empty query lists",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryLists
              &Version=2020-01-08""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            ListArg: []
        }
    },
    {
        id: "FlattenedQueryLists",
        description: "Flattens query lists by repeating the member name and removing the member element",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryLists
              &Version=2020-01-08
              &FlattenedListArg.1=A
              &FlattenedListArg.2=B""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            FlattenedListArg: ["A", "B"]
        }
    },
    {
        id: "QueryListArgWithXmlNameMember",
        description: "Changes the member of lists using xmlName trait",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryLists
              &Version=2020-01-08
              &ListArgWithXmlNameMember.item.1=A
              &ListArgWithXmlNameMember.item.2=B""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            ListArgWithXmlNameMember: ["A", "B"]
        }
    },
    {
        id: "QueryFlattenedListArgWithXmlName",
        description: "Changes the name of flattened lists using xmlName trait on the structure member",
        protocol: "aws.query",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryLists
              &Version=2020-01-08
              &Hi.1=A
              &Hi.2=B""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            FlattenedListArgWithXmlName: ["A", "B"]
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
}

list ListWithXmlName {
    @xmlName("item")
    member: String
}
