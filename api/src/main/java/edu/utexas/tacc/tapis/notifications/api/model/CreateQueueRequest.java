package edu.utexas.tacc.tapis.notifications.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

public class CreateQueueRequest {


    @NotBlank
    @Schema(description = "Queue names must be unique and can only contain A-z, 0-9, and . - _")
    @Pattern(regexp = "^[.A-Za-z0-9_-]+$")
    @Size(max=256)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
