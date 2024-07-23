/*
 * Copyright (c) 2021 IBA Group, a.s. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.ibagroup.vfdatabricks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Graph DTO class.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GraphDto {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private List<NodeDto> nodes;
    private List<EdgeDto> edges;

    /**
     * Parse definition to nodes and edges.
     *
     * @param definition json
     * @return GraphDto with nodes and edges
     */
    public static GraphDto parseGraph(JsonNode definition) {
        ArrayNode nodesArray = definition.withArray("graph");

        List<NodeDto> nodes = new ArrayList<>();
        List<EdgeDto> edges = new ArrayList<>();
        for (JsonNode node : nodesArray) {
            if (node.get("vertex") != null) {
                nodes.add(MAPPER.convertValue(node, NodeDto.class));
            } else {
                edges.add(MAPPER.convertValue(node, EdgeDto.class));
            }
        }

        return new GraphDto(nodes, edges);
    }

    @Override
    public String toString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Bad graph structure", e);
        }
    }

    /**
     * Node Dto class.
     */
    @AllArgsConstructor
    @Builder
    @NoArgsConstructor
    @Getter
    @Setter
    @EqualsAndHashCode
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NodeDto {
        private String id;
        private Map<String, String> value;
        private List<EdgeDto> edges;
    }

    /**
     * Edge DTO class
     */
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    @EqualsAndHashCode
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EdgeDto {
        private Map<String, String> value;
        private String source;
        private String target;
    }
}
