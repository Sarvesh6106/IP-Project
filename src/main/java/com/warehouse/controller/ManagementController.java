package com.warehouse.controller;

import com.warehouse.dto.ManagementDto;
import com.warehouse.service.ManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/management")
@RequiredArgsConstructor
public class ManagementController {

    private final ManagementService managementService;

    @GetMapping("/branches")
    public ResponseEntity<List<ManagementDto.BranchDto>> getAllBranches() {
        return ResponseEntity.ok(managementService.getAllBranches());
    }

    @GetMapping("/branches/{id}")
    public ResponseEntity<ManagementDto.BranchDto> getBranchById(@PathVariable Long id) {
        return ResponseEntity.ok(managementService.getBranchById(id));
    }

    @PostMapping("/branches")
    public ResponseEntity<ManagementDto.BranchDto> createBranch(@RequestBody ManagementDto.BranchDto dto) {
        return ResponseEntity.ok(managementService.createBranch(dto));
    }

    @PutMapping("/branches/{id}")
    public ResponseEntity<ManagementDto.BranchDto> updateBranch(@PathVariable Long id, @RequestBody ManagementDto.BranchDto dto) {
        return ResponseEntity.ok(managementService.updateBranch(id, dto));
    }

    @DeleteMapping("/branches/{id}")
    public ResponseEntity<Map<String, String>> deleteBranch(@PathVariable Long id) {
        managementService.deleteBranch(id);
        return ResponseEntity.ok(Map.of("message", "Branch deleted successfully"));
    }

    @GetMapping("/products")
    public ResponseEntity<List<ManagementDto.ProductDto>> getAllProducts() {
        return ResponseEntity.ok(managementService.getAllProducts());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ManagementDto.ProductDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(managementService.getProductById(id));
    }

    @PostMapping("/products")
    public ResponseEntity<ManagementDto.ProductDto> createProduct(@RequestBody ManagementDto.ProductDto dto) {
        return ResponseEntity.ok(managementService.createProduct(dto));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ManagementDto.ProductDto> updateProduct(@PathVariable Long id, @RequestBody ManagementDto.ProductDto dto) {
        return ResponseEntity.ok(managementService.updateProduct(id, dto));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable Long id) {
        managementService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    @GetMapping("/staff")
    public ResponseEntity<List<ManagementDto.StaffDto>> getAllStaff() {
        return ResponseEntity.ok(managementService.getAllStaff());
    }

    @GetMapping("/staff/{id}")
    public ResponseEntity<ManagementDto.StaffDto> getStaffById(@PathVariable Long id) {
        return ResponseEntity.ok(managementService.getStaffById(id));
    }

    @PostMapping("/staff")
    public ResponseEntity<ManagementDto.StaffDto> createStaff(@RequestBody ManagementDto.StaffCreateRequest req) {
        return ResponseEntity.ok(managementService.createStaff(req));
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<ManagementDto.StaffDto> updateStaff(@PathVariable Long id, @RequestBody ManagementDto.StaffUpdateRequest req) {
        return ResponseEntity.ok(managementService.updateStaff(id, req));
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<Map<String, String>> deleteStaff(@PathVariable Long id) {
        managementService.deleteStaff(id);
        return ResponseEntity.ok(Map.of("message", "Staff deleted successfully"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
