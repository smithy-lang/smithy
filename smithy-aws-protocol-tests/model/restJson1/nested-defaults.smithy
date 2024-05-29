$version: "2.0"

namespace aws.protocoltests.restjson

use aws.protocols#restJson1
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

apply OperationWithNestedStructure @httpRequestTests([
    {
        id: "RestJsonClientPopulatesNestedDefaultValuesWhenMissing"
        documentation: "Client populates nested default values when missing."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: restJson1
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/OperationWithNestedStructure"
        headers: {"Content-Type": "application/json"}
        body: """
            {
                "topLevel": {
                    "dialog": {
                        "language": "en",
                        "greeting": "hi"
                    },
                    "dialogList": [
                        {
                            "greeting": "hi"
                        },
                        {
                            "greeting": "hi",
                            "farewell": {
                                "phrase": "bye"
                            }
                        },
                        {
                            "language": "it",
                            "greeting": "ciao",
                            "farewell": {
                                "phrase": "arrivederci"
                            }
                        }
                    ],
                    "dialogMap": {
                        "emptyDialog": {
                            "greeting": "hi"
                        },
                        "partialEmptyDialog": {
                            "language": "en",
                            "greeting": "hi",
                            "farewell": {
                                "phrase": "bye"
                            }
                        },
                        "nonEmptyDialog": {
                            "greeting": "konnichiwa",
                            "farewell": {
                                "phrase": "sayonara"
                            }
                        }
                    }
                }
            }"""
        params: {
            "topLevel": {
                "dialog": {
                    "language": "en"
                },
                "dialogList": [
                    {
                    },
                    {
                        "farewell": {}
                    },
                    {
                        "language": "it",
                        "greeting": "ciao",
                        "farewell": {
                            "phrase": "arrivederci"
                        }
                    }
                ],
                "dialogMap": {
                    "emptyDialog": {
                    },
                    "partialEmptyDialog": {
                        "language": "en",
                        "farewell": {}
                    },
                    "nonEmptyDialog": {
                        "greeting": "konnichiwa",
                        "farewell": {
                            "phrase": "sayonara"
                        }
                    }
                }
            }
        }
    }
    {
        id: "RestJsonServerPopulatesNestedDefaultsWhenMissingInRequestBody"
        documentation: "Server populates nested default values when missing in request body."
        appliesTo: "server"
        tags: ["defaults"]
        protocol: restJson1
        method: "POST"
        bodyMediaType: "application/json"
        uri: "/OperationWithNestedStructure"
        headers: {"Content-Type": "application/json"}
        body: """
            {
                "topLevel": {
                    "dialog": {
                        "language": "en"
                    },
                    "dialogList": [
                        {
                        },
                        {
                            "farewell": {}
                        },
                        {
                            "language": "it",
                            "greeting": "ciao",
                            "farewell": {
                                "phrase": "arrivederci"
                            }
                        }
                    ],
                    "dialogMap": {
                        "emptyDialog": {
                        },
                        "partialEmptyDialog": {
                            "language": "en",
                            "farewell": {}
                        },
                        "nonEmptyDialog": {
                            "greeting": "konnichiwa",
                            "farewell": {
                                "phrase": "sayonara"
                            }
                        }
                    }
                }
            }"""
        params: {
            "topLevel": {
                "dialog": {
                    "language": "en"
                    "greeting": "hi",
                }
                "dialogList": [
                    {
                        "greeting": "hi",
                    },
                    {
                        "greeting": "hi",
                        "farewell": {
                            "phrase": "bye",
                        }
                    },
                    {
                        "language": "it",
                        "greeting": "ciao",
                        "farewell": {
                            "phrase": "arrivederci"
                        }
                    }
                ],
                "dialogMap": {
                    "emptyDialog": {
                        "greeting": "hi",
                    },
                    "partialEmptyDialog": {
                        "language": "en",
                        "greeting": "hi",
                        "farewell": {
                            "phrase": "bye",
                        }
                    },
                    "nonEmptyDialog": {
                        "greeting": "konnichiwa",
                        "farewell": {
                            "phrase": "sayonara"
                        }
                    }
                }
            }
        }
    }
])

apply OperationWithNestedStructure @httpResponseTests([
    {
        id: "RestJsonClientPopulatesNestedDefaultsWhenMissingInResponseBody"
        documentation: "Client populates nested default values when missing in response body."
        appliesTo: "client"
        tags: ["defaults"]
        protocol: restJson1
        code: 200
        bodyMediaType: "application/json"
        headers: {"Content-Type": "application/json"}
        body: """
            {
                "dialog": {
                    "language": "en"
                },
                "dialogList": [
                    {
                    },
                    {
                        "farewell": {}
                    },
                    {
                        "language": "it",
                        "greeting": "ciao",
                        "farewell": {
                            "phrase": "arrivederci"
                        }
                    }
                ],
                "dialogMap": {
                    "emptyDialog": {
                    },
                    "partialEmptyDialog": {
                        "language": "en",
                        "farewell": {}
                    },
                    "nonEmptyDialog": {
                        "greeting": "konnichiwa",
                        "farewell": {
                            "phrase": "sayonara"
                        }
                    }
                }
            }"""
        params: {
            "dialog": {
                "language": "en"
                "greeting": "hi",
            }
            "dialogList": [
                {
                    "greeting": "hi",
                },
                {
                    "greeting": "hi",
                    "farewell": {
                        "phrase": "bye",
                    }
                },
                {
                    "language": "it",
                    "greeting": "ciao",
                    "farewell": {
                        "phrase": "arrivederci"
                    }
                }
            ],
            "dialogMap": {
                "emptyDialog": {
                    "greeting": "hi",
                },
                "partialEmptyDialog": {
                    "language": "en",
                    "greeting": "hi",
                    "farewell": {
                        "phrase": "bye",
                    }
                },
                "nonEmptyDialog": {
                    "greeting": "konnichiwa",
                    "farewell": {
                        "phrase": "sayonara"
                    }
                }
            }
        }
    }
    {
        id: "RestJsonServerPopulatesNestedDefaultValuesWhenMissingInInResponseParams"
        documentation: "Server populates nested default values when missing in response params."
        appliesTo: "server"
        tags: ["defaults"]
        protocol: restJson1
        code: 200
        bodyMediaType: "application/json"
        headers: {"Content-Type": "application/json"}
        body: """
            {
                "dialog": {
                    "language": "en",
                    "greeting": "hi"
                },
                "dialogList": [
                    {
                        "greeting": "hi"
                    },
                    {
                        "greeting": "hi",
                        "farewell": {
                            "phrase": "bye"
                        }
                    },
                    {
                        "language": "it",
                        "greeting": "ciao",
                        "farewell": {
                            "phrase": "arrivederci"
                        }
                    }
                ],
                "dialogMap": {
                    "emptyDialog": {
                        "greeting": "hi"
                    },
                    "partialEmptyDialog": {
                        "language": "en",
                        "greeting": "hi",
                        "farewell": {
                            "phrase": "bye"
                        }
                    },
                    "nonEmptyDialog": {
                        "greeting": "konnichiwa",
                        "farewell": {
                            "phrase": "sayonara"
                        }
                    }
                }
            }"""
        params: {
            "dialog": {
                "language": "en"
            },
            "dialogList": [
                {
                },
                {
                    "farewell": {}
                },
                {
                    "language": "it",
                    "greeting": "ciao",
                    "farewell": {
                        "phrase": "arrivederci"
                    }
                }
            ],
            "dialogMap": {
                "emptyDialog": {
                },
                "partialEmptyDialog": {
                    "language": "en",
                    "farewell": {}
                },
                "nonEmptyDialog": {
                    "greeting": "konnichiwa",
                    "farewell": {
                        "phrase": "sayonara"
                    }
                }
            }
        }
    }
])

@http(uri: "/OperationWithNestedStructure", method: "POST")
operation OperationWithNestedStructure {
    input := {
        @required
        topLevel: TopLevel
    }

    output := with [NestedDefaultsMixin] {
    }
}

structure TopLevel with [NestedDefaultsMixin] {

}

@mixin
structure NestedDefaultsMixin {
    @required
    dialog: Dialog
    dialogList: DialogList = []
    dialogMap: DialogMap = {}
}

structure Dialog {
    language: String
    greeting: String = "hi"
    farewell: Farewell
}

structure Farewell {
    phrase: String = "bye"
}

list DialogList {
    member: Dialog
}

map DialogMap {
    key: String
    value: Dialog
}