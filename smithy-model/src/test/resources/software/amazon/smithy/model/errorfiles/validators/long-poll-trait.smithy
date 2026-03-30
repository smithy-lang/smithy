$version: "2"

namespace smithy.example

@longPoll
operation LongPollWithoutTimeout {}

@longPoll(timeoutMillis: 70000)
operation LongPollWithTimeout {}
