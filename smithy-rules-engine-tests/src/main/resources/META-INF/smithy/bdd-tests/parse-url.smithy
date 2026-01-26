$version: "2.0"

namespace smithy.tests.endpointrules.parseurl

use smithy.rules#clientContextParams
use smithy.rules#endpointBdd
use smithy.rules#endpointTests

@suppress([
    "UnstableTrait"
])
@clientContextParams(
    Endpoint: {
        type: "string"
        documentation: "docs"
    }
)
@endpointBdd(
    version: "1.1"
    parameters: {
        Endpoint: {
            required: false
            documentation: "docs"
            type: "string"
        }
    }
    conditions: [
        {
            fn: "isSet"
            argv: [
                {
                    ref: "Endpoint"
                }
            ]
        }
        {
            fn: "parseURL"
            argv: [
                "{Endpoint}"
            ]
            assign: "url"
        }
        {
            fn: "booleanEquals"
            argv: [
                true
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "url"
                        }
                        "isIp"
                    ]
                }
            ]
        }
        {
            fn: "stringEquals"
            argv: [
                "/port"
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "url"
                        }
                        "path"
                    ]
                }
            ]
        }
        {
            fn: "stringEquals"
            argv: [
                "/"
                {
                    fn: "getAttr"
                    argv: [
                        {
                            ref: "url"
                        }
                        "normalizedPath"
                    ]
                }
            ]
        }
    ]
    results: [
        {
            conditions: []
            endpoint: {
                url: "{url#scheme}://{url#authority}{url#normalizedPath}is-ip-addr"
                properties: {}
                headers: {}
            }
            type: "endpoint"
        }
        {
            conditions: []
            endpoint: {
                url: "{url#scheme}://{url#authority}/uri-with-port"
                properties: {}
                headers: {}
            }
            type: "endpoint"
        }
        {
            conditions: []
            endpoint: {
                url: "https://{url#scheme}-{url#authority}-nopath.example.com"
                properties: {}
                headers: {}
            }
            type: "endpoint"
        }
        {
            conditions: []
            endpoint: {
                url: "https://{url#scheme}-{url#authority}.example.com/path-is{url#path}"
                properties: {}
                headers: {}
            }
            type: "endpoint"
        }
        {
            conditions: []
            error: "endpoint was invalid"
            type: "error"
        }
    ]
    root: 2
    nodeCount: 6
    nodes: "/////wAAAAH/////AAAAAAAAAAMF9eEFAAAAAQAAAAQF9eEFAAAAAgX14QEAAAAFAAAAAwX14QIAAAAGAAAABAX14QMF9eEE"
)
@endpointTests(
    version: "1.0"
    testCases: [
        {
            documentation: "simple URL parsing"
            params: {
                Endpoint: "https://authority.com/custom-path"
            }
            expect: {
                endpoint: {
                    url: "https://https-authority.com.example.com/path-is/custom-path"
                }
            }
        }
        {
            documentation: "empty path no slash"
            params: {
                Endpoint: "https://authority.com"
            }
            expect: {
                endpoint: {
                    url: "https://https-authority.com-nopath.example.com"
                }
            }
        }
        {
            documentation: "empty path with slash"
            params: {
                Endpoint: "https://authority.com/"
            }
            expect: {
                endpoint: {
                    url: "https://https-authority.com-nopath.example.com"
                }
            }
        }
        {
            documentation: "authority with port"
            params: {
                Endpoint: "https://authority.com:8000/port"
            }
            expect: {
                endpoint: {
                    url: "https://authority.com:8000/uri-with-port"
                }
            }
        }
        {
            documentation: "http schemes"
            params: {
                Endpoint: "http://authority.com:8000/port"
            }
            expect: {
                endpoint: {
                    url: "http://authority.com:8000/uri-with-port"
                }
            }
        }
        {
            documentation: "arbitrary schemes are not supported"
            params: {
                Endpoint: "acbd://example.com"
            }
            expect: {
                error: "endpoint was invalid"
            }
        }
        {
            documentation: "host labels are not validated"
            params: {
                Endpoint: "http://99_ab.com"
            }
            expect: {
                endpoint: {
                    url: "https://http-99_ab.com-nopath.example.com"
                }
            }
        }
        {
            documentation: "host labels are not validated"
            params: {
                Endpoint: "http://99_ab-.com"
            }
            expect: {
                endpoint: {
                    url: "https://http-99_ab-.com-nopath.example.com"
                }
            }
        }
        {
            documentation: "invalid URL"
            params: {
                Endpoint: "http://abc.com:a/foo"
            }
            expect: {
                error: "endpoint was invalid"
            }
        }
        {
            documentation: "IP Address"
            params: {
                Endpoint: "http://192.168.1.1/foo/"
            }
            expect: {
                endpoint: {
                    url: "http://192.168.1.1/foo/is-ip-addr"
                }
            }
        }
        {
            documentation: "IP Address with port"
            params: {
                Endpoint: "http://192.168.1.1:1234/foo/"
            }
            expect: {
                endpoint: {
                    url: "http://192.168.1.1:1234/foo/is-ip-addr"
                }
            }
        }
        {
            documentation: "IPv6 Address"
            params: {
                Endpoint: "https://[2001:db8:85a3:8d3:1319:8a2e:370:7348]:443"
            }
            expect: {
                endpoint: {
                    url: "https://[2001:db8:85a3:8d3:1319:8a2e:370:7348]:443/is-ip-addr"
                }
            }
        }
        {
            documentation: "weird DNS name"
            params: {
                Endpoint: "https://999.999.abc.blah"
            }
            expect: {
                endpoint: {
                    url: "https://https-999.999.abc.blah-nopath.example.com"
                }
            }
        }
        {
            documentation: "query in resolved endpoint is not supported"
            params: {
                Endpoint: "https://example.com/path?query1=foo"
            }
            expect: {
                error: "endpoint was invalid"
            }
        }
    ]
)
service FizzBuzz {
}
