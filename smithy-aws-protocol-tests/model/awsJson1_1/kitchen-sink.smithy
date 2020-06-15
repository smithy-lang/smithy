$version: "1.0"

namespace aws.protocoltests.json

use aws.protocols#awsJson1_1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@httpRequestTests([
    {
        id: "serializes_string_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes string shapes",
        body: "{\"String\":\"abc xyz\"}",
        bodyMediaType: "application/json",
        params: {
            String: "abc xyz",
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_string_shapes_with_jsonvalue_trait",
        protocol: awsJson1_1,
        documentation: "Serializes string shapes with jsonvalue trait",
        body: "{\"JsonValue\":\"{\\\"string\\\":\\\"value\\\",\\\"number\\\":1234.5,\\\"boolTrue\\\":true,\\\"boolFalse\\\":false,\\\"array\\\":[1,2,3,4],\\\"object\\\":{\\\"key\\\":\\\"value\\\"},\\\"null\\\":null}\"}",
        bodyMediaType: "application/json",
        params: {
            JsonValue: "{\"string\":\"value\",\"number\":1234.5,\"boolTrue\":true,\"boolFalse\":false,\"array\":[1,2,3,4],\"object\":{\"key\":\"value\"},\"null\":null}",
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_integer_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes integer shapes",
        body: "{\"Integer\":1234}",
        bodyMediaType: "application/json",
        params: {
            Integer: 1234,
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_long_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes long shapes",
        body: "{\"Long\":999999999999}",
        bodyMediaType: "application/json",
        params: {
            Long: 999999999999,
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_float_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes float shapes",
        body: "{\"Float\":1234.5}",
        bodyMediaType: "application/json",
        params: {
            Float: 1234.5,
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_double_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes double shapes",
        body: "{\"Double\":1234.5}",
        bodyMediaType: "application/json",
        params: {
            Double: 1234.5,
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_blob_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes blob shapes",
        body: "{\"Blob\":\"YmluYXJ5LXZhbHVl\"}",
        bodyMediaType: "application/json",
        params: {
            Blob: "binary-value",
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_boolean_shapes_true",
        protocol: awsJson1_1,
        documentation: "Serializes boolean shapes (true)",
        body: "{\"Boolean\":true}",
        bodyMediaType: "application/json",
        params: {
            Boolean: true,
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_boolean_shapes_false",
        protocol: awsJson1_1,
        documentation: "Serializes boolean shapes (false)",
        body: "{\"Boolean\":false}",
        bodyMediaType: "application/json",
        params: {
            Boolean: false,
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_timestamp_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes timestamp shapes",
        body: "{\"Timestamp\":946845296}",
        bodyMediaType: "application/json",
        params: {
            Timestamp: 946845296,
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_timestamp_shapes_with_iso8601_timestampformat",
        protocol: awsJson1_1,
        documentation: "Serializes timestamp shapes with iso8601 timestampFormat",
        body: "{\"Iso8601Timestamp\":\"2000-01-02T20:34:56Z\"}",
        bodyMediaType: "application/json",
        params: {
            Iso8601Timestamp: 946845296,
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_timestamp_shapes_with_httpdate_timestampformat",
        protocol: awsJson1_1,
        documentation: "Serializes timestamp shapes with httpdate timestampFormat",
        body: "{\"HttpdateTimestamp\":\"Sun, 02 Jan 2000 20:34:56 GMT\"}",
        bodyMediaType: "application/json",
        params: {
            HttpdateTimestamp: 946845296,
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_timestamp_shapes_with_unixtimestamp_timestampformat",
        protocol: awsJson1_1,
        documentation: "Serializes timestamp shapes with unixTimestamp timestampFormat",
        body: "{\"UnixTimestamp\":946845296}",
        bodyMediaType: "application/json",
        params: {
            UnixTimestamp: 946845296,
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_list_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes list shapes",
        body: "{\"ListOfStrings\":[\"abc\",\"mno\",\"xyz\"]}",
        bodyMediaType: "application/json",
        params: {
            ListOfStrings: [
                "abc",
                "mno",
                "xyz",
            ],
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_empty_list_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes empty list shapes",
        body: "{\"ListOfStrings\":[]}",
        bodyMediaType: "application/json",
        params: {
            ListOfStrings: [],
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_list_of_map_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes list of map shapes",
        body: "{\"ListOfMapsOfStrings\":[{\"foo\":\"bar\"},{\"abc\":\"xyz\"},{\"red\":\"blue\"}]}",
        bodyMediaType: "application/json",
        params: {
            ListOfMapsOfStrings: [
                {
                    foo: "bar",
                },
                {
                    abc: "xyz",
                },
                {
                    red: "blue",
                },
            ],
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_list_of_structure_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes list of structure shapes",
        body: "{\"ListOfStructs\":[{\"Value\":\"abc\"},{\"Value\":\"mno\"},{\"Value\":\"xyz\"}]}",
        bodyMediaType: "application/json",
        params: {
            ListOfStructs: [
                {
                    Value: "abc",
                },
                {
                    Value: "mno",
                },
                {
                    Value: "xyz",
                },
            ],
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_list_of_recursive_structure_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes list of recursive structure shapes",
        body: "{\"RecursiveList\":[{\"RecursiveList\":[{\"RecursiveList\":[{\"Integer\":123}]}]}]}",
        bodyMediaType: "application/json",
        params: {
            RecursiveList: [
                {
                    RecursiveList: [
                        {
                            RecursiveList: [
                                {
                                    Integer: 123,
                                },
                            ],
                        },
                    ],
                },
            ],
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_map_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes map shapes",
        body: "{\"MapOfStrings\":{\"abc\":\"xyz\",\"mno\":\"hjk\"}}",
        bodyMediaType: "application/json",
        params: {
            MapOfStrings: {
                abc: "xyz",
                mno: "hjk",
            },
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_empty_map_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes empty map shapes",
        body: "{\"MapOfStrings\":{}}",
        bodyMediaType: "application/json",
        params: {
            MapOfStrings: {},
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_map_of_list_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes map of list shapes",
        body: "{\"MapOfListsOfStrings\":{\"abc\":[\"abc\",\"xyz\"],\"mno\":[\"xyz\",\"abc\"]}}",
        bodyMediaType: "application/json",
        params: {
            MapOfListsOfStrings: {
                abc: [
                    "abc",
                    "xyz",
                ],
                mno: [
                    "xyz",
                    "abc",
                ],
            },
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_map_of_structure_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes map of structure shapes",
        body: "{\"MapOfStructs\":{\"key1\":{\"Value\":\"value-1\"},\"key2\":{\"Value\":\"value-2\"}}}",
        bodyMediaType: "application/json",
        params: {
            MapOfStructs: {
                key1: {
                    Value: "value-1",
                },
                key2: {
                    Value: "value-2",
                },
            },
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_map_of_recursive_structure_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes map of recursive structure shapes",
        body: "{\"RecursiveMap\":{\"key1\":{\"RecursiveMap\":{\"key2\":{\"RecursiveMap\":{\"key3\":{\"Boolean\":false}}}}}}}",
        bodyMediaType: "application/json",
        params: {
            RecursiveMap: {
                key1: {
                    RecursiveMap: {
                        key2: {
                            RecursiveMap: {
                                key3: {
                                    Boolean: false,
                                },
                            },
                        },
                    },
                },
            },
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_structure_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes structure shapes",
        body: "{\"SimpleStruct\":{\"Value\":\"abc\"}}",
        bodyMediaType: "application/json",
        params: {
            SimpleStruct: {
                Value: "abc",
            },
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_structure_members_with_locationname_traits",
        protocol: awsJson1_1,
        documentation: "Serializes structure members with locationName traits",
        body: "{\"StructWithLocationName\":{\"RenamedMember\":\"some-value\"}}",
        bodyMediaType: "application/json",
        params: {
            StructWithLocationName: {
                Value: "some-value",
            },
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_empty_structure_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes empty structure shapes",
        body: "{\"SimpleStruct\":{}}",
        bodyMediaType: "application/json",
        params: {
            SimpleStruct: {},
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_structure_which_have_no_members",
        protocol: awsJson1_1,
        documentation: "Serializes structure which have no members",
        body: "{\"EmptyStruct\":{}}",
        bodyMediaType: "application/json",
        params: {
            EmptyStruct: {},
        },
        method: "POST",
        uri: "/",
    },
    {
        id: "serializes_recursive_structure_shapes",
        protocol: awsJson1_1,
        documentation: "Serializes recursive structure shapes",
        body: "{\"String\":\"top-value\",\"Boolean\":false,\"RecursiveStruct\":{\"String\":\"nested-value\",\"Boolean\":true,\"RecursiveList\":[{\"String\":\"string-only\"},{\"RecursiveStruct\":{\"MapOfStrings\":{\"color\":\"red\",\"size\":\"large\"}}}]}}",
        bodyMediaType: "application/json",
        params: {
            String: "top-value",
            Boolean: false,
            RecursiveStruct: {
                String: "nested-value",
                Boolean: true,
                RecursiveList: [
                    {
                        String: "string-only",
                    },
                    {
                        RecursiveStruct: {
                            MapOfStrings: {
                                color: "red",
                                size: "large",
                            },
                        },
                    },
                ],
            },
        },
        method: "POST",
        uri: "/",
    },
])
@httpResponseTests([
    {
        id: "parses_operations_with_empty_json_bodies",
        protocol: awsJson1_1,
        documentation: "Parses operations with empty JSON bodies",
        body: "{}",
        bodyMediaType: "application/json",
        code: 200,
    },
    {
        id: "parses_string_shapes",
        protocol: awsJson1_1,
        documentation: "Parses string shapes",
        body: "{\"String\":\"string-value\"}",
        bodyMediaType: "application/json",
        params: {
            String: "string-value",
        },
        code: 200,
    },
    {
        id: "parses_integer_shapes",
        protocol: awsJson1_1,
        documentation: "Parses integer shapes",
        body: "{\"Integer\":1234}",
        bodyMediaType: "application/json",
        params: {
            Integer: 1234,
        },
        code: 200,
    },
    {
        id: "parses_long_shapes",
        protocol: awsJson1_1,
        documentation: "Parses long shapes",
        body: "{\"Long\":1234567890123456789}",
        bodyMediaType: "application/json",
        params: {
            Long: 1234567890123456789,
        },
        code: 200,
    },
    {
        id: "parses_float_shapes",
        protocol: awsJson1_1,
        documentation: "Parses float shapes",
        body: "{\"Float\":1234.5}",
        bodyMediaType: "application/json",
        params: {
            Float: 1234.5,
        },
        code: 200,
    },
    {
        id: "parses_double_shapes",
        protocol: awsJson1_1,
        documentation: "Parses double shapes",
        body: "{\"Double\":123456789.12345679}",
        bodyMediaType: "application/json",
        params: {
            Double: 123456789.12345679,
        },
        code: 200,
    },
    {
        id: "parses_boolean_shapes_true",
        protocol: awsJson1_1,
        documentation: "Parses boolean shapes (true)",
        body: "{\"Boolean\":true}",
        bodyMediaType: "application/json",
        params: {
            Boolean: true,
        },
        code: 200,
    },
    {
        id: "parses_boolean_false",
        protocol: awsJson1_1,
        documentation: "Parses boolean (false)",
        body: "{\"Boolean\":false}",
        bodyMediaType: "application/json",
        params: {
            Boolean: false,
        },
        code: 200,
    },
    {
        id: "parses_blob_shapes",
        protocol: awsJson1_1,
        documentation: "Parses blob shapes",
        body: "{\"Blob\":\"YmluYXJ5LXZhbHVl\"}",
        bodyMediaType: "application/json",
        params: {
            Blob: "binary-value",
        },
        code: 200,
    },
    {
        id: "parses_timestamp_shapes",
        protocol: awsJson1_1,
        documentation: "Parses timestamp shapes",
        body: "{\"Timestamp\":946845296}",
        bodyMediaType: "application/json",
        params: {
            Timestamp: 946845296,
        },
        code: 200,
    },
    {
        id: "parses_iso8601_timestamps",
        protocol: awsJson1_1,
        documentation: "Parses iso8601 timestamps",
        body: "{\"Timestamp\":\"2000-01-02T20:34:56.000Z\"}",
        bodyMediaType: "application/json",
        params: {
            Timestamp: 946845296,
        },
        code: 200,
    },
    {
        id: "parses_httpdate_timestamps",
        protocol: awsJson1_1,
        documentation: "Parses httpdate timestamps",
        body: "{\"Timestamp\":\"Sun, 02 Jan 2000 20:34:56.000 GMT\"}",
        bodyMediaType: "application/json",
        params: {
            Timestamp: 946845296,
        },
        code: 200,
    },
    {
        id: "parses_list_shapes",
        protocol: awsJson1_1,
        documentation: "Parses list shapes",
        body: "{\"ListOfStrings\":[\"abc\",\"mno\",\"xyz\"]}",
        bodyMediaType: "application/json",
        params: {
            ListOfStrings: [
                "abc",
                "mno",
                "xyz",
            ],
        },
        code: 200,
    },
    {
        id: "parses_list_of_map_shapes",
        protocol: awsJson1_1,
        documentation: "Parses list of map shapes",
        body: "{\"ListOfMapsOfStrings\":[{\"size\":\"large\"},{\"color\":\"red\"}]}",
        bodyMediaType: "application/json",
        params: {
            ListOfMapsOfStrings: [
                {
                    size: "large",
                },
                {
                    color: "red",
                },
            ],
        },
        code: 200,
    },
    {
        id: "parses_list_of_list_shapes",
        protocol: awsJson1_1,
        documentation: "Parses list of list shapes",
        body: "{\"ListOfLists\":[[\"abc\",\"mno\",\"xyz\"],[\"hjk\",\"qrs\",\"tuv\"]]}",
        bodyMediaType: "application/json",
        params: {
            ListOfLists: [
                [
                    "abc",
                    "mno",
                    "xyz",
                ],
                [
                    "hjk",
                    "qrs",
                    "tuv",
                ],
            ],
        },
        code: 200,
    },
    {
        id: "parses_list_of_structure_shapes",
        protocol: awsJson1_1,
        documentation: "Parses list of structure shapes",
        body: "{\"ListOfStructs\":[{\"Value\":\"value-1\"},{\"Value\":\"value-2\"}]}",
        bodyMediaType: "application/json",
        params: {
            ListOfStructs: [
                {
                    Value: "value-1",
                },
                {
                    Value: "value-2",
                },
            ],
        },
        code: 200,
    },
    {
        id: "parses_list_of_recursive_structure_shapes",
        protocol: awsJson1_1,
        documentation: "Parses list of recursive structure shapes",
        body: "{\"RecursiveList\":[{\"RecursiveList\":[{\"RecursiveList\":[{\"String\":\"value\"}]}]}]}",
        bodyMediaType: "application/json",
        params: {
            RecursiveList: [
                {
                    RecursiveList: [
                        {
                            RecursiveList: [
                                {
                                    String: "value",
                                },
                            ],
                        },
                    ],
                },
            ],
        },
        code: 200,
    },
    {
        id: "parses_map_shapes",
        protocol: awsJson1_1,
        documentation: "Parses map shapes",
        body: "{\"MapOfStrings\":{\"size\":\"large\",\"color\":\"red\"}}",
        bodyMediaType: "application/json",
        params: {
            MapOfStrings: {
                size: "large",
                color: "red",
            },
        },
        code: 200,
    },
    {
        id: "parses_map_of_list_shapes",
        protocol: awsJson1_1,
        documentation: "Parses map of list shapes",
        body: "{\"MapOfListsOfStrings\":{\"sizes\":[\"large\",\"small\"],\"colors\":[\"red\",\"green\"]}}",
        bodyMediaType: "application/json",
        params: {
            MapOfListsOfStrings: {
                sizes: [
                    "large",
                    "small",
                ],
                colors: [
                    "red",
                    "green",
                ],
            },
        },
        code: 200,
    },
    {
        id: "parses_map_of_map_shapes",
        protocol: awsJson1_1,
        documentation: "Parses map of map shapes",
        body: "{\"MapOfMaps\":{\"sizes\":{\"large\":\"L\",\"medium\":\"M\"},\"colors\":{\"red\":\"R\",\"blue\":\"B\"}}}",
        bodyMediaType: "application/json",
        params: {
            MapOfMaps: {
                sizes: {
                    large: "L",
                    medium: "M",
                },
                colors: {
                    red: "R",
                    blue: "B",
                },
            },
        },
        code: 200,
    },
    {
        id: "parses_map_of_structure_shapes",
        protocol: awsJson1_1,
        documentation: "Parses map of structure shapes",
        body: "{\"MapOfStructs\":{\"size\":{\"Value\":\"small\"},\"color\":{\"Value\":\"red\"}}}",
        bodyMediaType: "application/json",
        params: {
            MapOfStructs: {
                size: {
                    Value: "small",
                },
                color: {
                    Value: "red",
                },
            },
        },
        code: 200,
    },
    {
        id: "parses_map_of_recursive_structure_shapes",
        protocol: awsJson1_1,
        documentation: "Parses map of recursive structure shapes",
        body: "{\"RecursiveMap\":{\"key-1\":{\"RecursiveMap\":{\"key-2\":{\"RecursiveMap\":{\"key-3\":{\"String\":\"value\"}}}}}}}",
        bodyMediaType: "application/json",
        params: {
            RecursiveMap: {
                "key-1": {
                    RecursiveMap: {
                        "key-2": {
                            RecursiveMap: {
                                "key-3": {
                                    String: "value",
                                },
                            },
                        },
                    },
                },
            },
        },
        code: 200,
    },
    {
        id: "parses_the_request_id_from_the_response",
        protocol: awsJson1_1,
        documentation: "Parses the request id from the response",
        body: "{}",
        bodyMediaType: "application/json",
        headers: {
            "X-Amzn-Requestid": "amazon-uniq-request-id",
        },
        code: 200,
    },
])
operation KitchenSinkOperation {
    input: KitchenSink,
    output: KitchenSink,
    errors: [
        ErrorWithMembers,
        ErrorWithoutMembers,
    ],
}

structure EmptyStruct {}

@error("client")
structure ErrorWithMembers {
    Code: smithy.api#String,
    ComplexData: KitchenSink,
    IntegerField: smithy.api#Integer,
    ListField: ListOfStrings,
    MapField: MapOfStrings,
    Message: smithy.api#String,
    /// abc
    StringField: smithy.api#String,
}

@error("server")
structure ErrorWithoutMembers {}

structure KitchenSink {
    Blob: smithy.api#Blob,
    Boolean: smithy.api#Boolean,
    Double: smithy.api#Double,
    EmptyStruct: EmptyStruct,
    Float: smithy.api#Float,
    @timestampFormat("http-date"    )
    HttpdateTimestamp: smithy.api#Timestamp,
    Integer: smithy.api#Integer,
    @timestampFormat("date-time"    )
    Iso8601Timestamp: smithy.api#Timestamp,
    JsonValue: JsonValue,
    ListOfLists: ListOfListOfStrings,
    ListOfMapsOfStrings: ListOfMapsOfStrings,
    ListOfStrings: ListOfStrings,
    ListOfStructs: ListOfStructs,
    Long: smithy.api#Long,
    MapOfListsOfStrings: MapOfListsOfStrings,
    MapOfMaps: MapOfMapOfStrings,
    MapOfStrings: MapOfStrings,
    MapOfStructs: MapOfStructs,
    RecursiveList: ListOfKitchenSinks,
    RecursiveMap: MapOfKitchenSinks,
    RecursiveStruct: KitchenSink,
    SimpleStruct: SimpleStruct,
    String: smithy.api#String,
    StructWithLocationName: StructWithLocationName,
    Timestamp: smithy.api#Timestamp,
    @timestampFormat("epoch-seconds"    )
    UnixTimestamp: smithy.api#Timestamp,
}



structure StructWithLocationName {
    @jsonName("RenamedMember"    )
    Value: smithy.api#String,
}

list ListOfKitchenSinks {
    member: KitchenSink,
}

list ListOfListOfStrings {
    member: ListOfStrings,
}

list ListOfMapsOfStrings {
    member: MapOfStrings,
}

list ListOfStrings {
    member: smithy.api#String,
}

list ListOfStructs {
    member: SimpleStruct,
}

map MapOfKitchenSinks {
    key: smithy.api#String,
    value: KitchenSink,
}

map MapOfListsOfStrings {
    key: smithy.api#String,
    value: ListOfStrings,
}

map MapOfMapOfStrings {
    key: smithy.api#String,
    value: MapOfStrings,
}

map MapOfStrings {
    key: smithy.api#String,
    value: smithy.api#String,
}

map MapOfStructs {
    key: smithy.api#String,
    value: SimpleStruct,
}

blob Blob

boolean Boolean

double Double

float Float

integer Integer

@mediaType("application/json")
string JsonValue

long Long

string String

timestamp Timestamp
