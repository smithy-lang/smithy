// This file defines test cases that test list query serialization.

$version: "2.0"

namespace aws.protocoltests.ec2

use aws.protocols#ec2Query
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
        id: "Ec2Lists",
        documentation: "Serializes query lists. All EC2 lists are flattened.",
        protocol: ec2Query,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=QueryLists&Version=2020-01-08&ListArg.1=foo&ListArg.2=bar&ListArg.3=baz&ComplexListArg.1.Hi=hello&ComplexListArg.2.Hi=hola",
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
        // Empty lists are NOT serialized for the EC2 query protocol, which differs from the AWS query protocol.
        // See "EmptyQueryLists" for the AWS query protocol equivalent test case.
        documentation: "Does not serialize empty query lists.",
        protocol: ec2Query,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=QueryLists&Version=2020-01-08",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            ListArg: []
        }
    },
    {
        id: "Ec2ListArgWithXmlNameMember",
        documentation: "An xmlName trait in the member of a list has no effect on the list serialization.",
        protocol: ec2Query,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=QueryLists&Version=2020-01-08&ListArgWithXmlNameMember.1=A&ListArgWithXmlNameMember.2=B",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            ListArgWithXmlNameMember: ["A", "B"]
        }
    },
    {
        id: "Ec2ListMemberWithXmlName",
        documentation: "Changes the name of the list using the xmlName trait",
        protocol: ec2Query,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=QueryLists&Version=2020-01-08&Hi.1=A&Hi.2=B",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            ListArgWithXmlName: ["A", "B"]
        }
    },
    {
        id: "Ec2ListNestedStructWithList",
        documentation: "Nested structure with a list member",
        protocol: ec2Query,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=QueryLists&Version=2020-01-08&NestedWithList.ListArg.1=A&NestedWithList.ListArg.2=B",
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

    // Notice that the xmlName on the targeted list member is ignored.
    ListArgWithXmlNameMember: ListWithXmlName,

    @xmlName("Hi")
    ListArgWithXmlName: ListWithXmlName,

    NestedWithList: NestedStructWithList,
}

list ListWithXmlName {
    @xmlName("item")
    member: String
}

structure NestedStructWithList {
    ListArg: StringList
}
