// This file defines test cases that test map query serialization.
// Query maps have order, but not all languages have maps that
// guarantee insertion order and those that do may choose not to
// enforce their usage. Those languages may choose to sort entries
// by key at serialization time, and so to facilitate that strategy
// all maps in this file should be sorted by key. Alternatively,
// a language implementing these tests may choose to implement a
// query body parser that understands query maps and/or lists.

$version: "2.0"

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
        body: "Action=QueryMaps&Version=2020-01-08&MapArg.entry.1.key=bar&MapArg.entry.1.value=Bar&MapArg.entry.2.key=foo&MapArg.entry.2.value=Foo",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            MapArg: {
                bar: "Bar",
                foo: "Foo",
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
        body: "Action=QueryMaps&Version=2020-01-08&Foo.entry.1.key=foo&Foo.entry.1.value=Foo",
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
        body: "Action=QueryMaps&Version=2020-01-08&ComplexMapArg.entry.1.key=bar&ComplexMapArg.entry.1.value.hi=Bar&ComplexMapArg.entry.2.key=foo&ComplexMapArg.entry.2.value.hi=Foo",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            ComplexMapArg: {
                bar: {
                    hi: "Bar"
                },
                foo: {
                    hi: "Foo",
                },
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
        body: "Action=QueryMaps&Version=2020-01-08",
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
        body: "Action=QueryMaps&Version=2020-01-08&MapWithXmlMemberName.entry.1.K=bar&MapWithXmlMemberName.entry.1.V=Bar&MapWithXmlMemberName.entry.2.K=foo&MapWithXmlMemberName.entry.2.V=Foo",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            MapWithXmlMemberName: {
                bar: "Bar",
                foo: "Foo",
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
        body: "Action=QueryMaps&Version=2020-01-08&FlattenedMap.1.key=bar&FlattenedMap.1.value=Bar&FlattenedMap.2.key=foo&FlattenedMap.2.value=Foo",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            FlattenedMap: {
                bar: "Bar",
                foo: "Foo",
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
        body: "Action=QueryMaps&Version=2020-01-08&Hi.1.K=bar&Hi.1.V=Bar&Hi.2.K=foo&Hi.2.V=Foo",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            FlattenedMapWithXmlName: {
                bar: "Bar",
                foo: "Foo",
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
        body: "Action=QueryMaps&Version=2020-01-08&MapOfLists.entry.1.key=bar&MapOfLists.entry.1.value.member.1=C&MapOfLists.entry.1.value.member.2=D&MapOfLists.entry.2.key=foo&MapOfLists.entry.2.value.member.1=A&MapOfLists.entry.2.value.member.2=B",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            MapOfLists: {
                bar: ["C", "D"],
                foo: ["A", "B"],
            }
        }
    },
    {
        id: "QueryNestedStructWithMap",
        documentation: "Serializes nested struct with map member",
        protocol: awsQuery,
        method: "POST",
        uri: "/",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: "Action=QueryMaps&Version=2020-01-08&NestedStructWithMap.MapArg.entry.1.key=bar&NestedStructWithMap.MapArg.entry.1.value=Bar&NestedStructWithMap.MapArg.entry.2.key=foo&NestedStructWithMap.MapArg.entry.2.value=Foo",
        bodyMediaType: "application/x-www-form-urlencoded",
        params: {
            NestedStructWithMap: {
                MapArg: {
                    bar: "Bar",
                    foo: "Foo",
                }
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

    NestedStructWithMap: NestedStructWithMap,
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

structure NestedStructWithMap {
    MapArg: StringMap
}
