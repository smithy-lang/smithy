FROM gradle:5.4.1-jdk12
USER root
ADD ./ /smithy
WORKDIR /smithy
RUN gradle :smithy-cli:runtime
WORKDIR /work

ENTRYPOINT [ "/smithy/smithy-cli/build/image/bin/smithy" ]
