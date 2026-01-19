package com.xurx.springai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentUploadDTO {
    private UUID userId;
    private int chunkSize;
    private int chunkOverlap;
}