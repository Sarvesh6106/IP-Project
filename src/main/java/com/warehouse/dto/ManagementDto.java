package com.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ManagementDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchDto {
        private Long id;
        private String name;
        private String location;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductDto {
        private Long id;
        private String sku;
        private String name;
        private String description;
        private Integer minimumStockThreshold;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffDto {
        private Long id;
        private String name;
        private String email;
        private String role;
        private Long assignedBranchId;
        private String assignedBranchName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffCreateRequest {
        private String name;
        private String email;
        private String password;
        private String role;
        private Long assignedBranchId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffUpdateRequest {
        private String name;
        private String email;
        private String role;
        private Long assignedBranchId;
    }
}
