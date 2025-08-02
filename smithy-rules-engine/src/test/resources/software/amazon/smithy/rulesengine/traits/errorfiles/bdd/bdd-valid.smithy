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
            "conditions": [],
            "endpoint": {
                "url": "https://service-fips.{Region}.amazonaws.com",
                "properties": {},
                "headers": {}
            },
            "type": "endpoint"
        },
        {
            "conditions": [],
            "endpoint": {
                "url": "https://service.{Region}.amazonaws.com",
                "properties": {},
                "headers": {}
            },
            "type": "endpoint"
        }
    ],
    "root": 2,
    "nodeCount": 2,
    "nodes": "/////wAAAAH/////AAAAAAX14QEF9eEC"
})
service ValidBddService {
    version: "2022-01-01"
}
