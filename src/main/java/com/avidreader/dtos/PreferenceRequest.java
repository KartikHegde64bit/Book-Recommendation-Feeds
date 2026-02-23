package com.avidreader.dtos;

import lombok.Data;

import java.util.List;

/**
 * Request body for updating user reading preferences (tags).
 */
@Data
public class PreferenceRequest {
    private List<String> tags;
}
