package com.nexbuy.modules.admin.controller;

import com.nexbuy.modules.admin.dto.AdminBrandDto;
import com.nexbuy.modules.admin.dto.AdminBrandRequest;
import com.nexbuy.modules.admin.dto.AdminCreateAdminRequest;
import com.nexbuy.modules.admin.dto.AdminDashboardDto;
import com.nexbuy.modules.admin.dto.AdminOrderDto;
import com.nexbuy.modules.admin.dto.AdminOrderStatusRequest;
import com.nexbuy.modules.admin.dto.AdminProductDto;
import com.nexbuy.modules.admin.dto.AdminProductRequest;
import com.nexbuy.modules.admin.dto.AdminProductStockRequest;
import com.nexbuy.modules.admin.dto.AdminReturnReviewRequest;
import com.nexbuy.modules.admin.dto.AdminUploadResponse;
import com.nexbuy.modules.admin.dto.AdminUserDto;
import com.nexbuy.modules.order.dto.OrderDto;
import com.nexbuy.modules.admin.service.AdminMediaService;
import com.nexbuy.modules.admin.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AdminMediaService adminMediaService;

    public AdminController(AdminService adminService, AdminMediaService adminMediaService) {
        this.adminService = adminService;
        this.adminMediaService = adminMediaService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDto> getDashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getUsers() {
        return ResponseEntity.ok(adminService.getUsers());
    }

    @GetMapping("/admins")
    public ResponseEntity<List<AdminUserDto>> getAdmins() {
        return ResponseEntity.ok(adminService.getAdmins());
    }

    @PostMapping("/admins")
    public ResponseEntity<AdminUserDto> createAdmin(@RequestBody @Valid AdminCreateAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createAdmin(request));
    }

    @GetMapping("/brands")
    public ResponseEntity<List<AdminBrandDto>> getBrands() {
        return ResponseEntity.ok(adminService.getBrands());
    }

    @PostMapping("/brands")
    public ResponseEntity<AdminBrandDto> createBrand(@RequestBody @Valid AdminBrandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createBrand(request));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<AdminOrderDto>> getOrders() {
        return ResponseEntity.ok(adminService.getOrders());
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<OrderDto.OrderDetail> getOrderDetail(@PathVariable Long orderId) {
        return ResponseEntity.ok(adminService.getOrderDetail(orderId));
    }

    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<AdminOrderDto> updateOrderStatus(@PathVariable Long orderId,
                                                           @RequestBody @Valid AdminOrderStatusRequest request) {
        return ResponseEntity.ok(adminService.updateOrderStatus(orderId, request));
    }

    @PatchMapping("/orders/{orderId}/return-review")
    public ResponseEntity<OrderDto.OrderDetail> reviewReturn(@PathVariable Long orderId,
                                                             @RequestBody @Valid AdminReturnReviewRequest request) {
        return ResponseEntity.ok(adminService.reviewReturn(orderId, request));
    }

    @GetMapping("/products")
    public ResponseEntity<List<AdminProductDto>> getProducts() {
        return ResponseEntity.ok(adminService.getProducts());
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<AdminProductDto> getProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(adminService.getProduct(productId));
    }

    @PostMapping("/products")
    public ResponseEntity<AdminProductDto> createProduct(@RequestBody @Valid AdminProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createProduct(request));
    }

    @PutMapping("/products/{productId}")
    public ResponseEntity<AdminProductDto> updateProduct(@PathVariable Long productId,
                                                         @RequestBody @Valid AdminProductRequest request) {
        return ResponseEntity.ok(adminService.updateProduct(productId, request));
    }

    @PatchMapping("/products/{productId}/stock")
    public ResponseEntity<AdminProductDto> updateProductStock(@PathVariable Long productId,
                                                              @RequestBody AdminProductStockRequest request) {
        return ResponseEntity.ok(adminService.updateProductStock(productId, request));
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        adminService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/uploads/product-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminUploadResponse> uploadProductImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(adminMediaService.uploadProductImage(file));
    }
}
