$version: "2.0"

namespace smithy.protocoltests.rpcv2Cbor

use smithy.protocols#rpcv2Cbor
use smithy.test#httpRequestTests
use smithy.test#httpResponseTests

@httpRequestTests([
    {
        id: "RpcV2CborSimpleScalarProperties",
        protocol: rpcv2Cbor,
        documentation: "Serializes simple scalar properties",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        }
        requireHeaders: [
            "Content-Length"
        ],
        method: "POST",
        bodyMediaType: "application/cbor",
        uri: "/service/RpcV2Protocol/operation/SimpleScalarProperties",
        body: "v2lieXRlVmFsdWUFa2RvdWJsZVZhbHVl+z/+OVgQYk3TcWZhbHNlQm9vbGVhblZhbHVl9GpmbG9hdFZhbHVl+kD0AABsaW50ZWdlclZhbHVlGQEAaWxvbmdWYWx1ZRkmkWpzaG9ydFZhbHVlGSaqa3N0cmluZ1ZhbHVlZnNpbXBsZXB0cnVlQm9vbGVhblZhbHVl9WlibG9iVmFsdWVDZm9v/w==",
        params: {
            byteValue: 5,
            doubleValue: 1.889,
            falseBooleanValue: false,
            floatValue: 7.625,
            integerValue: 256,
            longValue: 9873,
            shortValue: 9898,
            stringValue: "simple",
            trueBooleanValue: true,
            blobValue: "foo"
        }
    },
    {
        id: "RpcV2CborSimpleScalarPropertiesUsingIndefiniteLength",
        protocol: rpcv2Cbor,
        documentation: """
            The server should be capable of deserializing simple scalar properties
            encoded using a map with a definite length. The server should also be able to parse
            a key encoded using an indefinite length string.""",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        }
        requireHeaders: [
            "Content-Length"
        ],
        method: "POST",
        bodyMediaType: "application/cbor",
        uri: "/service/RpcV2Protocol/operation/SimpleScalarProperties",
        body: "qmlieXRlVmFsdWUFf2Zkb3VibGVlVmFsdWX/+z/+OVgQYk3Tf2VmYWxzZWdCb29sZWFuZVZhbHVl//RqZmxvYXRWYWx1ZfpA9AAAbGludGVnZXJWYWx1ZRkBAGlsb25nVmFsdWUZJpFqc2hvcnRWYWx1ZRkmqn9mc3RyaW5nZVZhbHVl/2ZzaW1wbGVwdHJ1ZUJvb2xlYW5WYWx1ZfVpYmxvYlZhbHVlQ2Zvbw==",
        params: {
            byteValue: 5,
            doubleValue: 1.889,
            falseBooleanValue: false,
            floatValue: 7.625,
            integerValue: 256,
            longValue: 9873,
            shortValue: 9898,
            stringValue: "simple",
            trueBooleanValue: true,
            blobValue: "foo"
        },
        appliesTo: "server"
    },
    {
        id: "RpcV2CborClientDoesntSerializeNullStructureValues",
        documentation: "RpcV2 Cbor should not serialize null structure values",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/SimpleScalarProperties",
        body: "v/8=",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        }
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            stringValue: null
        },
        appliesTo: "client"
    },
    {
        id: "RpcV2CborServerDoesntDeSerializeNullStructureValues",
        documentation: "RpcV2 Cbor should not deserialize null structure values",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/SimpleScalarProperties",
        body: "v2tzdHJpbmdWYWx1Zfb/",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        }
        requireHeaders: [
            "Content-Length"
        ],
        params: {},
        appliesTo: "server"
    },
    {
        id: "RpcV2CborSupportsNaNFloatInputs",
        protocol: rpcv2Cbor,
        documentation: "Supports handling NaN float values.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        }
        requireHeaders: [
            "Content-Length"
        ],
        method: "POST",
        bodyMediaType: "application/cbor",
        uri: "/service/RpcV2Protocol/operation/SimpleScalarProperties",
        body: "v2tkb3VibGVWYWx1Zft/+AAAAAAAAGpmbG9hdFZhbHVl+n/AAAD/"
        params: {
            doubleValue: "NaN",
            floatValue: "NaN"
        }
    },
    {
        id: "RpcV2CborSupportsInfinityFloatInputs",
        protocol: rpcv2Cbor,
        documentation: "Supports handling Infinity float values.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        }
        requireHeaders: [
            "Content-Length"
        ],
        method: "POST",
        bodyMediaType: "application/cbor",
        uri: "/service/RpcV2Protocol/operation/SimpleScalarProperties",
        body: "v2tkb3VibGVWYWx1Zft/8AAAAAAAAGpmbG9hdFZhbHVl+n+AAAD/"
        params: {
            doubleValue: "Infinity",
            floatValue: "Infinity"
        }
    },
    {
        id: "RpcV2CborSupportsNegativeInfinityFloatInputs",
        protocol: rpcv2Cbor,
        documentation: "Supports handling Infinity float values.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        }
        requireHeaders: [
            "Content-Length"
        ],
        method: "POST",
        bodyMediaType: "application/cbor",
        uri: "/service/RpcV2Protocol/operation/SimpleScalarProperties",
        body: "v2tkb3VibGVWYWx1Zfv/8AAAAAAAAGpmbG9hdFZhbHVl+v+AAAD/"
        params: {
            doubleValue: "-Infinity",
            floatValue: "-Infinity"
        }
    },
    {
        id: "RpcV2CborIndefiniteLengthStringsCanBeDeserialized",
        protocol: rpcv2Cbor,
        documentation: "The server should be capable of deserializing indefinite length text strings.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        }
        requireHeaders: [
            "Content-Length"
        ],
        method: "POST",
        bodyMediaType: "application/cbor",
        uri: "/service/RpcV2Protocol/operation/SimpleScalarProperties",
        body: "oWtzdHJpbmdWYWx1ZX94HUFuIGV4YW1wbGUgaW5kZWZpbml0ZSBzdHJpbmcscSBjaHVua2VkIG9uIGNvbW1h/w=="
        params: {
            stringValue: "An example indefinite string, chunked on comma"
        },
        appliesTo: "server"
    },
    {
        id: "RpcV2CborIndefiniteLengthByteStringsCanBeDeserialized",
        protocol: rpcv2Cbor,
        documentation: "The server should be capable of deserializing indefinite length byte strings.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        }
        requireHeaders: [
            "Content-Length"
        ],
        method: "POST",
        bodyMediaType: "application/cbor",
        uri: "/service/RpcV2Protocol/operation/SimpleScalarProperties",
        body: "oWlibG9iVmFsdWVfWCJBbiBleGFtcGxlIGluZGVmaW5pdGUtYnl0ZSBzdHJpbmcsUSBjaHVua2VkIG9uIGNvbW1h/w=="
        params: {
            blobValue: "An example indefinite-byte string, chunked on comma"
        },
        appliesTo: "server"
    },
    {
        id: "RpcV2CborSupportsUpcastingData",
        protocol: rpcv2Cbor,
        documentation: "Supports upcasting from a smaller byte representation of the same data type.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        }
        requireHeaders: [
            "Content-Length"
        ],
        method: "POST",
        bodyMediaType: "application/cbor",
        uri: "/service/RpcV2Protocol/operation/SimpleScalarProperties",
        body: "v2tkb3VibGVWYWx1Zfk+AGpmbG9hdFZhbHVl+UegbGludGVnZXJWYWx1ZRg4aWxvbmdWYWx1ZRkBAGpzaG9ydFZhbHVlCv8="
        params: {
            doubleValue: 1.5,
            floatValue: 7.625,
            integerValue: 56,
            longValue: 256,
            shortValue: 10
        },
        appliesTo: "server"
    },
    {
        id: "RpcV2CborExtraFieldsInTheBodyShouldBeSkippedByServers",
        protocol: rpcv2Cbor,
        documentation: """
            The server should skip over additional fields that are not part of the structure. This allows a
            client generated against a newer Smithy model to be able to communicate with a server that is
            generated against an older Smithy model.""",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        }
        requireHeaders: [
            "Content-Length"
        ],
        method: "POST",
        bodyMediaType: "application/cbor",
        uri: "/service/RpcV2Protocol/operation/SimpleScalarProperties",
        body: "v2lieXRlVmFsdWUFa2RvdWJsZVZhbHVl+z/+OVgQYk3TcWZhbHNlQm9vbGVhblZhbHVl9GpmbG9hdFZhbHVl+kD0AABrZXh0cmFPYmplY3S/c2luZGVmaW5pdGVMZW5ndGhNYXC/a3dpdGhBbkFycmF5nwECA///cWRlZmluaXRlTGVuZ3RoTWFwo3J3aXRoQURlZmluaXRlQXJyYXmDAQIDeB1hbmRTb21lSW5kZWZpbml0ZUxlbmd0aFN0cmluZ3gfdGhhdCBoYXMsIGJlZW4gY2h1bmtlZCBvbiBjb21tYWxub3JtYWxTdHJpbmdjZm9vanNob3J0VmFsdWUZJw9uc29tZU90aGVyRmllbGR2dGhpcyBzaG91bGQgYmUgc2tpcHBlZP9saW50ZWdlclZhbHVlGQEAaWxvbmdWYWx1ZRkmkWpzaG9ydFZhbHVlGSaqa3N0cmluZ1ZhbHVlZnNpbXBsZXB0cnVlQm9vbGVhblZhbHVl9WlibG9iVmFsdWVDZm9v/w==",
        params: {
            byteValue: 5,
            doubleValue: 1.889,
            falseBooleanValue: false,
            floatValue: 7.625,
            integerValue: 256,
            longValue: 9873,
            shortValue: 9898,
            stringValue: "simple",
            trueBooleanValue: true,
            blobValue: "foo"
        },
        appliesTo: "server"
    },
    {
        id: "RpcV2CborServersShouldHandleNoAcceptHeader",
        protocol: rpcv2Cbor,
        documentation: "Servers should tolerate requests without an Accept header set.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        }
        requireHeaders: [
            "Content-Length"
        ],
        method: "POST",
        bodyMediaType: "application/cbor",
        uri: "/service/RpcV2Protocol/operation/SimpleScalarProperties",
        body: "v2lieXRlVmFsdWUFa2RvdWJsZVZhbHVl+z/+OVgQYk3TcWZhbHNlQm9vbGVhblZhbHVl9GpmbG9hdFZhbHVl+kD0AABsaW50ZWdlclZhbHVlGQEAaWxvbmdWYWx1ZRkmkWpzaG9ydFZhbHVlGSaqa3N0cmluZ1ZhbHVlZnNpbXBsZXB0cnVlQm9vbGVhblZhbHVl9WlibG9iVmFsdWVDZm9v/w==",
        params: {
            byteValue: 5,
            doubleValue: 1.889,
            falseBooleanValue: false,
            floatValue: 7.625,
            integerValue: 256,
            longValue: 9873,
            shortValue: 9898,
            stringValue: "simple",
            trueBooleanValue: true,
            blobValue: "foo"
        },
        appliesTo: "server"
    }
])
@httpResponseTests([
    {
        id: "RpcV2CborSimpleScalarProperties",
        protocol: rpcv2Cbor,
        documentation: "Serializes simple scalar properties",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        }
        bodyMediaType: "application/cbor",
        body: "v3B0cnVlQm9vbGVhblZhbHVl9XFmYWxzZUJvb2xlYW5WYWx1ZfRpYnl0ZVZhbHVlBWtkb3VibGVWYWx1Zfs//jlYEGJN02pmbG9hdFZhbHVl+kD0AABsaW50ZWdlclZhbHVlGQEAanNob3J0VmFsdWUZJqprc3RyaW5nVmFsdWVmc2ltcGxlaWJsb2JWYWx1ZUNmb2//",
        code: 200,
        params: {
            trueBooleanValue: true,
            falseBooleanValue: false,
            byteValue: 5,
            doubleValue: 1.889,
            floatValue: 7.625,
            integerValue: 256,
            shortValue: 9898,
            stringValue: "simple",
            blobValue: "foo"
        }
    },
    {
        id: "RpcV2CborSimpleScalarPropertiesUsingDefiniteLength",
        protocol: rpcv2Cbor,
        documentation: "Deserializes simple scalar properties encoded using a map with definite length",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        }
        bodyMediaType: "application/cbor",
        body: "qXB0cnVlQm9vbGVhblZhbHVl9XFmYWxzZUJvb2xlYW5WYWx1ZfRpYnl0ZVZhbHVlBWtkb3VibGVWYWx1Zfs//jlYEGJN02pmbG9hdFZhbHVl+kD0AABsaW50ZWdlclZhbHVlGQEAanNob3J0VmFsdWUZJqprc3RyaW5nVmFsdWVmc2ltcGxlaWJsb2JWYWx1ZUNmb28=",
        code: 200,
        params: {
            trueBooleanValue: true,
            falseBooleanValue: false,
            byteValue: 5,
            doubleValue: 1.889,
            floatValue: 7.625,
            integerValue: 256,
            shortValue: 9898,
            stringValue: "simple",
            blobValue: "foo"
        },
        appliesTo: "client"
    },
    {
        id: "RpcV2CborClientDoesntDeserializeNullStructureValues",
        documentation: "RpcV2 Cbor should not deserialize null structure values",
        protocol: rpcv2Cbor,
        body: "v2tzdHJpbmdWYWx1Zfb/",
        code: 200,
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        }
        params: {}
        appliesTo: "client"
    },
    {
        id: "RpcV2CborServerDoesntSerializeNullStructureValues",
        documentation: "RpcV2 Cbor should not serialize null structure values",
        protocol: rpcv2Cbor,
        body: "v/8=",
        code: 200,
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        }
        params: {
            stringValue: null
        },
        appliesTo: "server"
    },
    {
        id: "RpcV2CborSupportsNaNFloatOutputs",
        protocol: rpcv2Cbor,
        documentation: "Supports handling NaN float values.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        code: 200,
        bodyMediaType: "application/cbor",
        body: "v2tkb3VibGVWYWx1Zft/+AAAAAAAAGpmbG9hdFZhbHVl+n/AAAD/"
        params: {
            doubleValue: "NaN",
            floatValue: "NaN"
        }
    },
    {
        id: "RpcV2CborSupportsInfinityFloatOutputs",
        protocol: rpcv2Cbor,
        documentation: "Supports handling Infinity float values.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        code: 200,
        bodyMediaType: "application/cbor",
        body: "v2tkb3VibGVWYWx1Zft/8AAAAAAAAGpmbG9hdFZhbHVl+n+AAAD/"
        params: {
            doubleValue: "Infinity",
            floatValue: "Infinity"
        }
    },
    {
        id: "RpcV2CborSupportsNegativeInfinityFloatOutputs",
        protocol: rpcv2Cbor,
        documentation: "Supports handling Negative Infinity float values.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        code: 200,
        bodyMediaType: "application/cbor",
        body: "v2tkb3VibGVWYWx1Zfv/8AAAAAAAAGpmbG9hdFZhbHVl+v+AAAD/"
        params: {
            doubleValue: "-Infinity",
            floatValue: "-Infinity"
        }
    },
    {
        id: "RpcV2CborSupportsUpcastingDataOnDeserialize",
        protocol: rpcv2Cbor,
        documentation: "Supports upcasting from a smaller byte representation of the same data type.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        code: 200,
        bodyMediaType: "application/cbor",
        body: "v2tkb3VibGVWYWx1Zfk+AGpmbG9hdFZhbHVl+UegbGludGVnZXJWYWx1ZRg4aWxvbmdWYWx1ZRkBAGpzaG9ydFZhbHVlCv8="
        params: {
            doubleValue: 1.5,
            floatValue: 7.625,
            integerValue: 56,
            longValue: 256,
            shortValue: 10
        },
        appliesTo: "client"
    },
    {
        id: "RpcV2CborExtraFieldsInTheBodyShouldBeSkippedByClients",
        protocol: rpcv2Cbor,
        documentation: """
            The client should skip over additional fields that are not part of the structure. This allows a
            client generated against an older Smithy model to be able to communicate with a server that is
            generated against a newer Smithy model.""",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        }
        code: 200,
        bodyMediaType: "application/cbor",
        body: "v2lieXRlVmFsdWUFa2RvdWJsZVZhbHVl+z/+OVgQYk3TcWZhbHNlQm9vbGVhblZhbHVl9GpmbG9hdFZhbHVl+kD0AABrZXh0cmFPYmplY3S/c2luZGVmaW5pdGVMZW5ndGhNYXC/a3dpdGhBbkFycmF5nwECA///cWRlZmluaXRlTGVuZ3RoTWFwo3J3aXRoQURlZmluaXRlQXJyYXmDAQIDeB1hbmRTb21lSW5kZWZpbml0ZUxlbmd0aFN0cmluZ3gfdGhhdCBoYXMsIGJlZW4gY2h1bmtlZCBvbiBjb21tYWxub3JtYWxTdHJpbmdjZm9vanNob3J0VmFsdWUZJw9uc29tZU90aGVyRmllbGR2dGhpcyBzaG91bGQgYmUgc2tpcHBlZP9saW50ZWdlclZhbHVlGQEAaWxvbmdWYWx1ZRkmkWpzaG9ydFZhbHVlGSaqa3N0cmluZ1ZhbHVlZnNpbXBsZXB0cnVlQm9vbGVhblZhbHVl9WlibG9iVmFsdWVDZm9v/w==",
        params: {
            byteValue: 5,
            doubleValue: 1.889,
            falseBooleanValue: false,
            floatValue: 7.625,
            integerValue: 256,
            longValue: 9873,
            shortValue: 9898,
            stringValue: "simple",
            trueBooleanValue: true,
            blobValue: "foo"
        },
        appliesTo: "client"
    }
])
operation SimpleScalarProperties {
    input: SimpleScalarStructure,
    output: SimpleScalarStructure
}

apply RecursiveShapes @httpRequestTests([
    {
        id: "RpcV2CborRecursiveShapes",
        documentation: "Serializes recursive structures",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/RpcV2Protocol/operation/RecursiveShapes",
        body: "v2ZuZXN0ZWS/Y2Zvb2RGb28xZm5lc3RlZL9jYmFyZEJhcjFvcmVjdXJzaXZlTWVtYmVyv2Nmb29kRm9vMmZuZXN0ZWS/Y2JhcmRCYXIy//////8=",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor",
            "Accept": "application/cbor"
        },
        requireHeaders: [
            "Content-Length"
        ],
        params: {
            nested: {
                foo: "Foo1",
                nested: {
                    bar: "Bar1",
                    recursiveMember: {
                        foo: "Foo2",
                        nested: {
                            bar: "Bar2"
                        }
                    }
                }
            }
        }
    }
])

apply RecursiveShapes @httpResponseTests([
    {
        id: "RpcV2CborRecursiveShapes",
        documentation: "Serializes recursive structures",
        protocol: rpcv2Cbor,
        code: 200,
        body: "v2ZuZXN0ZWS/Y2Zvb2RGb28xZm5lc3RlZL9jYmFyZEJhcjFvcmVjdXJzaXZlTWVtYmVyv2Nmb29kRm9vMmZuZXN0ZWS/Y2JhcmRCYXIy//////8=",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            nested: {
                foo: "Foo1",
                nested: {
                    bar: "Bar1",
                    recursiveMember: {
                        foo: "Foo2",
                        nested: {
                            bar: "Bar2"
                        }
                    }
                }
            }
        }
    },
    {
        id: "RpcV2CborRecursiveShapesUsingDefiniteLength",
        documentation: "Deserializes recursive structures encoded using a map with definite length",
        protocol: rpcv2Cbor,
        code: 200,
        body: "oWZuZXN0ZWSiY2Zvb2RGb28xZm5lc3RlZKJjYmFyZEJhcjFvcmVjdXJzaXZlTWVtYmVyomNmb29kRm9vMmZuZXN0ZWShY2JhcmRCYXIy"
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Content-Type": "application/cbor"
        },
        params: {
            nested: {
                foo: "Foo1",
                nested: {
                    bar: "Bar1",
                    recursiveMember: {
                        foo: "Foo2",
                        nested: {
                            bar: "Bar2"
                        }
                    }
                }
            }
        },
        appliesTo: "client"
    }
])

operation RecursiveShapes {
    input: RecursiveShapesInputOutput,
    output: RecursiveShapesInputOutput
}

structure RecursiveShapesInputOutput {
    nested: RecursiveShapesInputOutputNested1
}

structure RecursiveShapesInputOutputNested1 {
    foo: String,
    nested: RecursiveShapesInputOutputNested2
}

structure RecursiveShapesInputOutputNested2 {
    bar: String,
    recursiveMember: RecursiveShapesInputOutputNested1,
}

structure SimpleScalarStructure {
    trueBooleanValue: Boolean,
    falseBooleanValue: Boolean,
    byteValue: Byte,
    doubleValue: Double,
    floatValue: Float,
    integerValue: Integer,
    longValue: Long,
    shortValue: Short,
    stringValue: String,
    blobValue: Blob
}
