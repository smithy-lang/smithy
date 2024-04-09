# URI Pattern Validation and Conflict Resolution

[**Draft**]

**Author**:  Manuel Sugawara
**Created**: 2022/12/13

## Overview

This document proposes changing the Smithy spec to relax the rule we
use to validate HTTP bindings to avoid conflicts. Instead of avoiding
conflicts we will require servers to implement a well-defined way to
route requests for ambiguous URI that match two or more patterns by
using the most specific route.

## Motivation

Smithy supports defining HTTP bindings using path patterns with
optional labels, placeholders that can be used to capture segments of
the path to be passed to the operation handler, and query parameters
of the Unified Resource Identifiers (URI’s), however this feature has
been restricted to avoid conflicting URIs, in particular the rule
number 5 in the Smithy spec


> A label and a literal SHOULD NOT both occupy the same segment in patterns which are equivalent to that point if they share a host.


Removing or relaxing this rule has been a frequent request from
customers given that it restricts the way service owners can model
their API by disallowing the definition of path hierarchies that makes
the most sense for their business logic. The proposal is to remove
this constraint to allow the use of URI path patterns that are not
ambiguous but that are not currently supported, such as:

* `/abc/{xyz}` and
* `/abc/{xyz}/cde`

and also those that can be ambiguous such as 

* `/abc/bcd/{xyz}`
* `/abc/{xyz}/cde`

while also defining a set of rules to unambiguously route requests
when two or more path patterns match a given request based on the
*specificity* of the path, which can be loosely defined as “a path
with a non-label segment is considered more specific than one with a
label segment in the same position”.

Given that there is no reasonable way to distinguish *equivalent* path
patterns (i.e., that look the same except for the name of the labels)
we will disallow them, for instance,

* `/abc/{foo}/cde`
* `/abc/{bar}/cde`

## Proposal

### Specificity Routing

The proposal is to  accept any non-equivalent URI path patterns and
also specify the expected routing algorithm. The routing algorithm
will choose the *best-ranked* match using the specificity of the path
and literal query parameters when present. The Smithy documentation
will be changed in the following way


>A label and a literal MAY both occupy the same segment in patterns that are equivalent to that point if they share a host. Server implementations MUST route ambiguous requests to the operation with the most specific URI path.


And the following algorithm to compare two paths will be added to the
specification

Given two ambiguous URI patterns `A` and `B` with segments
`[A0, …, An]` and `[B0, …, Bm]` with query string literals
`[AQ0, …, AQp]` and `[BQ0, …, BQq]` (with both `p` and `q` possibly
zero, i.e., without query string literals), we use the following
algorithm to compare them, for each index `x` from `0` to `min(n, m)`

1. If `A[x]` and `B[x]` are both literals then continue (the literal values have to be equal otherwise the patterns would not be ambiguous)
2. If `A[x]` is a literal and `B[x]` is a label then `A` is more specific than `B`, 
3. If `A[x]` is a non-greedy label and `B[x]` is a greedy label then `A` is more specific than `B`
4. If `n > m` then `A` is more specific than `B`
5. if `p > q` then `A` is more specific than `B`

#### Routing Example 1

Given the following path patterns

1. `/abc/bcd/{xyz}` 
2. `/abc/{xyz}/cde` 
3.  `/{xyz}/bcd/cde`

This is how we will route different path requests

* `/abc/bcd/cde` will route to pattern number 1 (ambiguous with numbers 2 and 3)
* `/abc/**foo**/cde` will route to pattern number 2 (ambiguous with number 3)
* `/**foo**/bcd/cde` will route to pattern number 3 (non-ambiguous)

**Routing Example 2**

Given the following path patterns

1. `/abc/bcd/{xyz}` 
2. `/abc/{xyz}/cde` 
3.  `/{xyz}/bcd/cde?def=efg`

This is how we will route different path requests

* `/abc/bcd/cde?def=efg` will route to pattern number 1 (ambiguous with numbers 2 and 3, notice that path specificity wins over query string literals)
* `/abc/`**`foo`**`/cde?def=efg` will route to pattern number 2 (ambiguous with number 3)
* `/`**`foo`**`/bcd/cde?def=efg` will route to pattern number 3 (non-ambiguous)

**Routing Example 3**

1. `/abc/{xyz+}/bcd` 
2. `/abc/{xyz+}` 

This is how we will route different path requests

* `/abc/**foo**/**bar**/bcd` will route to pattern number 1 (ambiguous with number 2)
* `/abc/foo/bar/baz` will route to pattern number 2 (non-ambiguous)

## Appendix

### Compatibility with API Gateway

API Gateway also uses
[specificity](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-routes.html)
to route requests, so this proposal will be mostly compatible with API
Gateway. There is a catch to this: currently Smithy supports URI
patterns with
[query string literals](https://awslabs.github.io/smithy/2.0/spec/http-bindings.html#query-string-literals)
that are used for matching requests. For API Gateway, query parameters
are not used to match operations and while query parameters can be
made in theory
[required with a constant value](https://swagger.io/docs/specification/describing-parameters/#constant)
(notice that API Gateway can validate that a required query param is
present but does not validate the value itself) the response status
might be different.

