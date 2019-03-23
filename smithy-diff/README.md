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


# Using diff tags

This library checks for traits with special tags to determine if adding,
removing, or changing a trait is a backward incompatible change. For
example, adding the `required` trait to a member is a breaking change
because it adds new requirements that did not previously exist.

This library checks for the following tags on trait definitions:

* `diff.error.add`: An error is emitted when the trait is added to a shape.
* `diff.error.remove`: An error is emitted when the trait is removed
  from a shape.
* `diff.error.update`: An error is emitted when the trait is updated or
  modified in some way.
* `diff.error.const`: An error is emitted when the trait is added, removed,
  or modified on a trait.

The following example defines a trait that is configures this library to emit
an error event when the trait is added to a shape.

```
namespace smithy.example

trait myTrait {
  selector: "*",
  tags: [diff.error.add],
}
```


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
