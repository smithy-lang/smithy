$version: "2"

namespace smithy.example

@longPoll(timeoutMillis: 70000)
operation LongPollWithTimeout {}
