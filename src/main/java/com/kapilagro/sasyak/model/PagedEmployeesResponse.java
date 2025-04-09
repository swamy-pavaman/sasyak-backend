package com.kapilagro.sasyak.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PagedEmployeesResponse {
    private List<GetEmployeesResponse.EmployeeDTO> employees;
    private long totalItems;
    private int totalPages;
    private int currentPage;
}