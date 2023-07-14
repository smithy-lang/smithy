# Defining Smithy Timestamps

* **Author**: Michael Dowling
* **Created**: 2023-07-14

# Abstract

This document defines the missing details of Smithy timestamp shapes that will
help ensure consistent implementations across programming languages, including
the temporal resolution and range of allowable dates.

## Motivation

The timestamp shape is a protocol agnostic instant in time; its data type in
code does not change based on the protocol bindings or serialized format of the
timestamp. Smithy's timestamp shape does not currently define critical details
that implementations need to consistently implement Smithy across programming
languages, including the temporal resolution of a timestamp or the supported
date range. This lack of specification risks incompatibilities across
implementations.

## Proposal

A timestamp represents an instant in time in the proleptic Gregorian calendar,
independent of local times or timezones. Timestamps support an allowable date
range between midnight January 1, 0001 CE to 23:59:59.999 on
December 31, 9999 CE, with a temporal resolution of 1 millisecond. This
resolution and range ensures broad support across programming languages and
guarantees compatibility with [RFC 3339](https://www.rfc-editor.org/rfc/rfc3339.html).

### Rationale

Determining the temporal resolution and allowable range of a Smithy timestamp is
difficult because we want to ensure broad compatibility across programming
languages. Smithy protocols today support an undefined fractional precision for
many timestamp formats, which in practice when calling AWS APIs is millisecond
precision. This implies that Smithy timestamps should at least support
millisecond precision, but it does not imply a minimum and maximum date. To
determine an appropriate range, we observe the following programming language
date/time types:

* .NET
  - [DateTime](https://learn.microsoft.com/en-us/dotnet/api/system.datetime?view=net-7.0):
    Nanosecond precision (every 100 nanosecond "ticks"), with a range of
    January 1, 0001 through December 31, 9999.
* C++: [chrono time_point](https://en.cppreference.com/w/cpp/chrono/time_point):
  Seems to allow any resolution, calendar, or date range.
* Go: [Time](https://pkg.go.dev/time#Time): Nanosecond precision, using a
  signed int to represent years.
* Java:
    * [Instant](https://docs.oracle.com/javase/8/docs/api/java/time/Instant.html):
      Nanosecond precision, ranging from -1000000000-01-01 to 1000000000-12-31.
    * [Date](https://docs.oracle.com/javase/8/docs/api/java/util/Date.html):
      Millisecond precision with a range defined by a Long, allowing for dates
      in the distant past and future.
* JavaScript: [Date](https://262.ecma-international.org/5.1/#sec-15.9.1.1):
  Millisecond precision. Can store ±8,640,000,000,000,000 milliseconds, which is
  based on and slightly less than the maximum integral number that can be safely
  represented by a JavaScript number. This provides a range from April 20,
  271821 BCE to September 13, 275760 CE.
* Kotlin: Java's Instant or [kotlinx.Instant](https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime/-instant/):
  Nanosecond precision, with an undocumented range.
* PHP: [DateTime](https://www.php.net/manual/en/intro.datetime.php):
  Microsecond precision, "The range is from about 292 billion years in the past
  to the same in the future".
* Python: [datetime](https://docs.python.org/3/library/datetime.html):
  Microsecond precision, ranging from 0001-01-01T00:00:00Z to 9999-12-31T23:59:
  59.999999Z.
* Ruby: [DateTime](https://ruby-doc.org/stdlib-2.6.1/libdoc/date/rdoc/DateTime.html):
  Nanosecond precision, with an undocumented range.
* Swift: [Date](https://developer.apple.com/documentation/foundation/date)
  / [NSDate](https://developer.apple.com/documentation/foundation/nsdate):
  "sub-millisecond" precision, with the underlying representation using a Double
  to contain sections and fractions of a second.

Millisecond precision is the most broadly supported. Setting the default
precision of timestamps to nanoseconds rather than milliseconds would
immediately render the AWS SDK for PHP, JavaScript, Botocore, and many Amazon
services incompatible with the requirement. Languages without nanosecond
support would need to create custom data structures to represent timestamps
that are incompatible with the rest of their ecosystem.

Python and .NET's date ranges are the most limited. However, this range likely
captures most of the use cases developers would expect from Smithy timestamps
(i.e., typically representing nearby instants in time), and also ensures
timestamp values are compatible with
[RFC 3339 dates](https://www.ietf.org/rfc/rfc3339.txt).

### Protocol limitations

While Smithy timestamps can store timestamps with millisecond precision, not all
protocols support serializing timestamps with this precision. For example,
the `http-date` timestamp format does not support anything beyond second
precision, which can result in a loss of precision when transmitting a
timestamp. Modelers should be aware of the limitations of timestamp formats when
defining Smithy models.

This proposal will update the specification of Smithy's
existing [timestamp formats](https://smithy.io/2.0/spec/protocol-traits.html#timestampformat-trait)
to document the representable precision and behavior when a timestamp is too
granular.

* `http-date`: Second precision with no fractional seconds. A deserializer that
  encounters an `http-date` timestamp with fractional precision SHOULD fail to
  deserialize the value (for example, an HTTP server SHOULD return a 400 status
  code).
* `date-time`: Millisecond precision. Values that are more granular than
  millisecond precision SHOULD be truncated to fit millisecond precision.
* `epoch-seconds`: Millisecond precision. Values that are more granular than 
  millisecond precision SHOULD be truncated to fit millisecond precision.

#### Protocol limitations rationale

Because the resolution of a Smithy timestamp was previously undefined,
truncating timestamps that are too granular rather than failing to deserialize
them ensures that existing and new implementations can interoperate. Truncating
overly precise times also keeps the door open in case support for nanosecond
precision is needed in the future (e.g., using a hypothetical
`@resolution("nanos")` trait).

### Timestamp representation in code

Implementations MUST NOT directly expose the serialized value of a timestamp.
Timestamps are an abstraction to represent an instant in time, and the
serialization format of a timestamp can vary depending on the protocol
or `@timestampFormat` trait of the shape. Because Smithy is protocol agnostic,
changing the serialization format of a shape MUST NOT have any impact on the
representation of that shape in code.

When generating code, implementations MUST use a type for Smithy timestamp
shapes that can hold *at least* the minimum and maximum supported values using
millisecond precision. Implementations MAY use a type that represents dates with
a wider range or temporal resolution than Smithy's timestamp shape.
Implementations SHOULD use an idiomatic date/time data structure for the
target environment, though they may need to use an alternative data structure
to represent Smithy timestamps if they do not have access to a date/time type
with at least millisecond precision spanning the supported range.

Some programming language types offer more granularity than millisecond
precision (e.g., Java's `Instant`). Using such data types to represent a Smithy
timestamp is fine as long as the timestamp is serialized according to any
limitations defined in the protocol and any `@timestampFormat` trait applied to
the timestamp. Implementations SHOULD NOT refuse to serialize a timestamp due to
an overly granular value, and instead they should serialize the timestamp
according to the limitations of the protocol and model.

## Alternatives and tradeoffs

### Use nanosecond resolution

Standardizing on nanoseconds would make it more difficult for JavaScript (ms),
PHP (us), Python (us), and to an extent .NET to losslessly represent timestamps.
Smithy implementations for these programming languages would need to choose
whether they should use a lossy but idiomatic data type when working with
timestamps, or if they want to create their own data structure.

Languages without nanosecond support could provide a light-weight data structure
that easily converts to idiomatic date/time types. This kind of abstraction
SHOULD NOT be created for languages that already provide compatible date/time
types. A drawback of such a type is that a Smithy timestamp would be
incompatible with the rest of the language's date/time types (e.g, intervals).
Any date/time-based logic would have to be handled manually by end-users.

Finally, nanosecond precision is not currently supported by most of AWS, which
supports millisecond precision.
