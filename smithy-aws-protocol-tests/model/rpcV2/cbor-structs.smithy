$version: "2.0"

namespace aws.protocoltests.rpcv2Cbor

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
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        }
        method: "POST",
        bodyMediaType: "application/cbor",
        uri: "/service/aws.protocoltests.rpcv2Cbor.RpcV2Protocol/operation/SimpleScalarProperties",
        body: "v2lieXRlVmFsdWUFa2RvdWJsZVZhbHVl+z/+OVgQYk3TcWZhbHNlQm9vbGVhblZhbHVl9GpmbG9hdFZhbHVl+kDz989saW50ZWdlclZhbHVlGQEAaWxvbmdWYWx1ZRkmkWpzaG9ydFZhbHVlGSaqa3N0cmluZ1ZhbHVlZnNpbXBsZXB0cnVlQm9vbGVhblZhbHVl9f8="
        params: {
            trueBooleanValue: true,
            falseBooleanValue: false,
            byteValue: 5,
            doubleValue: 1.889,
            floatValue: 7.624,
            integerValue: 256,
            shortValue: 9898,
            longValue: 9873,
            stringValue: "simple"
        }
    },
    {
        id: "RpcV2CborClientDoesntSerializeNullStructureValues",
        documentation: "RpcV2 Cbor should not serialize null structure values",
        protocol: rpcv2Cbor,
        method: "POST",
        uri: "/service/aws.protocoltests.rpcv2Cbor.RpcV2Protocol/operation/SimpleScalarProperties",
        body: "v/8=",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        }
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
        uri: "/service/aws.protocoltests.rpcv2Cbor.RpcV2Protocol/operation/SimpleScalarProperties",
        body: "v2tzdHJpbmdWYWx1Zfb/",
        bodyMediaType: "application/cbor",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        }
        params: {},
        appliesTo: "server"
    },
    {
        id: "RpcV2CborSupportsNaNFloatInputs",
        protocol: rpcv2Cbor,
        documentation: "Supports handling NaN float values.",
        headers: {
            "smithy-protocol": "rpc-v2-cbor",
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        }
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
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        }
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
            "Accept": "application/cbor",
            "Content-Type": "application/cbor"
        }
        method: "POST",
        bodyMediaType: "application/cbor",
        uri: "/service/RpcV2Protocol/operation/SimpleScalarProperties",
        body: "v2tkb3VibGVWYWx1Zfv/8AAAAAAAAGpmbG9hdFZhbHVl+v+AAAD/"
        params: {
            doubleValue: "-Infinity",
            floatValue: "-Infinity"
        }
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
        body: "v2lieXRlVmFsdWUFa2RvdWJsZVZhbHVl+z/+OVgQYk3TcWZhbHNlQm9vbGVhblZhbHVl9GpmbG9hdFZhbHVl+kDz989saW50ZWdlclZhbHVlGQEAaWxvbmdWYWx1ZRkmkWpzaG9ydFZhbHVlGSaqa3N0cmluZ1ZhbHVlZnNpbXBsZXB0cnVlQm9vbGVhblZhbHVl9f8=",
        code: 200,
        params: {
            trueBooleanValue: true,
            falseBooleanValue: false,
            byteValue: 5,
            doubleValue: 1.889,
            floatValue: 7.624,
            integerValue: 256,
            shortValue: 9898,
            stringValue: "simple"
        }
    },
    {
        id: "RpcV2CborClientDoesntDeSerializeNullStructureValues",
        documentation: "RpcV2 Cbor should deserialize null structure values",
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
            "Accept": "application/cbor",
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
}