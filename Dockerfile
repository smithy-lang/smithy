FROM amazonlinux:2
ADD smithy-cli/build/image/smithy-cli-linux-x86_64 /smithy
WORKDIR /smithy

# Build application class data sharing archive using smithy validate as the baseline.
RUN SMITHY_OPTS="-XX:DumpLoadedClassList=/smithy/lib/smithy.lst" \
    /smithy/bin/smithy validate
RUN SMITHY_OPTS="-Xshare:dump -XX:SharedArchiveFile=/smithy/lib/smithy.jsa -XX:SharedClassListFile=/smithy/lib/smithy.lst" \
    /smithy/bin/smithy validate

ENTRYPOINT [ "/smithy/bin/smithy" ]
