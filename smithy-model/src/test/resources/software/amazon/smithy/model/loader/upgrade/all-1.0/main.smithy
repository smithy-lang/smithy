$version: "1.0"

namespace smithy.example

structure Bytes {
    nullable: Byte,

    nonNull: PrimitiveByte,

    @box
    nullable2: PrimitiveByte,
}

structure Shorts {
    nullable: Short,

    nonNull: PrimitiveShort,

    @box
    nullable2: PrimitiveShort,
}

structure Integers {
    nullable: Integer,

    nonNull: PrimitiveInteger,

    @range(min: 0, max: 1)
    ranged: PrimitiveInteger,

    @box
    nullable2: PrimitiveInteger
}

structure BlobPayload {
    payload: StreamingBlob
}

@streaming
blob StreamingBlob
