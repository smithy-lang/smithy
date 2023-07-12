#################################################
Smithy Server Generator for TypeScript validation
#################################################

Smithy defines a set of :ref:`constraint traits <constraint-traits>` that can be used to restrict the set of allowed
values for a shape. These traits do not change the type generated; the type will always correspond with the Smithy type.
This allows Smithy models to evolve over time, so that model changes will not cause changes that break type checks.

Smithy server SDKs will only validate input values against constraint traits. While output structures can have
constraint traits applied, they will never be evaluated by the server SDK directly. This is because the failure would
be too late in the process. At the point of validation, the server SDKs could only return an
:ref:`InternalFailureException <TS SSDK internal-failure-exception>` to the user, and changes made by the operation
implementation would neither be returned to the user nor rolled back.

Default validation
==================

The server SDK validates inputs by default if every operation in the service has the
`smithy.framework#ValidationException`_ error associated. This can be accomplished by associating the error directly
with the :ref:`service <service>`, as well. In this mode, the constraint traits built into Smithy are applied to each
input and an error is generated and automatically returned if any are violated. ``ValidationException`` contains an
entry for each constraint violation encountered in the input, along with a pointer to the modeled member that failed
validation, so that user interfaces can associate these validation errors with input more easily.

When default validation is enabled and validation fails, the operation is not invoked, and there is no opportunity given
to the developer to customize the format of the error, to suppress certain errors in certain conditions, or to apply
constraints that are not supported directly by Smithy.

If any operation is not associated with ``smithy.framework#ValidationException``, the server SDK will refuse to generate
code unless default validation is disabled.

Custom validation
=================

For advanced use-cases, developers can choose to customize the validation behavior of the server SDK. This requires
setting ``disableDefaultValidation`` to ``true`` in ``smithy-build.json`` for the ``typescript-ssdk-codegen`` plugin:

.. code-block:: json

      "plugins": {
        "typescript-ssdk-codegen": {
          "package": "@aws-smithy/example",
          "packageVersion": "1.0.0-alpha.1",
          "disableDefaultValidation": true
        }
      }

When default validation is disabled, every input is still validated against the model's constraints. Instead of
short-circuiting and returning an error, however, the server SDK instead requires the developer to decide what to do
with the validation failures encountered. In order to support this, the server SDK adds a required function parameter
of type ``ValidationCustomizer`` to every handler factory:

.. code-block:: typescript

    export const getExampleOperationHandler = <Context>(
      operation: Operation<ExampleOperationServerInput, ExampleOperationServerOutput, Context>,
      customizer: ValidationCustomizer<"ExampleOperation">
    ): ServiceHandler<Context, HttpRequest, HttpResponse> => {

Where ``ValidationCustomizer`` is an interface built in to ``@aws-smithy/server-common`` for the purpose of customizing
validation failures:

.. code-block:: typescript

    export type ValidationFailure =
      | EnumValidationFailure
      | LengthValidationFailure
      | PatternValidationFailure
      | RangeValidationFailure
      | RequiredValidationFailure
      | UniqueItemsValidationFailure;

    export interface ValidationContext<O extends string> {
      operation: O;
    }

    export type ValidationCustomizer<O extends string> = (
      context: ValidationContext<O>,
      failures: ValidationFailure[]
    ) => ServiceException | undefined;

If the developer-supplied ``ValidationCustomizer`` returns ``undefined``, then the handler will continue executing the
operation, essentially suppressing validation failures. While this is generally not a good idea, it can be useful in
cases where new constraints are being evaluated for backwards compatibility, and the service wants to log certain
validation failures instead of returning errors.

The developer-supplied ``ValidationCustomizer`` can also return a code-generated exception extending
``ServiceException``. This will cause the operation to not be executed, and an error rendered instead.

.. warning::

    Server SDKs will not return an error from an operation without the error being associated with the service or
    operation. If you return a code-generated error that is not associated with the operation in question, the handler
    will render an :ref:`InternalFailureException <TS SSDK internal-failure-exception>` instead.

For example, consider a service that, for backwards compatibility purposes, needs to return a ``BadInput`` error:

.. code-block:: smithy

    @restJson1
    service WeatherService {
        version: "2018-05-10",
        operations: [GetForecast],
        errors: [BadInput]
    }

    @httpError(400)
    @error("client")
    structure BadInput {
        message: String,
    }

After disabling default validation in ``smithy-build.json``:

.. code-block:: json

      "plugins": {
        "typescript-ssdk-codegen": {
          "package": "@example/weather-service",
          "packageVersion": "1.0.0-alpha.1",
          "disableDefaultValidation": true
        }
      }

the developer can still use Smithy's built in validation, but return ``BadInput`` instead of ``ValidationException``:

.. code-block:: typescript

    const handler = getGetForecastHandler(async (input) => getForecast(input),
        (ctx, failures) => {
            return new BadInputError(`${failures.length} bad inputs detected.`);
        };
    });

.. _smithy.framework#ValidationException: https://github.com/smithy-lang/smithy/blob/main/smithy-validation-model/model/smithy.framework.validation.smithy#L9
