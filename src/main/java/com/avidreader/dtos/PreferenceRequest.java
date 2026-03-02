package com.avidreader.dtos;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for updating user reading preferences (tags).
 */
@Data
public class PreferenceRequest {
    @NotEmpty(message = "Tags list cannot be empty")
    @Size(min = 1, max = 50, message = "Must have between 1 and 50 tags")
    private List<String> tags;
}
