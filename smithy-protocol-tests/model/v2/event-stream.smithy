$version: "2.0"

namespace smithy.protocoltests.corpus

// EventStreamProtocolTestService covers event stream framing.
//
// Its operations and shape combinations are largely meant to mimic that of
// CoreProtocolTestService.
@mixin
service EventStreamProtocolTestService {
    operations: [
        EventStreamResponse
        EventStreamResponseBlobPayload
        EventStreamResponseHeaders
        EventStreamResponseImplicitPayload
        EventStreamError
        EventStreamRequest
        EventStreamInitialRequest
        EventStreamInitialResponse
        EventStreamDuplex
    ]
}

operation EventStreamResponse {
    input: EventStreamResponseInput
    output: EventStreamResponseOutput
}

structure EventStreamResponseInput {}

structure EventStreamResponseOutput {
    @httpPayload
    @required
    events: ResponseEventStream
}

@streaming
union ResponseEventStream {
    scalarsEvent: ScalarsEvent
    listEvent: ListEvent
    mapEvent: MapEvent
    structEvent: StructEvent
    unionEvent: UnionEvent
    heartbeatEvent: HeartbeatEvent
}

structure ScalarsEvent {
    booleanMember: Boolean
    byteMember: Byte
    shortMember: Short
    integerMember: Integer
    longMember: Long
    floatMember: Float
    doubleMember: Double
    bigIntegerMember: BigInteger
    bigDecimalMember: BigDecimal
    stringMember: String
    blobMember: Blob
    timestampMember: NoFormatTimestamp
    stringEnum: CorpusStringEnum
    intEnum: CorpusIntEnum
}

structure ListEvent {
    strings: StringList
    integers: IntegerList
    timestamps: TimestampList
}

structure MapEvent {
    strings: StringMap
    integers: IntegerMap
    timestamps: TimestampMap
}

structure StructEvent {
    value: SimpleStruct
}

structure UnionEvent {
    value: CorpusUnion
}

structure MessageEvent {
    content: String
    sequence: Integer
}

structure HeartbeatEvent {}

operation EventStreamResponseBlobPayload {
    input: EventStreamResponseBlobPayloadInput
    output: EventStreamResponseBlobPayloadOutput
}

structure EventStreamResponseBlobPayloadInput {}

structure EventStreamResponseBlobPayloadOutput {
    @httpPayload
    @required
    events: BlobPayloadEventStream
}

@streaming
union BlobPayloadEventStream {
    blobEvent: BlobPayloadEvent
}

structure BlobPayloadEvent {
    @eventHeader
    contentType: String

    @eventPayload
    data: Blob
}

operation EventStreamResponseHeaders {
    input: EventStreamResponseHeadersInput
    output: EventStreamResponseHeadersOutput
}

structure EventStreamResponseHeadersInput {}

structure EventStreamResponseHeadersOutput {
    @httpPayload
    @required
    events: HeaderEventStream
}

@streaming
union HeaderEventStream {
    headerEvent: HeaderEvent
}

structure HeaderEvent {
    @eventHeader
    stringHeader: String

    @eventHeader
    integerHeader: Integer

    @eventHeader
    booleanHeader: Boolean

    @eventHeader
    longHeader: Long

    @eventHeader
    timestampHeader: NoFormatTimestamp

    @eventHeader
    blobHeader: Blob

    @eventPayload
    body: String
}

operation EventStreamResponseImplicitPayload {
    input: EventStreamResponseImplicitPayloadInput
    output: EventStreamResponseImplicitPayloadOutput
}

structure EventStreamResponseImplicitPayloadInput {}

structure EventStreamResponseImplicitPayloadOutput {
    @httpPayload
    @required
    events: ImplicitPayloadEventStream
}

@streaming
union ImplicitPayloadEventStream {
    dataEvent: ImplicitPayloadEvent
}

structure ImplicitPayloadEvent {
    @eventHeader
    requestId: String

    content: String

    count: Integer

    nested: SimpleStruct
}

operation EventStreamError {
    input: EventStreamErrorInput
    output: EventStreamErrorOutput
}

structure EventStreamErrorInput {}

structure EventStreamErrorOutput {
    @httpPayload
    @required
    events: ErrorEventStream
}

@streaming
union ErrorEventStream {
    messageEvent: MessageEvent
    streamError: StreamError
}

@error("server")
structure StreamError {
    message: String
    code: Integer
}

operation EventStreamRequest {
    input: EventStreamRequestInput
    output: EventStreamRequestOutput
}

structure EventStreamRequestInput {
    @httpPayload
    @required
    events: RequestEventStream
}

structure EventStreamRequestOutput {}

@streaming
union RequestEventStream {
    sendMessage: SendMessageEvent
    endStream: EndStreamEvent
}

structure SendMessageEvent {
    content: String
}

structure EndStreamEvent {}

operation EventStreamInitialResponse {
    input: EventStreamInitialResponseInput
    output: EventStreamInitialResponseOutput
}

structure EventStreamInitialResponseInput {}

structure EventStreamInitialResponseOutput {
    @httpHeader("X-Session-Id")
    sessionId: String

    @httpHeader("X-Timeout")
    timeout: Integer

    @httpPayload
    events: ResponseEventStream
}

operation EventStreamInitialRequest {
    input: EventStreamInitialRequestInput
    output: EventStreamInitialRequestOutput
}

structure EventStreamInitialRequestInput {
    @httpHeader("X-Request-Id")
    requestId: String

    @httpHeader("X-Priority")
    priority: Integer

    @httpPayload
    events: RequestEventStream
}

structure EventStreamInitialRequestOutput {}

operation EventStreamDuplex {
    input: EventStreamDuplexInput
    output: EventStreamDuplexOutput
}

structure EventStreamDuplexInput {
    @httpHeader("X-Request-Id")
    requestId: String

    @httpPayload
    events: RequestEventStream
}

structure EventStreamDuplexOutput {
    @httpHeader("X-Session-Id")
    sessionId: String

    @httpPayload
    events: ResponseEventStream
}
