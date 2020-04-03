// This file defines test cases that test map query serialization.

$version: "1.0.0"

namespace aws.protocoltests.query

use aws.protocols#awsQuery
use aws.protocoltests.shared#GreetingStruct
use aws.protocoltests.shared#StringList
use aws.protocoltests.shared#StringMap
use smithy.test#httpRequestTests

/// This test serializes simple and complex maps.
operation QueryMaps {
    input: QueryMapsInput
}

apply QueryMaps @httpRequestTests([
    {
        id: "QuerySimpleQueryMaps",
        documentation: "Serializes query maps",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryLists
              &Version=2020-01-08
              &MapArg.entry.1.key=foo
              &MapArg.entry.1.value=Foo
              &MapArg.entry.2.key=bar
              &MapArg.entry.2.value=Bar""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            MapArg: {
                foo: "Foo",
                bar: "Bar"
            }
        }
    },
    {
        id: "QuerySimpleQueryMapsWithXmlName",
        documentation: "Serializes query maps and uses xmlName",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryLists
              &Version=2020-01-08
              &Foo.entry.1.key=foo
              &Foo.entry.1.value=Foo""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            RenamedMapArg: {
                foo: "Foo"
            }
        }
    },
    {
        id: "QueryComplexQueryMaps",
        documentation: "Serializes complex query maps",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryLists
              &Version=2020-01-08
              &ComplexMapArg.entry.1.key=foo
              &ComplexMapArg.entry.1.value.hi=Foo
              &ComplexMapArg.entry.2.key=bar
              &ComplexMapArg.entry.2.value.hi=Bar""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            ComplexMapArg: {
                foo: {
                    hi: "Foo",
                },
                bar: {
                    hi: "Bar"
                }
            }
        }
    },
    {
        id: "QueryEmptyQueryMaps",
        documentation: "Does not serialize empty query maps",
        protocol: awsQuery,
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
            MapArg: {}
        }
    },
    {
        id: "QueryQueryMapWithMemberXmlName",
        documentation: "Serializes query maps where the member has an xmlName trait",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryLists
              &Version=2020-01-08
              &MapWithXmlMemberName.entry.1.K=foo
              &MapWithXmlMemberName.entry.1.V=Foo
              &MapWithXmlMemberName.entry.1.K=bar
              &MapWithXmlMemberName.entry.1.V=Bar""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            MapWithXmlMemberName: {
                foo: "Foo",
                bar: "Bar"
            }
        }
    },
    {
        id: "QueryFlattenedQueryMaps",
        documentation: "Serializes flattened query maps",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryLists
              &Version=2020-01-08
              &FlattenedMap.1.key=foo
              &FlattenedMap.1.value=Foo
              &FlattenedMap.1.key=bar
              &FlattenedMap.1.value=Bar""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            FlattenedMap: {
                foo: "Foo",
                bar: "Bar"
            }
        }
    },
    {
        id: "QueryFlattenedQueryMapsWithXmlName",
        documentation: "Serializes flattened query maps that use an xmlName",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryLists
              &Version=2020-01-08
              &Hi.1.K=foo
              &Hi.1.V=Foo
              &Hi.2.K=bar
              &Hi.2.V=Bar""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            FlattenedMapWithXmlName: {
                foo: "Foo",
                bar: "Bar"
            }
        }
    },
    {
        id: "QueryQueryMapOfLists",
        documentation: "Serializes query map of lists",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: """
              Action=QueryLists
              &Version=2020-01-08
              &MapOfLists.entry.1.key.1=A
              &MapOfLists.entry.1.key.2=B
              &MapOfLists.entry.2.key.1=C
              &MapOfLists.entry.2.key.2=D""",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            MapOfLists: {
                foo: ["A", "B"],
                bar: ["C", "D"],
            }
        }
    },
])

structure QueryMapsInput {
    MapArg: StringMap,

    @xmlName("Foo")
    RenamedMapArg: StringMap,

    ComplexMapArg: ComplexMap,

    MapWithXmlMemberName: MapWithXmlName,

    @xmlFlattened
    FlattenedMap: StringMap,

    @xmlFlattened
    @xmlName("Hi")
    FlattenedMapWithXmlName: MapWithXmlName,

    MapOfLists: MapOfLists,
}

map ComplexMap {
    key: String,
    value: GreetingStruct,
}

map MapWithXmlName {
    @xmlName("K")
    key: String,

    @xmlName("V")
    value: String
}

map MapOfLists {
    key: String,
    value: StringList,
}
