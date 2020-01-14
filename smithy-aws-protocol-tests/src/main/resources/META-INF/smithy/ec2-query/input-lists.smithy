// This file defines test cases that test list query serialization.

$version: "0.5.0"

namespace aws.protocols.tests.ec2

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
        id: "Ec2Lists",
        description: "Serializes query lists. All EC2 lists are flattened.",
        protocol: "aws.ec2",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryLists
              &Version=2020-01-08
              &ListArg.1=foo
              &ListArg.2=bar
              &ListArg.3=baz
              &ComplexListArg.1.hi=hello
              &ComplexListArg.2.hi=hola""",
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
        id: "Ec2EmptyQueryLists",
        description: "Does not serialize empty query lists",
        protocol: "aws.ec2",
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
        id: "Ec2ListArgWithXmlNameMember",
        description: "An xmlName trait in the member of a list has no effect on the list serialization.",
        protocol: "aws.ec2",
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryLists
              &Version=2020-01-08
              &ListArgWithXmlNameMember.1=A
              &ListArgWithXmlNameMember.2=B""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            ListArgWithXmlNameMember: ["A", "B"]
        }
    },
    {
        id: "Ec2ListMemberWithXmlName",
        description: "Changes the name of the list using the xmlName trait",
        protocol: "aws.ec2",
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
            ListArgWithXmlName: ["A", "B"]
        }
    },
])

structure QueryListsInput {
    ListArg: StringList,
    ComplexListArg: GreetingList,

    // Notice that the xmlName on the targeted list member is ignored.
    ListArgWithXmlNameMember: ListWithXmlName,

    @xmlName("Hi")
    ListArgWithXmlName: ListWithXmlName,
}

list ListWithXmlName {
    @xmlName("item")
    member: String
}
