package software.amazon.smithy.codegen.core;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Assertions;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

import java.io.*;
import java.net.URISyntaxException;
import java.util.HashMap;

public class TraceFileIntegrationTest {
    /* Correct TraceFile use tests
    -Test parse and write functionality combined on a correct trace file
    -Test parse and write functionality combined on a correct trace file without the optional definitions section
     */
    @Test
    void correctParseWrite() throws IOException, URISyntaxException {
        TraceFile traceFile = new TraceFile();
        traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
        traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        TraceFile traceFile2 = new TraceFile();
        traceFile2.parseTraceFile(getClass().getResource("trace_file_output.txt").toURI());

        assert traceFile2.getArtifactMetadata().getId().equals("software.amazon.awssdk.services:snowball:2.10.79");
        assert traceFile2.getDefinitions().get().getTags().containsValue("AWS SDK response builder");
        assert traceFile2.getShapes().containsKey(ShapeId.from("com.amazonaws.snowball#Snowball"));
        assert traceFile.getShapes().get(ShapeId.from("com.amazonaws.snowball#Snowball")).get(0).getType().equals("TYPE");
        assert traceFile2.getSmithyTrace().equals("1.0");
    }

    @Test
    void correctWithoutDefinitionsParseWrite() throws IOException, URISyntaxException {
        TraceFile traceFile = new TraceFile();
        traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());

        //set definitions to null before writing and parsing again
        traceFile.setDefinitions(null);

        traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        TraceFile traceFile2 = new TraceFile();
        traceFile2.parseTraceFile(getClass().getResource("trace_file_output.txt").toURI());

        assert traceFile2.getArtifactMetadata().getId().equals("software.amazon.awssdk.services:snowball:2.10.79");
        assert traceFile2.getShapes().containsKey(ShapeId.from("com.amazonaws.snowball#Snowball"));
        assert traceFile.getShapes().get(ShapeId.from("com.amazonaws.snowball#Snowball")).get(0).getType().equals("TYPE");
        assert traceFile2.getSmithyTrace().equals("1.0");
    }

    /* Incorrect write tests:
    -Test null values for all required variables of TraceFile and TraceFile's variable's variables
    -Test empty values for a variety TraceFile and TraceFile's variable's variables that are not Strings or Numbers
     */
    //TraceFile writing integration tests
    @Test
    void incorrectWriteNoSmithyTrace() throws IOException, URISyntaxException {
        Assertions.assertThrows(TraceFileWritingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            //set smithyTrace to null before writing and parsing again
            traceFile.setSmithyTrace(null);
            traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        });
    }

    @Test
    void incorrectWriteNoArtifactMetadata(){
        Assertions.assertThrows(TraceFileWritingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            //set to null before writing and parsing again
            traceFile.setArtifactMetadata(null);
            traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        });
    }

    @Test
    void incorrectWriteNoShapes(){
        Assertions.assertThrows(TraceFileWritingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            //set to null before writing and parsing again
            traceFile.setShapes(null);
            traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        });
    }

    @Test
    void incorrectWriteNoShapeId(){
        Assertions.assertThrows(TraceFileWritingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            //set to null before writing and parsing again
            traceFile.setShapes(new HashMap<>());
            traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        });
    }

    //Definitions specific writing integration tests
    @Test
    void incorrectWriteNoDefinitionsTags(){
        Assertions.assertThrows(TraceFileWritingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            //set to null before writing and parsing again
            traceFile.getDefinitions().orElseThrow(Exception::new).setTags(null);
            traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        });
    }

    @Test
    void incorrectWriteDefinitionsTagsEmpty(){
        Assertions.assertThrows(TraceFileWritingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            //set to null before writing and parsing again
            traceFile.getDefinitions().orElseThrow(Exception::new).setTags(new HashMap<>());
            traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        });
    }

    @Test
    void incorrectWriteNoDefinitionsTypes(){
        Assertions.assertThrows(TraceFileWritingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            //set to null before writing and parsing again
            traceFile.getDefinitions().orElseThrow(Exception::new).setTags(null);
            traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        });
    }

    @Test
    void incorrectWriteDefinitionsTypesEmpty(){
        Assertions.assertThrows(TraceFileWritingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            //set to null before writing and parsing again
            traceFile.getDefinitions().orElseThrow(Exception::new).setTypes(new HashMap<>());
            traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        });
    }

    //ArtifactMetadata specific writing integration tests
    @Test
    void incorrectWriteNoArtifactId(){
        Assertions.assertThrows(TraceFileWritingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            //set to null before writing and parsing again
            traceFile.getArtifactMetadata().setId(null);
            traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        });
    }

    @Test
    void incorrectWriteNoArtifactVersion(){
        Assertions.assertThrows(TraceFileWritingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            //set to null before writing and parsing again
            traceFile.getArtifactMetadata().setVersion(null);
            traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        });
    }

    @Test
    void incorrectWriteNoArtifactType(){
        Assertions.assertThrows(TraceFileWritingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            //set to null before writing and parsing again
            traceFile.getArtifactMetadata().setType(null);
            traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        });
    }

    @Test
    void incorrectWriteNoArtifactTimeStamp(){
        Assertions.assertThrows(TraceFileWritingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            //set to null before writing and parsing again
            traceFile.getArtifactMetadata().setTimestamp(null);
            traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        });
    }

    //ShapeLink specific writing integration tests
    @Test
    void incorrectWriteNoShapeLinkType(){
        Assertions.assertThrows(TraceFileWritingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            //set to null before writing and parsing again
            for(ShapeId id: traceFile.getShapes().keySet()){
                for(ShapeLink link: traceFile.getShapes().get(id)){
                    link.setType(null);
                }
            }
            traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        });
    }

    @Test
    void incorrectWriteNoShapeLinkId(){
        Assertions.assertThrows(TraceFileWritingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file.txt").toURI());
            //set to null before writing and parsing again
            for(ShapeId id: traceFile.getShapes().keySet()){
                for(ShapeLink link: traceFile.getShapes().get(id)){
                    link.setId(null);
                }
            }
            traceFile.writeTraceFile(getClass().getResource("trace_file_output.txt").getFile());
        });
    }

    /* Incorrect Parse tests:
    -Test empty JSON tags/values in trace file to be parsed for all required variables of TraceFile and TraceFile's variable's variables
    -Test empty JSON tags/values in trace file to be parsed for a variety TraceFile and TraceFile's variable's variables
     */
    //TraceFile parsing integration tests
    @Test
    void incorrectParseNoSmithyTrace() throws IOException, URISyntaxException {
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file_incorrect1.txt").toURI());
        });
    }

    @Test
    void incorrectParseNoArtifactMetadata(){
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file_incorrect2.txt").toURI());
        });    }

    @Test
    void incorrectParseNoShapes(){
        Assertions.assertThrows(ExpectationNotMetException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file_incorrect3.txt").toURI());
        });    }

    @Test
    void incorrectParseNoShapeId(){
        Assertions.assertThrows(TraceFileParsingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file_incorrect4.txt").toURI());
        });
    }

    //Definitions specific parsing integration tests
    @Test
    void incorrectParseNoDefinitionsTags(){
        Assertions.assertThrows(TraceFileParsingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file_incorrect5.txt").toURI());
        });
    }

    @Test
    void incorrectParseDefinitionsTagsEmpty(){
        Assertions.assertThrows(TraceFileParsingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file_incorrect6.txt").toURI());
        });
    }

    @Test
    void incorrectParseNoDefinitionsTypes(){
        Assertions.assertThrows(TraceFileParsingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file_incorrect7.txt").toURI());
        });
    }

    @Test
    void incorrectParseDefinitionsTypesEmpty(){
        Assertions.assertThrows(TraceFileParsingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file_incorrect8.txt").toURI());
        });
    }

    //ArtifactMetadata specific parsing integration tests
    @Test
    void incorrectParseNoArtifactId(){
        Assertions.assertThrows(TraceFileParsingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file_incorrect9.txt").toURI());
        });
    }

    @Test
    void incorrectParseNoArtifactVersion(){
        Assertions.assertThrows(TraceFileParsingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file_incorrect10.txt").toURI());
        });
    }

    @Test
    void incorrectParseNoArtifactType(){
        Assertions.assertThrows(TraceFileParsingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file_incorrect11.txt").toURI());
        });
    }

    @Test
    void incorrectParseNoArtifactTimeStamp(){
        Assertions.assertThrows(TraceFileParsingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file_incorrect12.txt").toURI());
        });
    }

    //ShapeLink specific parsing integration tests
    @Test
    void incorrectParseNoShapeLinkType(){
        Assertions.assertThrows(TraceFileParsingException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file_incorrect13.txt").toURI());
        });
    }

    @Test
    void incorrectParseNoShapeLinkId(){
        Assertions.assertThrows(ModelSyntaxException.class, () -> {
            TraceFile traceFile = new TraceFile();
            traceFile.parseTraceFile(getClass().getResource("trace_file_incorrect14.txt").toURI());
        });
    }
}
