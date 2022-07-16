.. _aws-authentication:

=========================
AWS Authentication Traits
=========================

This document defines AWS authentication schemes.

.. smithy-trait:: aws.auth#sigv4
.. _aws.auth#sigv4-trait:

------------------------
``aws.auth#sigv4`` trait
------------------------

Trait summary
    The ``aws.auth#sigv4`` trait adds support for `AWS signature version 4`_
    to a service.
Trait selector
    ``service``
Trait value
    An ``object`` that supports the following properties:

    .. list-table::
        :header-rows: 1
        :widths: 10 20 70

        * - Property
          - Type
          - Description
        * - name
          - ``string``
          - **Required**. The signature version 4 service signing name to use
            in the `credential scope`_ when signing requests. This value MUST
            NOT be empty. This value SHOULD match the ``arnNamespace`` property
            of the :ref:`aws.api#service-trait`.

.. tabs::

    .. code-tab:: smithy

        namespace aws.fooBaz

        use aws.api#service
        use aws.auth#sigv4
        use aws.protocols#restJson1

        @service(sdkId: "Some Value")
        @sigv4(name: "foobaz")
        @restJson1
        service FooBaz {
            version: "2018-03-17",
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "aws.fooBaz#FooBaz": {
                    "type": "service",
                    "version": "2018-03-17",
                    "traits": {
                        "aws.protocols#restJson1": {},
                        "aws.api#service": {
                            "sdkId": "Some Value"
                        },
                        "aws.auth#sigv4": {
                            "name": "foobaz"
                        }
                    }
                }
            }
        }


.. smithy-trait:: aws.auth#unsignedPayload
.. _aws.auth#unsignedPayload-trait:

----------------------------------
``aws.auth#unsignedPayload`` trait
----------------------------------

Summary
    Indicates that the payload of an operation is not to be part of the
    signature computed for the request of an operation.
Trait selector
    ``operation``
Value type
    Annotation trait

Most requests sent to AWS services require that the payload of the request is
signed. However, in some cases, a service that streams large amounts of data
with an unknown size at the time a request is initiated might require that the
payload of a request is not signed.

The following example defines an operation that indicates the payload of the
operation MUST NOT be used as part of the request signature calculation:

.. tabs::

    .. code-tab:: smithy

        use aws.auth#unsignedPayload

        @unsignedPayload
        operation PutThings {
            input: PutThingsInput,
            output: PutThingsOutput
        }

    .. code-tab:: json

        {
            "smithy": "1.0",
            "shapes": {
                "smithy.example#PutThings": {
                    "type": "operation",
                    "input": {
                        "target": "smithy.example#PutThingsInput"
                    },
                    "output": {
                        "target": "smithy.example#PutThingsOutput"
                    },
                    "traits": {
                        "aws.auth#unsignedPayload": {}
                    }
                }
            }
        }


Unsigned Payloads and signature version 4
=========================================

Using an unsigned payload with `AWS signature version 4`_ requires that the
literal string ``UNSIGNED-PAYLOAD`` is used when constructing a
`canonical request`_, and the same value is sent in the
`x-amz-content-sha256`_ header when sending an HTTP request.


.. smithy-trait:: aws.auth#cognitoUserPools
.. _aws.auth#cognitoUserPools-trait:

-----------------------------------
``aws.auth#cognitoUserPools`` trait
-----------------------------------

Trait summary
    The ``aws.auth#cognitoUserPools`` trait adds support for
    `Amazon Cognito User Pools`_ to a service.
Trait selector
    ``service``
Trait value
    An ``object`` that supports the following properties:

    .. list-table::
        :header-rows: 1
        :widths: 10 20 70

        * - Property
          - Type
          - Description
        * - providerArns
          - ``[string]``
          - **Required**. A list of the Amazon Cognito user pool ARNs. Each
            element is of this format: ``arn:aws:cognito-idp:{region}:{account_id}:userpool/{user_pool_id}``.

.. code-block:: smithy

    namespace aws.fooBaz

    use aws.api#service
    use aws.auth#cognitoUserPools
    use aws.protocols#restJson1

    @service(sdkId: "Some Value")
    @cognitoUserPools(
        providerArns: ["arn:aws:cognito-idp:us-east-1:123:userpool/123"])
    @restJson1
    service FooBaz {
        version: "2018-03-17",
    }


.. _AWS signature version 4: https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html
.. _credential scope: https://docs.aws.amazon.com/general/latest/gr/sigv4-create-string-to-sign.html
.. _Amazon Cognito User Pools: https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-identity-pools.html
.. _canonical request: https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html
.. _x-amz-content-sha256: https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html
