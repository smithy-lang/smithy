$version: "2.0"

namespace smithy.protocoltests.corpus

use smithy.protocols#rpcv2Cbor

// Only the event stream mixin is claimed. The other corpus mixins are not
// included because rpcv2Cbor does not support document types and because this
// service carries no non-event-stream test cases; the pre-existing
// model/rpcv2Cbor/ corpus covers the buffered shape serialization.
@rpcv2Cbor
@title("RpcV2 CBOR Corpus Protocol Tests")
service RpcV2CborCorpusTests with [EventStreamProtocolTestService] {}
