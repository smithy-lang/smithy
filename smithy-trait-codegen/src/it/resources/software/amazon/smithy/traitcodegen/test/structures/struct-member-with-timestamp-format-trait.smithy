$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.structures#StructMemberWithTimestampFormat

@StructMemberWithTimestampFormat(
    memberDateTime: "1985-04-12T23:20:50.52Z"
    memberHttpDate: "Tue, 29 Apr 2014 18:30:38 GMT"
    memberEpochSeconds: 1515531081.123
)
structure myStruct {}
