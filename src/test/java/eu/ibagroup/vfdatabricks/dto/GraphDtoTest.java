package eu.ibagroup.vfdatabricks.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class GraphDtoTest {

    private static final String INPUT_GRAPH = "{\n" +
            "  \"graph\": [\n" +
            "    {\n" +
            "       \"id\": \"-jRjFu5yR\",\n" +
            "       \"vertex\": true,\n" +
            "      \"value\": {\n" +
            "        \"label\": \"Read\",\n" +
            "        \"text\": \"stage\",\n" +
            "        \"desc\": \"description\",\n" +
            "        \"type\": \"read\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "       \"id\": \"cyVyU8Xfw\",\n" +
            "       \"vertex\": true,\n" +
            "      \"value\": {\n" +
            "        \"label\": \"Write\",\n" +
            "        \"text\": \"stage\",\n" +
            "        \"desc\": \"description\",\n" +
            "        \"type\": \"write\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"value\": {},\n" +
            "      \"id\": \"4\",\n" +
            "      \"edge\": true,\n" +
            "      \"parent\": \"1\",\n" +
            "      \"source\": \"-jRjFu5yR\",\n" +
            "      \"target\": \"cyVyU8Xfw\",\n" +
            "      \"successPath\": true,\n" +
            "      \"mxObjectId\": \"mxCell#8\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    @Test
    void testToString() throws JsonProcessingException {
                GraphDto graphDto = GraphDto.parseGraph(new ObjectMapper().readTree(INPUT_GRAPH));
        assertEquals(new ObjectMapper().writeValueAsString(graphDto), graphDto.toString(), "String must be equal to expected");
    }

    @Test
    void testParseGraph() throws JsonProcessingException {
        List<GraphDto.NodeDto> expectedNodes = List.of(new GraphDto.NodeDto("-jRjFu5yR",
                        Map.of("label",
                                "Read",
                                "text",
                                "stage",
                                "desc",
                                "description",
                                "type",
                                "read"),
                        null),
                new GraphDto.NodeDto("cyVyU8Xfw",
                        Map.of("label",
                                "Write",
                                "text",
                                "stage",
                                "desc",
                                "description",
                                "type",
                                "write")
                        , null));
        List<GraphDto.EdgeDto> expectedEdges = List.of(new GraphDto.EdgeDto(Map.of(), "-jRjFu5yR", "cyVyU8Xfw"));

        GraphDto graphDto = GraphDto.parseGraph(new ObjectMapper().readTree(INPUT_GRAPH));
        assertEquals(expectedNodes, graphDto.getNodes(), "Nodes must be equal to expected");
        assertEquals(expectedEdges, graphDto.getEdges(), "Edges must be equal to expected");
    }
}
