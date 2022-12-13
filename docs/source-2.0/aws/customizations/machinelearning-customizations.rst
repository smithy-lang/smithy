======================================
Amazon Machine Learning Customizations
======================================

----------------------------------------
Set host to value of ``PredictEndpoint``
----------------------------------------

The `Predict operation`_ makes use of an endpoint provided by other operations
in the service. The operation expects that the request will be sent to the
host specified in the ``PredictEndpoint`` parameter. An AWS client SHOULD
automatically set the host for the request to the host from the
``PredictEndpoint`` value.


.. _Predict operation: https://docs.aws.amazon.com/machine-learning/latest/APIReference/API_Predict.html
