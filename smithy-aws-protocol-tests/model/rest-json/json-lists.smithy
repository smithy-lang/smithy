// This file defines test cases that serialize lists in JSON documents.

$version: "0.5.0"

namespace aws.protocols.tests.restjson

use aws.protocols.tests.shared#BooleanList
use aws.protocols.tests.shared#EpochSeconds
use aws.protocols.tests.shared#FooEnumList
use aws.protocols.tests.shared#GreetingList
use aws.protocols.tests.shared#IntegerList
use aws.protocols.tests.shared#NestedStringList
use aws.protocols.tests.shared#StringList
use aws.protocols.tests.shared#StringSet
use aws.protocols.tests.shared#TimestampList
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
        protocol: "aws.rest-json-1.1",
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
        protocol: "aws.rest-json-1.1",
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
    },
    {
          id: "RestJsonListsSerializeNull",
          documentation: "Serializes null values in lists",
          protocol: "aws.rest-json-1.1",
          method: "PUT",
          uri: "/JsonLists",
          body: """
                {
                    "stringList": [
                        null
                    ]
                }""",
          bodyMediaType: "application/json",
          headers: {"Content-Type": "application/json"},
          params: {
              stringList: [null]
          }
    }
])

apply JsonLists @httpResponseTests([
    {
        id: "RestJsonLists",
        documentation: "Serializes JSON lists",
        protocol: "aws.rest-json-1.1",
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
        protocol: "aws.rest-json-1.1",
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
    },
    {
          id: "RestJsonListsSerializeNull",
          documentation: "Serializes null values in lists",
          protocol: "aws.rest-json-1.1",
          code: 200,
          body: """
                {
                    "stringList": [
                        null
                    ]
                }""",
          bodyMediaType: "application/json",
          headers: {"Content-Type": "application/json"},
          params: {
              stringList: [null]
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
