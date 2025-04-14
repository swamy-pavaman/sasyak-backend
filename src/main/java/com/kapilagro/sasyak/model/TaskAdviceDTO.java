package com.kapilagro.sasyak.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAdviceDTO {
    private int id;
    private int taskId;
    private String managerName;
    private String adviceText;
    private LocalDateTime createdAt;
}