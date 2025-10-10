// This file defines test cases that test HTTP query params behavior when no other query parameters exist.
// See: https://smithy.io/2.0/spec/http-bindings.html#httpqueryparams-trait

$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpRequestTests

/// This example tests httpQueryParams when no other query parameters exist.
@readonly
@http(uri: "/http-query-params-only", method: "GET")
operation HttpQueryParamsOnlyOperation {
    input: HttpQueryParamsOnlyInput,
}

apply HttpQueryParamsOnlyOperation @httpRequestTests([
    {
        id: "HttpQueryParamsOnlyRequest",
        documentation: "Test that httpQueryParams are included in request when no other query parameters exist",
        protocol: restJson1,
        method: "GET",
        uri: "/http-query-params-only",
        queryParams: ["a=b", "c=d"],
        params: {
            queryMap: {
                "a": "b",
                "c": "d"
            }
        },
        appliesTo: "client",
    },
    {
        id: "HttpQueryParamsOnlyEmptyRequest",
        documentation: "Test that empty httpQueryParams map results in no query parameters",
        protocol: restJson1,
        method: "GET",
        uri: "/http-query-params-only",
        params: {
            queryMap: {}
        },
        appliesTo: "client",
    }
])

structure HttpQueryParamsOnlyInput {
    @httpQueryParams
    queryMap: QueryMap,
}

map QueryMap {
    key: String,
    value: String,
}
