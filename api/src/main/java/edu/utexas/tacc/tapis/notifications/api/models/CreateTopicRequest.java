package edu.utexas.tacc.tapis.notifications.api.models;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class CreateTopicRequest {

    @NotBlank
    @Schema(description = "Topic names must be unique and can only contain A-z, 0-9, and . - _")
    @Pattern(regexp = "^[.A-Za-z0-9_-]+$")
    @Size(max=32)
    private String name;

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
}

