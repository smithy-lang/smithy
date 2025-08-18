$version: "2.0"

namespace test.smithy.traitcodegen.structures

@trait
structure StructMemberWithTimestampFormat {
    @timestampFormat("date-time")
    memberDateTime: Timestamp

    @timestampFormat("http-date")
    memberHttpDate: Timestamp

    memberEpochSeconds: MemberEpochSeconds

    memberList: TimeStampList

    memberMap: StringTimeStampMap
}

@timestampFormat("epoch-seconds")
timestamp MemberEpochSeconds

list TimeStampList {
    @timestampFormat("http-date")
    member: Timestamp
}

map StringTimeStampMap {
    key: String
    @timestampFormat("http-date")
    value:Timestamp
}
