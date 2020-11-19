package edu.utexas.tacc.tapis.notifications.api.models;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;

public class CreateTopicRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String schema;

    @NotBlank
    private String description;

    @Schema(description = "An informative description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Schema(description = "Name for the topic, must be unique")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Schema(description = "A valid JSON schema")
    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }
}
