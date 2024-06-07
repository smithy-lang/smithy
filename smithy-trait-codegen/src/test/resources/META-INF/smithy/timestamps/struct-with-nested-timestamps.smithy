$version: "2.0"

namespace test.smithy.traitcodegen.timestamps

@trait
structure structWithNestedTimestamps {
    @required
    baseTime: basicTimestamp

    @required
    dateTime: dateTimeTimestamp

    @required
    httpDate: httpDateTimestamp

    @required
    epochSeconds: epochSecondsTimestamp
}

@private
timestamp basicTimestamp

@private
@timestampFormat("date-time")
timestamp dateTimeTimestamp

@private
@timestampFormat("http-date")
timestamp httpDateTimestamp

@private
@timestampFormat("epoch-seconds")
timestamp epochSecondsTimestamp
