package eu.ibagroup.vfdatabricks.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.ibagroup.vfdatabricks.dto.GraphDto;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DatabricksJobTask;
import eu.ibagroup.vfdatabricks.dto.jobs.databricks.DependentTask;
import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class PipelineTransformerTest {
    public static final String GRAPH = """
            {
              "graph": [
                {
                  "value": {
                    "operation": "JOB",
                    "name": "Job_stage",
                    "jobId": "job1",
                    "jobName": "read-from-google-job"
                  },
                  "id": "2",
                  "vertex": true,
                  "connectable": true,
                  "parent": "1",
                  "source": null,
                  "target": null,
                  "mxObjectId": "mxCell#8",
                  "edges": [
                    {
                      "value": {
                        "operation": "EDGE",
                        "successPath": "true",
                        "text": ""
                      },
                      "id": "4",
                      "edge": true,
                      "parent": "1",
                      "source": "3",
                      "target": "2",
                      "mxObjectId": "mxCell#11"
                    }
                  ]
                },
                {
                  "value": {
                    "operation": "PIPELINE",
                    "pipelineId": "pipeline1",
                    "pipelineName": "new",
                    "name": "Pipeline_stage"
                  },
                  "id": "3",
                  "vertex": true,
                  "connectable": true,
                  "parent": "1",
                  "source": null,
                  "target": null,
                  "mxObjectId": "mxCell#9",
                  "edges": [
                    {
                      "value": {
                        "operation": "EDGE",
                        "successPath": "true",
                        "text": ""
                      },
                      "id": "4",
                      "edge": true,
                      "parent": "1",
                      "source": "3",
                      "target": "2",
                      "mxObjectId": "mxCell#11"
                    },
                    {
                      "value": {
                        "operation": "EDGE",
                        "successPath": "true",
                        "text": ""
                      },
                      "id": "6",
                      "edge": true,
                      "parent": "1",
                      "source": "3",
                      "target": "5",
                      "mxObjectId": "mxCell#12"
                    },
                    {
                      "value": {
                        "operation": "EDGE",
                        "successPath": "true",
                        "text": ""
                      },
                      "id": "7",
                      "edge": true,
                      "parent": "1",
                      "source": "6",
                      "target": "3",
                      "mxObjectId": "mxCell#12"
                    }
                  ]
                },
                {
                  "value": {
                    "operation": "EDGE",
                    "successPath": "true",
                    "text": ""
                  },
                  "id": "4",
                  "edge": true,
                  "parent": "1",
                  "source": "3",
                  "target": "2",
                  "mxObjectId": "mxCell#11"
                },
                {
                  "value": {
                    "operation": "JOB",
                    "jobId": "job2",
                    "jobName": "some-job",
                    "name": "some-job-stage"
                  },
                  "id": "5",
                  "vertex": true,
                  "connectable": true,
                  "parent": "1",
                  "source": null,
                  "target": null,
                  "mxObjectId": "mxCell#10",
                  "edges": [
                    {
                      "value": {
                        "operation": "EDGE",
                        "successPath": "true",
                        "text": ""
                      },
                      "id": "6",
                      "edge": true,
                      "parent": "1",
                      "source": "3",
                      "target": "5",
                      "mxObjectId": "mxCell#12"
                    }
                  ]
                },
                {
                  "value": {
                    "operation": "JOB",
                    "jobId": "job3",
                    "jobName": "last-job",
                    "name": "last-job-stage"
                  },
                  "id": "6",
                  "vertex": true,
                  "connectable": true,
                  "parent": "1",
                  "source": null,
                  "target": null,
                  "mxObjectId": "mxCell#10",
                  "edges": [
                    {
                      "value": {
                        "operation": "EDGE",
                        "successPath": "true",
                        "text": ""
                      },
                      "id": "7",
                      "edge": true,
                      "parent": "1",
                      "source": "6",
                      "target": "3",
                      "mxObjectId": "mxCell#12"
                    }
                  ]
                },
                {
                  "value": {
                    "operation": "EDGE",
                    "successPath": "true",
                    "text": ""
                  },
                  "id": "6",
                  "edge": true,
                  "parent": "1",
                  "source": "3",
                  "target": "5",
                  "mxObjectId": "mxCell#12"
                }
              ]
            }""";
    static final String PIPE = """
            {
              "graph": [
                {
                  "value": {
                    "operation": "PIPELINE",
                    "pipelineId": "pipeline2",
                    "pipelineName": "nested_pipe",
                    "name": "NESTED_Pipeline_stage"
                  },
                  "id": "1",
                  "vertex": true,
                  "connectable": true,
                  "parent": "1",
                  "source": null,
                  "target": null,
                  "mxObjectId": "mxCell#9"
                }
              ]
            }""";
    private final ObjectMapper objectMapper = new ObjectMapper();

    Function<String, GraphDto> provider = (String id) -> {
        try {
            if ("pipeline1".equals(id)) {
                return GraphDto.parseGraph(objectMapper.readTree(PIPE));
            }
            JsonNode jsonNode = objectMapper.readTree("""
                    {
                      "graph": [
                        {
                          "value": {
                            "operation": "JOB",
                            "jobId": "last_job_in_pipeline",
                            "jobName": "new_job",
                            "name": "job1"
                          },
                          "id": "2",
                          "vertex": true,
                          "connectable": true,
                          "parent": "1",
                          "source": null,
                          "target": null,
                          "mxObjectId": "mxCell#21",
                          "edges": [
                            {
                              "value": {
                                "operation": "EDGE",
                                "successPath": "true",
                                "text": ""
                              },
                              "id": "4",
                              "edge": true,
                              "parent": "1",
                              "source": "3",
                              "target": "2",
                              "mxObjectId": "mxCell#11"
                            }
                          ]
                        },
                        {
                          "value": {
                            "operation": "JOB",
                            "jobId": "first_job_in_pipeline",
                            "jobName": "new_job",
                            "name": "job1"
                          },
                          "id": "3",
                          "vertex": true,
                          "connectable": true,
                          "parent": "1",
                          "source": null,
                          "target": null,
                          "mxObjectId": "mxCell#21",
                          "edges": [
                            {
                              "value": {
                                "operation": "EDGE",
                                "successPath": "true",
                                "text": ""
                              },
                              "id": "4",
                              "edge": true,
                              "parent": "1",
                              "source": "3",
                              "target": "2",
                              "mxObjectId": "mxCell#11"
                            }
                          ]
                        }
                      ]
                    }
                    """);
            return GraphDto.parseGraph(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    };

    private static Matcher<Object> getDependsOn(String... dependentTaskKey) {
        if (ArrayUtils.isEmpty(dependentTaskKey)) {
            return nullValue();
        }
        List<DependentTask> dependentTasks = Arrays.stream(dependentTaskKey)
                .map(taskKey -> new DependentTask(taskKey, null))
                .toList();
        return is(dependentTasks);
    }

    private static Matcher<DatabricksJobTask> getDatabricksJobTaskMatcher(String taskKey, String... dependsOn) {
        return allOf(
                hasProperty("taskKey", is(taskKey)),
                hasProperty("dependsOn", getDependsOn(dependsOn))
        );
    }

    @Test
    void transformShouldCreateDatabricksJobTasks() throws JsonProcessingException {
        PipelineTransformer pipelineTransformer = new PipelineTransformer(provider, jobId -> new DatabricksJobTask());
        List<DatabricksJobTask> tasks = pipelineTransformer.transform(GraphDto.parseGraph(objectMapper.readTree(GRAPH)));

        tasks.forEach(System.out::println);

        assertThat(tasks, allOf(
                containsInAnyOrder(
                        getDatabricksJobTaskMatcher("Job_stage-2", "job1-3-1-2"),
                        getDatabricksJobTaskMatcher("job1-3-1-2", "job1-3-1-3"),
                        getDatabricksJobTaskMatcher("job1-3-1-3", "last_job_stage-6"),
                        getDatabricksJobTaskMatcher("some_job_stage-5", "job1-3-1-2"),
                        getDatabricksJobTaskMatcher("last_job_stage-6")
                )
        ));
    }
}