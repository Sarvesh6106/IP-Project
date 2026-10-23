package com.warehouse.service;

import com.warehouse.dto.ManagementDto;
import com.warehouse.entity.Branch;
import com.warehouse.entity.Product;
import com.warehouse.entity.PurchaseRequest;
import com.warehouse.entity.Stock;
import com.warehouse.entity.TransferRequest;
import com.warehouse.entity.User;
import com.warehouse.repository.BranchRepository;
import com.warehouse.repository.ProductRepository;
import com.warehouse.repository.PurchaseRequestRepository;
import com.warehouse.repository.StockRepository;
import com.warehouse.repository.TransferRequestRepository;
import com.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagementService {

    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final ModelMapper modelMapper;

    public List<ManagementDto.BranchDto> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(branch -> modelMapper.map(branch, ManagementDto.BranchDto.class))
                .collect(Collectors.toList());
    }

    public ManagementDto.BranchDto getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + id));
        return modelMapper.map(branch, ManagementDto.BranchDto.class);
    }

    public ManagementDto.BranchDto createBranch(ManagementDto.BranchDto dto) {
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("Branch name is required");
        }
        Branch branch = modelMapper.map(dto, Branch.class);
        branch.setId(null);
        Branch saved = branchRepository.save(branch);
        return modelMapper.map(saved, ManagementDto.BranchDto.class);
    }

    public ManagementDto.BranchDto updateBranch(Long id, ManagementDto.BranchDto dto) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Branch not found with id: " + id));
        branch.setName(dto.getName());
        branch.setLocation(dto.getLocation());
        Branch saved = branchRepository.save(branch);
        return modelMapper.map(saved, ManagementDto.BranchDto.class);
    }

    @Transactional
    public void deleteBranch(Long id) {
        if (!branchRepository.existsById(id)) {
            throw new RuntimeException("Branch not found with id: " + id);
        }

        List<User> users = userRepository.findByAssignedBranchId(id);
        for (User u : users) {
            u.setAssignedBranch(null);
            userRepository.save(u);
        }

        List<Stock> stocks = stockRepository.findByBranchId(id);
        stockRepository.deleteAll(stocks);
List<PurchaseRequest> purchases = purchaseRequestRepository.findByBranchWithDetails(id);
        purchaseRequestRepository.deleteAll(purchases);

        List<TransferRequest> transfers = transferRequestRepository.findAll();
        for (TransferRequest tr : transfers) {
            if (tr.getSourceBranch().getId().equals(id) || tr.getDestinationBranch().getId().equals(id)) {
                transferRequestRepository.delete(tr);
            }
        }

        branchRepository.deleteById(id);
    }

    public List<ManagementDto.ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(product -> modelMapper.map(product, ManagementDto.ProductDto.class))
                .collect(Collectors.toList());
    }

    public ManagementDto.ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        return modelMapper.map(product, ManagementDto.ProductDto.class);
    }

    public ManagementDto.ProductDto createProduct(ManagementDto.ProductDto dto) {
        if (dto.getSku() == null || dto.getSku().trim().isEmpty()) {
            throw new RuntimeException("Product SKU is required");
        }
        if (productRepository.existsBySku(dto.getSku())) {
            throw new RuntimeException("Product SKU already exists: " + dto.getSku());
        }
        Product product = modelMapper.map(dto, Product.class);
        product.setId(null);
        Product saved = productRepository.save(product);
        return modelMapper.map(saved, ManagementDto.ProductDto.class);
    }

    public ManagementDto.ProductDto updateProduct(Long id, ManagementDto.ProductDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        product.setSku(dto.getSku());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setMinimumStockThreshold(dto.getMinimumStockThreshold());
        Product saved = productRepository.save(product);
        return modelMapper.map(saved, ManagementDto.ProductDto.class);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }

        List<Stock> stocks = stockRepository.findByProductId(id);
        stockRepository.deleteAll(stocks);

        List<PurchaseRequest> purchases = purchaseRequestRepository.findAll();
        for (PurchaseRequest pr : purchases) {
            if (pr.getProduct().getId().equals(id)) {
                purchaseRequestRepository.delete(pr);
            }
        }

        List<TransferRequest> transfers = transferRequestRepository.findAll();
        for (TransferRequest tr : transfers) {
            if (tr.getProduct().getId().equals(id)) {
                transferRequestRepository.delete(tr);
            }
        }

        productRepository.deleteById(id);
    }

    public List<ManagementDto.StaffDto> getAllStaff() {
        return userRepository.findAll().stream()
                .map(this::mapToStaffDto)
                .collect(Collectors.toList());
    }

    public ManagementDto.StaffDto getStaffById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return mapToStaffDto(user);
    }

    public ManagementDto.StaffDto createStaff(ManagementDto.StaffCreateRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already exists: " + req.getEmail());
        }

        User.Role role = User.Role.STAFF;
        if (req.getRole() != null && req.getRole().equalsIgnoreCase("ADMIN")) {
            role = User.Role.ADMIN;
        }

        Branch branch = null;
        if (req.getAssignedBranchId() != null) {
            branch = branchRepository.findById(req.getAssignedBranchId()).orElse(null);
        }

        User user = new User(null, req.getName(), req.getEmail(), req.getPassword(), role, branch);
        User saved = userRepository.save(user);
        return mapToStaffDto(saved);
    }

    public ManagementDto.StaffDto updateStaff(Long id, ManagementDto.StaffUpdateRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        user.setName(req.getName());
        if (req.getRole() != null) {
            user.setRole(User.Role.valueOf(req.getRole().toUpperCase()));
        }

        if (req.getAssignedBranchId() != null) {
            Branch branch = branchRepository.findById(req.getAssignedBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found: " + req.getAssignedBranchId()));
            user.setAssignedBranch(branch);
        } else {
            user.setAssignedBranch(null);
        }

        User saved = userRepository.save(user);
        return mapToStaffDto(saved);
    }

    @Transactional
    public void deleteStaff(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        List<TransferRequest> requests = transferRequestRepository.findAll();
        for (TransferRequest tr : requests) {
            if (tr.getRequestedBy() != null && tr.getRequestedBy().getId().equals(id)) {
                tr.setRequestedBy(null);
                transferRequestRepository.save(tr);
            }
        }

        userRepository.delete(user);
    }

    private ManagementDto.StaffDto mapToStaffDto(User user) {
        Long branchId = user.getAssignedBranch() != null ? user.getAssignedBranch().getId() : null;
        String branchName = user.getAssignedBranch() != null ? user.getAssignedBranch().getName() : null;
        return new ManagementDto.StaffDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                branchId,
                branchName
        );
    }
}
