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

package eu.ibagroup.vfdatabricks.services;

import eu.ibagroup.vfdatabricks.dto.GraphDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobTask;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DependentTask;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.RunIf;
import eu.ibagroup.vfdatabricks.exceptions.BadRequestException;
import org.springframework.data.util.Pair;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class PipelineTransformer {

    public static final String ENTRY_NODES = "entryNodes";
    public static final String EXIT_NODES = "exitNodes";
    public static final String OPERATION = "operation";
    public static final String SUCCESS_PATH = "successPath";
    public static final String PIPELINE = "PIPELINE";
    public static final String PIPELINE_ID = "pipelineId";
    public static final String SEPARATOR_CHAR = "-";

    private final Function<String, GraphDto> pipelineProvider;
    private final Function<String, DatabricksJobTask> databricksJobTaskProvider;
    private final Map<String, Set<Pair<String, String>>> parentMap = new HashMap<>();
    private final Map<String, GraphDto.NodeDto> nodeMap = new HashMap<>();

    public PipelineTransformer(Function<String, GraphDto> pipelineProvider,
                               Function<String, DatabricksJobTask> databricksJobTaskProvider) {
        this.pipelineProvider = pipelineProvider;
        this.databricksJobTaskProvider = databricksJobTaskProvider;
    }

    static String getId(String parentId, String id) {
        if (parentId == null) {
            return id;
        }
        return parentId + SEPARATOR_CHAR + id;
    }

    private GraphDto getGraphDto(String pipelineId, String parentId) {
        GraphDto graphDto = pipelineProvider.apply(pipelineId);
        updateReferences(graphDto, parentId);
        return graphDto;
    }

    private void buildNodeMap(GraphDto graphDto) {
        // Populate the nodeMap for easy lookup
        for (GraphDto.NodeDto node : graphDto.getNodes()) {
            nodeMap.put(node.getId(), node);
        }
    }

    public List<DatabricksJobTask> transform(GraphDto graphDto) {
        populateDependsMap(graphDto);
        ArrayList<DatabricksJobTask> tasks = new ArrayList<>();
        for (GraphDto.NodeDto node : graphDto.getNodes()) {
            createTasksFromNode(node, tasks);
        }
        return tasks;
    }

    private void populateDependsMap(GraphDto graphDto) {
        buildNodeMap(graphDto);
        for (GraphDto.NodeDto node : graphDto.getNodes()) {
            processEdges(node, null);
        }
        graphDto.getNodes().stream()
                .filter(node -> PIPELINE.equals(node.getValue().get(OPERATION)))
                .forEach(this::extractPipelines);
    }

    private void extractPipelines(GraphDto.NodeDto node) {
        String pipelineId = node.getValue().get(PIPELINE_ID);
        String id = node.getId();
        Set<Pair<String, String>> parent = parentMap.remove(node.getId());

        GraphDto graphDto = getGraphDto(pipelineId, id);
        populateDependsMap(graphDto);
        Map<String, List<GraphDto.NodeDto>> entryAndExitNodes = getEntryAndExitNodes(graphDto);
        if (!CollectionUtils.isEmpty(parent)) {
            entryAndExitNodes.get(ENTRY_NODES).forEach(entryNode -> parentMap.put(entryNode.getId(), parent));
        }
        // replace the pipeline node with the entry nodes in the parentMap
        parentMap.forEach((k, v) -> v.forEach((Pair<String, String> pair) -> {
            if (v.remove(Pair.of(node.getId(), pair.getSecond()))) {
                List<Pair<String, String>> newDepends = entryAndExitNodes.get(EXIT_NODES).stream()
                        .map(exitNode -> Pair.of(exitNode.getId(), pair.getSecond()))
                        .toList();
                v.addAll(newDepends);
            }
        }));
    }

    private void processEdges(GraphDto.NodeDto node, Set<Pair<String, String>> parents) {
        if (!CollectionUtils.isEmpty(parents)) {
            Set<Pair<String, String>> depends = parentMap.computeIfAbsent(node.getId(), k -> new HashSet<>());
            boolean pathMatches = depends.stream()
                    .allMatch(v -> Objects.equals(
                            v.getSecond(),
                            Objects.requireNonNull(CollectionUtils.firstElement(parents)).getSecond())
                    );
            if (pathMatches) {
                depends.addAll(parents);
            } else {
                throw new BadRequestException("Node can't have different type of income arrows");
            }
        }

        if (node.getEdges() != null) {
            for (GraphDto.EdgeDto edge : node.getEdges()) {
                if (Objects.equals(node.getId(), edge.getSource())) {
                    Pair<String, String> parent = Pair.of(edge.getSource(), edge.getValue().get(SUCCESS_PATH));
                    processEdges(nodeMap.get(edge.getTarget()), Set.of(parent));
                }
            }
        }
    }

    private MultiValueMap<String, GraphDto.NodeDto> getEntryAndExitNodes(GraphDto graphDto) {
        MultiValueMap<String, GraphDto.NodeDto> entryAndExitNodes = new LinkedMultiValueMap<>();
        for (GraphDto.NodeDto node : graphDto.getNodes()) {
            List<GraphDto.EdgeDto> edges = Optional.ofNullable(node.getEdges()).orElse(Collections.emptyList());
            boolean isEntryNode = edges.stream().noneMatch(edge -> node.getId().equals(edge.getTarget()));
            boolean isExitNode = edges.stream().noneMatch(edge -> node.getId().equals(edge.getSource()));
            if (isEntryNode || isExitNode) {
                processNodeForEntryAndExitNodes(node, entryAndExitNodes, isEntryNode, isExitNode);
            }
        }
        return entryAndExitNodes;
    }

    private void processNodeForEntryAndExitNodes(GraphDto.NodeDto node,
                                                 MultiValueMap<String, GraphDto.NodeDto> entryAndExitNodes,
                                                 boolean isEntryNode,
                                                 boolean isExitNode) {
        if ("JOB".equals(node.getValue().get(OPERATION))) {
            if (isEntryNode) {
                entryAndExitNodes.add(ENTRY_NODES, node);
            }
            if (isExitNode) {
                entryAndExitNodes.add(EXIT_NODES, node);
            }
        }
        if (PIPELINE.equals(node.getValue().get(OPERATION))) {
            GraphDto pipelineGraphDto = getGraphDto(node.getValue().get(PIPELINE_ID), node.getId());
            MultiValueMap<String, GraphDto.NodeDto> childEntryExitNodes = getEntryAndExitNodes(pipelineGraphDto);
            if (isEntryNode) {
                entryAndExitNodes.addAll(ENTRY_NODES, childEntryExitNodes.get(ENTRY_NODES));
            }
            if (isExitNode) {
                entryAndExitNodes.addAll(EXIT_NODES, childEntryExitNodes.get(EXIT_NODES));
            }
        }
    }


    void updateReferences(GraphDto graphDto, String parentId) {
        for (GraphDto.NodeDto node : graphDto.getNodes()) {
            node.setId(getId(parentId, node.getId()));
            if (node.getEdges() != null) {
                for (GraphDto.EdgeDto edge : node.getEdges()) {
                    edge.setSource(getId(parentId, edge.getSource()));
                    edge.setTarget(getId(parentId, edge.getTarget()));
                }
            }
        }
    }

    private void createTasksFromNode(GraphDto.NodeDto node, List<DatabricksJobTask> tasks) {

        Map<String, String> value = node.getValue();

        if ("JOB".equalsIgnoreCase(value.get(OPERATION))) {
            String jobId = value.get("jobId");
            DatabricksJobTask databricksJobTask = databricksJobTaskProvider.apply(jobId);
            databricksJobTask.setTaskKey(createTaskKey(node.getId()));
            Set<Pair<String, String>> depends = parentMap.get(node.getId());
            if (!CollectionUtils.isEmpty(depends)) {
                Pair<String, String> first = Objects.requireNonNull(CollectionUtils.firstElement(depends));
                RunIf runIf = getRunIf(first);
                databricksJobTask.setRunIf(runIf);
                List<DependentTask> dependentTasks = depends.stream()
                        .map(Pair::getFirst)
                        .map(nodeId -> DependentTask.builder().taskKey(createTaskKey(nodeId)).build())
                        .toList();
                databricksJobTask.setDependsOn(dependentTasks);
            }
            tasks.add(databricksJobTask);
        }

        if (PIPELINE.equalsIgnoreCase(value.get(OPERATION))) {
            String pipelineId = value.get(PIPELINE_ID);
            GraphDto pipelineGraphDto = getGraphDto(pipelineId, node.getId());
            for (GraphDto.NodeDto pipelineGraphDtoNode : pipelineGraphDto.getNodes()) {
                createTasksFromNode(pipelineGraphDtoNode, tasks);
            }
        }
    }

    private static RunIf getRunIf(Pair<String, String> first) {
        if ("false".equals(first.getSecond())) {
            return RunIf.ALL_FAILED;
        }
        return RunIf.ALL_SUCCESS;
    }

    private String createTaskKey(String nodeId) {
        String name = nodeMap.get(nodeId).getValue().get("name");
        return MapperService.toAlphaNumeric(name) + SEPARATOR_CHAR + nodeId;
    }
}
