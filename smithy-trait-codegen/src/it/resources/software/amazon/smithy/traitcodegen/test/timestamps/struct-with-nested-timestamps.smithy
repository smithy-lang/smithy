$version: "2.0"

namespace test.smithy.traitcodegen

use test.smithy.traitcodegen.timestamps#structWithNestedTimestamps

@structWithNestedTimestamps(
    baseTime: "1985-04-12T23:20:50.52Z"
    dateTime: "1985-04-12T23:20:50.52Z"
    httpDate: "Tue, 29 Apr 2014 18:30:38 GMT"
    epochSeconds: 1515531081.123
)
structure myStruct {}
