package software.amazon.smithy.codegen.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.shapes.ShapeId;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

class TraceFileTest {

    @Test
    void parseTraceFile() throws URISyntaxException, FileNotFoundException {
        TraceFile traceFile = new TraceFile();
        traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());

        assert traceFile.getArtifactMetadata().getId().equals("software.amazon.awssdk.services:snowball:2.10.79");
        assert traceFile.getDefinitions().get().getTags().containsValue("AWS SDK response builder");
        assert traceFile.getShapes().containsKey(ShapeId.from("com.amazonaws.snowball#Snowball"));
        assert traceFile.getSmithyTrace().equals("1.0");
    }

    @Test
    void writeTraceFile() throws URISyntaxException, IOException {
        TraceFile traceFile = new TraceFile();
        traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
        traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        TraceFile traceFile2 = new TraceFile();
        traceFile2.parseTraceFile(getClass().getResource("trace_file_output.txt").toURI());

        assert traceFile2.getArtifactMetadata().getId().equals("software.amazon.awssdk.services:snowball:2.10.79");
        assert traceFile2.getDefinitions().get().getTags().containsValue("AWS SDK response builder");
        assert traceFile2.getShapes().containsKey(ShapeId.from("com.amazonaws.snowball#Snowball"));
        assert traceFile2.getShapes().get(ShapeId.from("com.amazonaws.snowball#Snowball")).get(0).getType().equals("TYPE");
        assert traceFile2.getSmithyTrace().equals("1.0");
    }

    @Test
    void validateTypesOrTags() throws ExpectationNotMetException, URISyntaxException, FileNotFoundException {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            traceFile.getShapes().get(ShapeId.from("com.amazonaws.snowball#Snowball")).get(0).setType("fake_type");
            traceFile.validateTypesAndTags();
        });
    }

    @Test
    void validateModel() throws ExpectationNotMetException, URISyntaxException, FileNotFoundException {
        TraceFile traceFile = new TraceFile();
        traceFile.parseTraceFile(getClass().getResource("trace_file_weather.txt").toURI());

        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            traceFile.validateModel("weather-service.smithy");
        });
    }
}