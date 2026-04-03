package com.nexbuy.modules.admin.service;

import com.nexbuy.modules.admin.dto.AdminBrandDto;
import com.nexbuy.modules.admin.dto.AdminBrandRequest;
import com.nexbuy.modules.admin.dto.AdminCreateAdminRequest;
import com.nexbuy.modules.admin.dto.AdminDashboardDto;
import com.nexbuy.modules.admin.dto.AdminOrderDto;
import com.nexbuy.modules.admin.dto.AdminOrderStatusRequest;
import com.nexbuy.modules.admin.dto.AdminProductDto;
import com.nexbuy.modules.admin.dto.AdminProductRequest;
import com.nexbuy.modules.admin.dto.AdminUserDto;
import com.nexbuy.modules.order.dto.OrderDto;

import java.util.List;

public interface AdminService {
    AdminDashboardDto getDashboard();
    List<AdminUserDto> getUsers();
    List<AdminUserDto> getAdmins();
    AdminUserDto createAdmin(AdminCreateAdminRequest request);
    List<AdminBrandDto> getBrands();
    AdminBrandDto createBrand(AdminBrandRequest request);
    List<AdminOrderDto> getOrders();
    OrderDto.OrderDetail getOrderDetail(Long orderId);
    AdminOrderDto updateOrderStatus(Long orderId, AdminOrderStatusRequest request);
    List<AdminProductDto> getProducts();
    AdminProductDto getProduct(Long productId);
    AdminProductDto createProduct(AdminProductRequest request);
    AdminProductDto updateProduct(Long productId, AdminProductRequest request);
    void deleteProduct(Long productId);
}