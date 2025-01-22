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
            of the :ref:`aws.api#service-trait` if present and the ``name``
            property of the :ref:`aws.auth#sigv4a-trait` if present.

If a request contains the ``Authorization`` header or a query string parameter
with the name of ``X-Amz-Algorithm`` containing the value ``AWS4-HMAC-SHA256``,
the request will undergo authentication and be rejected if it fails. Otherwise,
if the :ref:`optionalAuth-trait` is applied, the service shall operate on the
unauthenticated request.

.. code-block:: smithy

    $version: "2"

    namespace aws.fooBaz

    use aws.api#service
    use aws.auth#sigv4
    use aws.protocols#restJson1

    @service(sdkId: "Some Value")
    @sigv4(name: "foobaz")
    @restJson1
    service FooBaz {
        version: "2018-03-17"
    }


.. smithy-trait:: aws.auth#sigv4a
.. _aws.auth#sigv4a-trait:

-------------------------
``aws.auth#sigv4a`` trait
-------------------------

Trait summary
    The ``aws.auth#sigv4a`` trait adds support for
    `AWS Signature Version 4 Asymmetric (SigV4A)`_, an extension of
    `AWS signature version 4`_ (SigV4), to a service.
Trait selector
    ``service[trait|aws.auth#sigv4]``
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
          - **Required**. The signature version 4a service signing name to use
            in the `credential scope`_ when signing requests. This value MUST
            NOT be empty. This value SHOULD match the ``arnNamespace`` property
            of the :ref:`aws.api#service-trait` if present and the ``name``
            property of the :ref:`aws.auth#sigv4-trait`.

SigV4A is nearly identical to SigV4, but also uses public-private keys and
asymmetric cryptographic signatures for every request. Most notably, SigV4A
supports signatures for multi-region API requests.

.. code-block:: smithy

    $version: "2"

    namespace aws.fooBaz

    use aws.api#service
    use aws.auth#sigv4
    use aws.auth#sigv4a
    use aws.protocols#restJson1

    // This service is an AWS service that prioritizes SigV4A
    // authentication before SigV4 authentication.
    // Note that services that support SigV4A MUST support SigV4.
    @service(sdkId: "Some Value")
    @auth([sigv4a, sigv4])
    @sigv4(name: "foobaz")
    @sigv4a(name: "foobaz")
    @restJson1
    service FooBaz {
        version: "2018-03-17"
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

.. code-block:: Smithy

    $version: "2"

    use aws.auth#unsignedPayload

    @unsignedPayload
    operation PutThings {
        input: PutThingsInput
        output: PutThingsOutput
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

    $version: "2"

    namespace aws.fooBaz

    use aws.api#service
    use aws.auth#cognitoUserPools
    use aws.protocols#restJson1

    @service(sdkId: "Some Value")
    @cognitoUserPools(
        providerArns: ["arn:aws:cognito-idp:us-east-1:123:userpool/123"])
    @restJson1
    service FooBaz {
        version: "2018-03-17"
    }


.. _AWS signature version 4: https://docs.aws.amazon.com/general/latest/gr/signature-version-4.html
.. _AWS Signature Version 4 Asymmetric (SigV4A): https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_sigv.html#how-sigv4a-works
.. _credential scope: https://docs.aws.amazon.com/general/latest/gr/sigv4-create-string-to-sign.html
.. _Amazon Cognito User Pools: https://docs.aws.amazon.com/cognito/latest/developerguide/cognito-user-identity-pools.html
.. _canonical request: https://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html
.. _x-amz-content-sha256: https://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html
