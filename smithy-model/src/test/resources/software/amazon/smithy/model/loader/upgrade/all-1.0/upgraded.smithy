$version: "2.0"

namespace smithy.example

structure Bytes {
    nullable: Byte,

    @default(0)
    nonNull: Byte,

    nullable2: Byte,
}

structure Shorts {
    nullable: Short,

    @default(0)
    nonNull: Short,

    nullable2: Short,
}

structure Integers {
    nullable: Integer,

    @default(0)
    nonNull: Integer,

    nullable2: Integer
}

structure BlobPayload {
    @default("")
    payload: StreamingBlob
}

@streaming
blob StreamingBlob
