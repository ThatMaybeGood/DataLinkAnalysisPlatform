package com.workflow.platform.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowQueryDTO {
    private String mode;
    private String sortOrder;
    private String searchText;
    private String name;
    private String category;
    private String status;
    private String alias;
    private Boolean isPublished;
    private Integer pageSize;
    private Integer pageNumber;
    private HttpServletRequest request;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

}