# Smithy Diff

This module detects differences between two Smithy models, identifying
changes that are safe and changes that are backward incompatible.


# Example usage

The `ModelDiff#compare` method is used to compare two models.

```
Model modelA = loadModelA();
Model modelB = loadModelB();
List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);
```

Smithy Diff will load and apply metadata suppressions and severityOverrides
from the new model to any emitted diff events. 

# Adding a custom DiffEvaluator

This library finds all instances of `DiffEvaluator`
using the [Java service provider interface](https://docs.oracle.com/javase/tutorial/ext/basics/spi.html).

The following example creates a custom `DiffEvaluator`:

```java
package com.example;

import java.util.List;
import java.util.stream.Collectors;
import DiffEvaluator;
import Differences;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Creates a NOTE event when a shape is added named "Foo".
 */
public class MyAddedShape extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.addedShapes()
                .filter(shape -> shape.getId().getName().equals("Foo"))
                .map(shape -> note(shape, String.format(
                        "Shape `%s` of type `%s` was added to the model with the name Foo",
                        shape.getId(), shape.getType())))
                .collect(Collectors.toList());
    }
}
```

**Note**: You will need to register your provider in a `module-info.java`
file so that the Java module system knows about your implementation:

```java
import DiffEvaluator;

module com.example {
    requires software.amazon.smithy.diff;

    uses DiffEvaluator;

    provides DiffEvaluator with com.example.MyAddedShape;
}
```


# Reporting

Reporting and visualizing the detected differences is not handled by this
library. This library is only responsible for detecting differences and
returning a list of `ValidationEvent` values. Higher-level tooling like a
CLI or Web frontend should be used to implement reporting.

A very simple form of reporting can be implementing by dumping each
event to stdout:

```
List<ValidationEvent> events = ModelDiff.compare(modelA, modelB);
events.forEach(System.out::println);
```

# Deprecated: Diff tags

## Using diff tags

This library checks for traits with special tags to determine if adding,
removing, or changing a trait is a backward incompatible change. For
example, adding the `required` trait to a member is a breaking change
because it adds new requirements that did not previously exist.

This library checks for the following tags on trait definitions:

* `diff.error.add`: An error event is emitted when the trait is added to a
   shape.
* `diff.error.remove`: An error event is emitted when the trait is removed
  from a shape.
* `diff.error.update`: An error event is emitted when the trait is updated
  or modified in some way.
* `diff.error.const`: An error event is emitted when the trait is added,
  removed, or modified in some way.
* `diff.danger.add`: A danger event is emitted when the trait is added to a
   shape.
* `diff.danger.remove`: A danger event is emitted when the trait is removed
  from a shape.
* `diff.danger.update`: A danger event is emitted when the trait is updated
  or modified in some way.
* `diff.danger.const`: A danger event is emitted when the trait is added,
  removed, or modified in some way.
* `diff.warning.add`: A warning event is emitted when the trait is added to a
   shape.
* `diff.warning.remove`: A warning event is emitted when the trait is removed
  from a shape.
* `diff.warning.update`: A warning event is emitted when the trait is updated
  or modified in some way.
* `diff.warning.const`: A warning event is emitted when the trait is added,
  removed, or modified in some way.

The following example defines a trait that configures this library to emit
an error event when `myTrait` is added to a shape.

```
namespace smithy.example

@tags(["diff.error.add"])
@trait
structure myTrait {}
```

## Nested trait diffs

The `diff.contents` tag is used to diff the contents of a trait value based
on diff tags applied to members of shapes used to define a trait. For example,
if it is a breaking change to update some members of a trait but not others,
this can be automatically validated using the `diff.contents` tag. When
evaluating the contents of a trait, only tags applied to members of lists,
sets, maps (value only), structures, and unions are considered.

The following example defines a trait:

```
namespace smithy.example

@aTrait(foo: "hi", baz: {foo: "bye"})
string Foo

// This tag is required on the trait definition in order to perform
// nested diff contents evaluation. It is also an error to add this
// trait to an existing shape because of the `diff.error.add` tag.
@tags(["diff.error.add", diff.contents"])
@trait
structure aTrait {
    // It is an error to remove this member from a trait value.
    @tags(["diff.error.remove"])
    foo: String,

    // It is a warning to add/remove/update this member on a trait value.
    @tags(["diff.warning.const"])
    bar: String,

    // It's fine to add or remove this member.
    baz: NestedTraitStruct
}

structure NestedTraitStruct {
    // This nested trait value is also validated. A value provided for
    // This member cannot be added removed or changed.
    @tags(["diff.error.const"])
    a: String,

    // This member can be added or removed in any way whe used as
    // part of a value for aTrait.
    b: String,
}
```

Nested diffs also works with lists and sets, when diff tags are applied
to members of lists and sets. For example, a trait could require that the
order of values is a backward compatibility contract; adding a value to the
beginning of a list or changing a value in a list is a breaking change.
The following trait only allows values to be appended to the `foo` list:

```
namespace smithy.example

// It is an error to remove this trait, and there are addional
// checks performed on nested trait values.
@tags(["diff.error.remove", "diff.contents"])
@trait
structure listTrait {
    // You can't remove this value, but you can append to it.
    @tags(["diff.error.remove"])
    foo: ListTraitFoos,
}

list ListTraitFoos {
    // A given member in a list must not change its position in the
    // list nor can the value of a list member change. You can only
    // append new values to the list.
    @tags(["diff.danger.const"])
    member: String,
}
```
