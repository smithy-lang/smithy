$version: "2"

namespace smithy.example

@longPoll(timeoutMillis: 70000)
operation LongPollWithTimeout {}

@longPoll(timeoutMillis: 0)
operation LongPollWithZeroTimeout {}

@longPoll(timeoutMillis: -1)
operation LongPollWithNegativeTimeout {}
