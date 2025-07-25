$version: "2.0"

namespace smithy.example

use smithy.rules#bdd
use smithy.rules#clientContextParams

@clientContextParams(
    Region: {type: "string", documentation: "docs"}
    UseFips: {type: "boolean", documentation: "docs"}
)
@bdd({
    "parameters": {
        "Region": {
            "required": true,
            "documentation": "The AWS region",
            "type": "string"
        },
        "UseFips": {
            "required": true,
            "default": false,
            "documentation": "Use FIPS endpoints",
            "type": "boolean"
        }
    },
    "conditions": [
        {
            "fn": "booleanEquals",
            "argv": [
                {
                    "ref": "UseFips"
                },
                true
            ]
        }
    ],
    "results": [
        {
            "endpoint": {
                "url": "https://service-fips.{Region}.amazonaws.com",
                "properties": {},
                "headers": {}
            },
            "type": "endpoint"
        },
        {
            "endpoint": {
                "url": "https://service.{Region}.amazonaws.com",
                "properties": {},
                "headers": {}
            },
            "type": "endpoint"
        }
    ],
    "root": 2,
    "nodes": "AQIBAIKEr1+EhK9f",
    "nodeCount": 2
})
service ValidBddService {
    version: "2022-01-01"
}
