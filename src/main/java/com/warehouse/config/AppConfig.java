package com.warehouse.config;

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
import org.modelmapper.ModelMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public CommandLineRunner initDatabase(
            UserRepository userRepository,
            BranchRepository branchRepository,
            ProductRepository productRepository,
            StockRepository stockRepository,
            TransferRequestRepository transferRequestRepository,
            PurchaseRequestRepository purchaseRequestRepository
    ) {
        return args -> {
            if (userRepository.count() == 0) {
                Branch branch1 = new Branch(null, "Central Warehouse", "Building A, Logistics Park");
                Branch branch2 = new Branch(null, "East Distribution Center", "Sector 5, Industrial Area");
                branch1 = branchRepository.save(branch1);
                branch2 = branchRepository.save(branch2);

                User admin = new User(null, "System Administrator", "admin@warehouse.com", "admin123", User.Role.ADMIN, null);
                User staff = new User(null, "Rahul Sharma", "staff@warehouse.com", "staff123", User.Role.STAFF, branch1);
                admin = userRepository.save(admin);
                staff = userRepository.save(staff);

                Product p1 = new Product(null, "PRD-101", "Industrial Pallet", "Heavy-duty wooden storage pallets", 20);
                Product p2 = new Product(null, "PRD-102", "Steel Storage Rack", "Modular 4-shelf industrial racking unit", 10);
                Product p3 = new Product(null, "PRD-103", "Cardboard Packing Boxes (50pk)", "Corrugated packing boxes 18x12x12", 50);
                Product p4 = new Product(null, "PRD-104", "Heavy-Duty Packing Tape (12pk)", "Transparent industrial packaging tape 48mm", 30);
                p1 = productRepository.save(p1);
                p2 = productRepository.save(p2);
                p3 = productRepository.save(p3);
                p4 = productRepository.save(p4);

                stockRepository.save(new Stock(null, branch1, p1, 100));
                stockRepository.save(new Stock(null, branch1, p2, 15));
                stockRepository.save(new Stock(null, branch1, p3, 40));
                stockRepository.save(new Stock(null, branch1, p4, 80));

                stockRepository.save(new Stock(null, branch2, p1, 25));
                stockRepository.save(new Stock(null, branch2, p2, 5));
                stockRepository.save(new Stock(null, branch2, p3, 60));
                stockRepository.save(new Stock(null, branch2, p4, 20));

                TransferRequest tr = new TransferRequest(null, branch1, branch2, p1, 15, TransferRequest.Status.PENDING, staff, LocalDateTime.now());
                transferRequestRepository.save(tr);

                PurchaseRequest pr = new PurchaseRequest(null, branch1, p3, 100, PurchaseRequest.Status.PENDING, LocalDateTime.now());
                purchaseRequestRepository.save(pr);
            }
        };
    }
}
