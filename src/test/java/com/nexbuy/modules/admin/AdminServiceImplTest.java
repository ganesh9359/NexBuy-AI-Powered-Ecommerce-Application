package com.nexbuy.modules.admin;

import com.nexbuy.exception.CustomException;
import com.nexbuy.integration.email.EmailService;
import com.nexbuy.modules.admin.dto.*;
import com.nexbuy.modules.admin.service.impl.AdminServiceImpl;
import com.nexbuy.modules.auth.entity.User;
import com.nexbuy.modules.auth.repository.UserRepository;
import com.nexbuy.modules.commerce.CommerceSupport;
import com.nexbuy.modules.payment.integration.PaymentGatewayClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Tests")
class AdminServiceImplTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CommerceSupport commerceSupport;
    @Mock EmailService emailService;
    @Mock PaymentGatewayClient paymentGatewayClient;

    @InjectMocks AdminServiceImpl adminService;

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDashboard")
    class DashboardTests {

        @Test
        @DisplayName("Returns dashboard with all counts")
        void getDashboard_success() {
            when(jdbcTemplate.queryForObject(contains("count(*) from users"), eq(Number.class))).thenReturn(10L);
            when(jdbcTemplate.queryForObject(contains("count(*) from products"), eq(Number.class))).thenReturn(50L);
            when(jdbcTemplate.queryForObject(contains("count(*) from orders"), eq(Number.class))).thenReturn(25L);
            when(jdbcTemplate.queryForObject(contains("'pending'"), eq(Number.class))).thenReturn(5L);
            when(jdbcTemplate.queryForObject(contains("'delivered'"), eq(Number.class))).thenReturn(15L);
            doNothing().when(jdbcTemplate).query(anyString(), any(org.springframework.jdbc.core.RowCallbackHandler.class));

            AdminDashboardDto dto = adminService.getDashboard();

            assertThat(dto.getTotalUsers()).isEqualTo(10L);
            assertThat(dto.getTotalProducts()).isEqualTo(50L);
            assertThat(dto.getTotalOrders()).isEqualTo(25L);
        }
    }

    // ── Orders ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Order Management")
    class OrderTests {

        @Test
        @DisplayName("getOrders returns list from DB")
        void getOrders_returnsList() {
            AdminOrderDto order = new AdminOrderDto();
            order.setId(1L);
            order.setOrderNumber("NB-001");
            when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(order));

            List<AdminOrderDto> result = adminService.getOrders();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOrderNumber()).isEqualTo("NB-001");
        }

        @Test
        @DisplayName("updateOrderStatus with unknown order throws NOT_FOUND")
        void updateOrderStatus_notFound() {
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyLong()))
                    .thenThrow(new EmptyResultDataAccessException(1));

            AdminOrderStatusRequest req = new AdminOrderStatusRequest();
            req.setStatus("shipped");

            assertThatThrownBy(() -> adminService.updateOrderStatus(999L, req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("updateOrderStatus with invalid status throws BAD_REQUEST")
        void updateOrderStatus_invalidStatus() {
            AdminOrderDto existing = new AdminOrderDto();
            existing.setId(1L);
            existing.setStatus("PENDING");
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyLong()))
                    .thenReturn(existing);

            AdminOrderStatusRequest req = new AdminOrderStatusRequest();
            req.setStatus("flying");

            assertThatThrownBy(() -> adminService.updateOrderStatus(1L, req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Cannot update DELIVERED order to different status")
        void updateOrderStatus_deliveredLocked() {
            AdminOrderDto existing = new AdminOrderDto();
            existing.setId(1L);
            existing.setStatus("DELIVERED");
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyLong()))
                    .thenReturn(existing);

            AdminOrderStatusRequest req = new AdminOrderStatusRequest();
            req.setStatus("pending");

            assertThatThrownBy(() -> adminService.updateOrderStatus(1L, req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Cannot update CANCELLED order to different status")
        void updateOrderStatus_cancelledLocked() {
            AdminOrderDto existing = new AdminOrderDto();
            existing.setId(1L);
            existing.setStatus("CANCELLED");
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyLong()))
                    .thenReturn(existing);

            AdminOrderStatusRequest req = new AdminOrderStatusRequest();
            req.setStatus("shipped");

            assertThatThrownBy(() -> adminService.updateOrderStatus(1L, req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }

    // ── Products ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Product Management")
    class ProductTests {

        @Test
        @DisplayName("getProduct with unknown ID throws NOT_FOUND")
        void getProduct_notFound() {
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyLong()))
                    .thenThrow(new EmptyResultDataAccessException(1));

            assertThatThrownBy(() -> adminService.getProduct(999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("deleteProduct with unknown ID throws NOT_FOUND")
        void deleteProduct_notFound() {
            when(jdbcTemplate.update(anyString(), anyLong())).thenReturn(0);

            assertThatThrownBy(() -> adminService.deleteProduct(999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("updateProductStock with null request throws BAD_REQUEST")
        void updateProductStock_nullRequest() {
            assertThatThrownBy(() -> adminService.updateProductStock(1L, null))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("updateProductStock with no qty or delta throws BAD_REQUEST")
        void updateProductStock_noQtyOrDelta() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyLong())).thenReturn(1L);
            when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(10);

            AdminProductStockRequest req = new AdminProductStockRequest();
            // both null

            assertThatThrownBy(() -> adminService.updateProductStock(1L, req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }

    // ── Admin Users ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Admin User Management")
    class AdminUserTests {

        @Test
        @DisplayName("createAdmin with duplicate email throws CONFLICT")
        void createAdmin_duplicateEmail() {
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            AdminCreateAdminRequest req = new AdminCreateAdminRequest();
            req.setEmail("admin@nexbuy.com");
            req.setPassword("Admin1234!");

            assertThatThrownBy(() -> adminService.createAdmin(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("createAdmin with duplicate phone throws CONFLICT")
        void createAdmin_duplicatePhone() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByPhone(anyString())).thenReturn(true);

            AdminCreateAdminRequest req = new AdminCreateAdminRequest();
            req.setEmail("newadmin@nexbuy.com");
            req.setPassword("Admin1234!");
            req.setPhone("+919876543210");

            assertThatThrownBy(() -> adminService.createAdmin(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        }
    }

    // ── Return Requests ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Return Requests")
    class ReturnRequestTests {

        @Test
        @DisplayName("getReturnRequest with unknown ID throws NOT_FOUND")
        void getReturnRequest_notFound() {
            when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyLong()))
                    .thenThrow(new EmptyResultDataAccessException(1));

            assertThatThrownBy(() -> adminService.getReturnRequest(999L))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("updateReturnRequest with unknown ID throws NOT_FOUND")
        void updateReturnRequest_notFound() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyLong()))
                    .thenThrow(new EmptyResultDataAccessException(1));

            AdminReturnUpdateRequest req = new AdminReturnUpdateRequest();
            req.setAction("approve");

            assertThatThrownBy(() -> adminService.updateReturnRequest(999L, req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // ── Brands ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Brand Management")
    class BrandTests {

        @Test
        @DisplayName("createBrand with duplicate name throws CONFLICT")
        void createBrand_duplicate() {
            when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString(), anyString()))
                    .thenReturn(1L);

            AdminBrandRequest req = new AdminBrandRequest();
            req.setName("Samsung");

            assertThatThrownBy(() -> adminService.createBrand(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        }
    }
}
