# Build Smithy CLI using latest JDK
FROM public.ecr.aws/docker/library/gradle:7-jdk17 AS build
WORKDIR /build
COPY . .

RUN \
    # Generate all traits
    ./gradlew :smithy-aws-apigateway-traits:jar --stacktrace && \
    ./gradlew :smithy-aws-cloudformation-traits:jar --stacktrace && \
    ./gradlew :smithy-aws-iam-traits:jar --stacktrace && \
    ./gradlew :smithy-aws-traits:jar --stacktrace && \
    ./gradlew :smithy-mqtt-traits:jar --stacktrace && \
    ./gradlew :smithy-protocol-test-traits:jar --stacktrace && \
    ./gradlew :smithy-validation-model:jar --stacktrace && \
    # Build the CLI
    ./gradlew :smithy-cli:runtime --stacktrace

# Run Smithy CLI in AL2 container
FROM public.ecr.aws/amazonlinux/amazonlinux:2

WORKDIR /smithy
COPY --from=build /build/smithy-cli/build/image/smithy-cli-linux-x86_64 .

# Copy in all traits
COPY --from=build /build/smithy-aws-apigateway-traits/build/libs/*.jar /smithy/lib/traits/
COPY --from=build /build/smithy-aws-cloudformation-traits/build/libs/*.jar /smithy/lib/traits/
COPY --from=build /build/smithy-aws-iam-traits/build/libs/*.jar /smithy/lib/traits/
COPY --from=build /build/smithy-aws-traits/build/libs/*.jar /smithy/lib/traits/
COPY --from=build /build/smithy-mqtt-traits/build/libs/*.jar /smithy/lib/traits/
COPY --from=build /build/smithy-protocol-test-traits/build/libs/*.jar /smithy/lib/traits/
COPY --from=build /build/smithy-validation-model/build/libs/*.jar /smithy/lib/traits/

# Build application class data sharing archive using smithy validate as the baseline.
RUN SMITHY_OPTS="-XX:DumpLoadedClassList=/smithy/lib/smithy.lst" \
    /smithy/bin/smithy validate
RUN SMITHY_OPTS="-Xshare:dump -XX:SharedArchiveFile=/smithy/lib/smithy.jsa -XX:SharedClassListFile=/smithy/lib/smithy.lst" \
    /smithy/bin/smithy validate

# Fixes UTF-8 issues in stdout
RUN echo "LC_ALL=en_US.UTF-8" >> /etc/environment
RUN echo "en_US.UTF-8 UTF-8" >> /etc/locale.gen
RUN echo "LANG=en_US.UTF-8" > /etc/locale.conf
ENV JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8

ENTRYPOINT [ "/smithy/bin/smithy" ]
