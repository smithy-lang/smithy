// This file defines test cases that serialize lists in JSON documents.

$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use aws.protocoltests.shared#BooleanList
use aws.protocoltests.shared#EpochSeconds
use aws.protocoltests.shared#FooEnumList
use aws.protocoltests.shared#IntegerEnumList
use aws.protocoltests.shared#GreetingList
use aws.protocoltests.shared#IntegerList
use aws.protocoltests.shared#NestedStringList
use aws.protocoltests.shared#SparseShortList
use aws.protocoltests.shared#StringList
use aws.protocoltests.shared#SparseStringList
use aws.protocoltests.shared#StringSet
use aws.protocoltests.shared#TimestampList
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

/// This test case serializes JSON lists for the following cases for both
/// input and output:
///
/// 1. Normal JSON lists.
/// 2. Normal JSON sets.
/// 3. JSON lists of lists.
/// 4. Lists of structures.
@idempotent
@http(uri: "/JsonLists", method: "PUT")
operation JsonLists {
    input: JsonListsInputOutput,
    output: JsonListsInputOutput
}

apply JsonLists @httpRequestTests([
    {
        id: "RestJsonLists",
        documentation: "Serializes JSON lists",
        protocol: restJson1,
        method: "PUT",
        uri: "/JsonLists",
        body: """
              {
                  "stringList": [
                      "foo",
                      "bar"
                  ],
                  "stringSet": [
                      "foo",
                      "bar"
                  ],
                  "integerList": [
                      1,
                      2
                  ],
                  "booleanList": [
                      true,
                      false
                  ],
                  "timestampList": [
                      1398796238,
                      1398796238
                  ],
                  "enumList": [
                      "Foo",
                      "0"
                  ],
                  "intEnumList": [
                      1,
                      2
                  ],
                  "nestedStringList": [
                      [
                          "foo",
                          "bar"
                      ],
                      [
                          "baz",
                          "qux"
                      ]
                  ],
                  "myStructureList": [
                      {
                          "value": "1",
                          "other": "2"
                      },
                      {
                          "value": "3",
                          "other": "4"
                      }
                  ]
              }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            "stringList": [
                "foo",
                "bar"
            ],
            "stringSet": [
                "foo",
                "bar"
            ],
            "integerList": [
                1,
                2
            ],
            "booleanList": [
                true,
                false
            ],
            "timestampList": [
                1398796238,
                1398796238
            ],
            "enumList": [
                "Foo",
                "0"
            ],
            "intEnumList": [
                1,
                2
            ],
            "nestedStringList": [
                [
                    "foo",
                    "bar"
                ],
                [
                    "baz",
                    "qux"
                ]
            ],
            "structureList": [
                {
                    "a": "1",
                    "b": "2"
                },
                {
                    "a": "3",
                    "b": "4"
                }
            ]
        }
    },
    {
        id: "RestJsonListsEmpty",
        documentation: "Serializes empty JSON lists",
        protocol: restJson1,
        method: "PUT",
        uri: "/JsonLists",
        body: """
              {
                  "stringList": []
              }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            stringList: []
        }
    }
])

apply JsonLists @httpResponseTests([
    {
        id: "RestJsonLists",
        documentation: "Serializes JSON lists",
        protocol: restJson1,
        code: 200,
        body: """
              {
                  "stringList": [
                      "foo",
                      "bar"
                  ],
                  "stringSet": [
                      "foo",
                      "bar"
                  ],
                  "integerList": [
                      1,
                      2
                  ],
                  "booleanList": [
                      true,
                      false
                  ],
                  "timestampList": [
                      1398796238,
                      1398796238
                  ],
                  "enumList": [
                      "Foo",
                      "0"
                  ],
                  "intEnumList": [
                      1,
                      2
                  ],
                  "nestedStringList": [
                      [
                          "foo",
                          "bar"
                      ],
                      [
                          "baz",
                          "qux"
                      ]
                  ],
                  "myStructureList": [
                      {
                          "value": "1",
                          "other": "2"
                      },
                      {
                          "value": "3",
                          "other": "4"
                      }
                  ]
              }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            "stringList": [
                "foo",
                "bar"
            ],
            "stringSet": [
                "foo",
                "bar"
            ],
            "integerList": [
                1,
                2
            ],
            "booleanList": [
                true,
                false
            ],
            "timestampList": [
                1398796238,
                1398796238
            ],
            "enumList": [
                "Foo",
                "0"
            ],
            "intEnumList": [
                1,
                2
            ],
            "nestedStringList": [
                [
                    "foo",
                    "bar"
                ],
                [
                    "baz",
                    "qux"
                ]
            ],
            "structureList": [
                {
                    "a": "1",
                    "b": "2"
                },
                {
                    "a": "3",
                    "b": "4"
                }
            ]
        }
    },
    {
        id: "RestJsonListsEmpty",
        documentation: "Serializes empty JSON lists",
        protocol: restJson1,
        code: 200,
        body: """
              {
                  "stringList": []
              }""",
        bodyMediaType: "application/json",
        headers: {"Content-Type": "application/json"},
        params: {
            stringList: []
        }
    }
])

structure JsonListsInputOutput {
    stringList: StringList,

    stringSet: StringSet,

    integerList: IntegerList,

    booleanList: BooleanList,

    timestampList: TimestampList,

    enumList: FooEnumList,

    intEnumList: IntegerEnumList,

    nestedStringList: NestedStringList,

    @jsonName("myStructureList")
    structureList: StructureList
}

list StructureList {
    member: StructureListMember,
}

structure StructureListMember {
    @jsonName("value")
    a: String,

    @jsonName("other")
    b: String,
}

@httpRequestTests([
    {
        id: "RestJsonSparseListsSerializeNull"
        documentation: "Serializes null values in sparse lists"
        protocol: restJson1
        method: "PUT"
        uri: "/SparseJsonLists"
        body: """
                {
                    "sparseStringList": [
                        null,
                        "hi"
                    ],
                    "sparseShortList": [
                        null,
                        2
                    ]
                }"""
        bodyMediaType: "application/json"
        headers: {"Content-Type": "application/json"}
        params: {
            sparseStringList: [null, "hi"],
            sparseShortList: [null, 2]
        }
    }
])
@httpResponseTests([
    {
        id: "RestJsonSparseListsSerializeNull"
        documentation: "Serializes null values in sparse lists"
        protocol: restJson1
        code: 200
        body: """
                {
                    "sparseStringList": [
                        null,
                        "hi"
                    ],
                    "sparseShortList": [
                        null,
                        2
                    ]
                }"""
        bodyMediaType: "application/json"
        headers: {"Content-Type": "application/json"}
        params: {
            sparseStringList: [null, "hi"],
            sparseShortList: [null, 2]
        }
    }
])
@idempotent
@http(uri: "/SparseJsonLists", method: "PUT")
operation SparseJsonLists {
    input: SparseJsonListsInputOutput
    output: SparseJsonListsInputOutput
}

structure SparseJsonListsInputOutput {
    sparseStringList: SparseStringList
    sparseShortList: SparseShortList
}
