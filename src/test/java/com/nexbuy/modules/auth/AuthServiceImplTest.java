package com.nexbuy.modules.auth;

import com.nexbuy.enums.Role;
import com.nexbuy.enums.UserStatus;
import com.nexbuy.exception.CustomException;
import com.nexbuy.integration.email.EmailService;
import com.nexbuy.integration.sms.SmsService;
import com.nexbuy.modules.auth.dto.*;
import com.nexbuy.modules.auth.entity.Otp;
import com.nexbuy.modules.auth.entity.PendingRegistration;
import com.nexbuy.modules.auth.entity.User;
import com.nexbuy.modules.auth.repository.OtpRepository;
import com.nexbuy.modules.auth.repository.PendingRegistrationRepository;
import com.nexbuy.modules.auth.repository.UserRepository;
import com.nexbuy.modules.auth.service.impl.AuthServiceImpl;
import com.nexbuy.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PendingRegistrationRepository pendingRegistrationRepository;
    @Mock OtpRepository otpRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock EmailService emailService;
    @Mock SmsService smsService;
    @Mock JdbcTemplate jdbcTemplate;

    @InjectMocks AuthServiceImpl authService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User activeUser(String email) {
        User u = new User();
        u.setId(1L);
        u.setEmail(email);
        u.setPasswordHash("hashed");
        u.setRole(Role.USER);
        u.setStatus(UserStatus.ACTIVE);
        return u;
    }

    private RegisterRequest registerRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setEmail("test@nexbuy.com");
        r.setPassword("Password1!");
        r.setConfirmPassword("Password1!");
        r.setFirstName("Test");
        r.setLastName("User");
        return r;
    }

    // ── Register ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Register")
    class RegisterTests {

        @Test
        @DisplayName("Successful registration creates pending record and returns OTP response")
        void register_success() {
            RegisterRequest req = registerRequest();
            when(pendingRegistrationRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            PendingRegistration saved = new PendingRegistration();
            saved.setId(10L);
            saved.setEmail(req.getEmail());
            saved.setOtpCode("123456");
            saved.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
            when(pendingRegistrationRepository.save(any())).thenReturn(saved);

            AuthResponse res = authService.register(req);

            assertThat(res.isRequiresOtp()).isTrue();
            assertThat(res.getEmail()).isEqualTo(req.getEmail());
            verify(emailService).sendOtp(eq(req.getEmail()), anyString(), eq("register"));
        }

        @Test
        @DisplayName("Mismatched passwords throw BAD_REQUEST")
        void register_passwordMismatch() {
            RegisterRequest req = registerRequest();
            req.setConfirmPassword("WrongPassword!");

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Duplicate email throws CONFLICT")
        void register_duplicateEmail() {
            RegisterRequest req = registerRequest();
            when(pendingRegistrationRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("Null confirm password throws BAD_REQUEST")
        void register_nullConfirmPassword() {
            RegisterRequest req = registerRequest();
            req.setConfirmPassword(null);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("Valid credentials return JWT token")
        void login_success() {
            User user = activeUser("user@nexbuy.com");
            LoginRequest req = new LoginRequest();
            req.setEmail("user@nexbuy.com");
            req.setPassword("Password1!");

            when(userRepository.findByEmail("user@nexbuy.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Password1!", "hashed")).thenReturn(true);
            when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("jwt-token");
            when(userRepository.save(any())).thenReturn(user);

            AuthResponse res = authService.login(req);

            assertThat(res.getToken()).isEqualTo("jwt-token");
            assertThat(res.getEmail()).isEqualTo("user@nexbuy.com");
            assertThat(res.isRequiresOtp()).isFalse();
        }

        @Test
        @DisplayName("Wrong password throws UNAUTHORIZED")
        void login_wrongPassword() {
            User user = activeUser("user@nexbuy.com");
            LoginRequest req = new LoginRequest();
            req.setEmail("user@nexbuy.com");
            req.setPassword("WrongPass!");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }

        @Test
        @DisplayName("Non-existent email throws UNAUTHORIZED")
        void login_userNotFound() {
            LoginRequest req = new LoginRequest();
            req.setEmail("ghost@nexbuy.com");
            req.setPassword("pass");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
        }

        @Test
        @DisplayName("Blocked user throws FORBIDDEN")
        void login_blockedUser() {
            User user = activeUser("blocked@nexbuy.com");
            user.setStatus(UserStatus.BLOCKED);
            LoginRequest req = new LoginRequest();
            req.setEmail("blocked@nexbuy.com");
            req.setPassword("Password1!");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("Inactive user throws FORBIDDEN")
        void login_inactiveUser() {
            User user = activeUser("inactive@nexbuy.com");
            user.setStatus(UserStatus.INACTIVE);
            LoginRequest req = new LoginRequest();
            req.setEmail("inactive@nexbuy.com");
            req.setPassword("Password1!");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        }
    }

    // ── OTP Verification ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("OTP Verification")
    class OtpVerificationTests {

        @Test
        @DisplayName("Valid OTP for reset purpose returns JWT")
        void verifyOtp_reset_success() {
            User user = activeUser("user@nexbuy.com");
            Otp otp = new Otp();
            otp.setUserId(1L);
            otp.setPurpose("reset");
            otp.setCode("654321");
            otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            otp.setUsed(false);

            OtpRequest req = new OtpRequest();
            req.setUserId(1L);
            req.setPurpose("reset");
            req.setOtp("654321");

            when(otpRepository.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(1L, "reset"))
                    .thenReturn(Optional.of(otp));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("jwt-token");
            when(otpRepository.save(any())).thenReturn(otp);

            AuthResponse res = authService.verifyOtp(req);

            assertThat(res.getToken()).isEqualTo("jwt-token");
        }

        @Test
        @DisplayName("Expired OTP throws BAD_REQUEST")
        void verifyOtp_expired() {
            Otp otp = new Otp();
            otp.setCode("123456");
            otp.setExpiresAt(LocalDateTime.now().minusMinutes(1));
            otp.setUsed(false);

            OtpRequest req = new OtpRequest();
            req.setUserId(1L);
            req.setPurpose("reset");
            req.setOtp("123456");

            when(otpRepository.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(anyLong(), anyString()))
                    .thenReturn(Optional.of(otp));

            assertThatThrownBy(() -> authService.verifyOtp(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Wrong OTP code throws BAD_REQUEST")
        void verifyOtp_wrongCode() {
            Otp otp = new Otp();
            otp.setCode("999999");
            otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            otp.setUsed(false);

            OtpRequest req = new OtpRequest();
            req.setUserId(1L);
            req.setPurpose("reset");
            req.setOtp("000000");

            when(otpRepository.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(anyLong(), anyString()))
                    .thenReturn(Optional.of(otp));

            assertThatThrownBy(() -> authService.verifyOtp(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }

    // ── Forgot / Reset Password ───────────────────────────────────────────────

    @Nested
    @DisplayName("Forgot & Reset Password")
    class PasswordTests {

        @Test
        @DisplayName("Forgot password sends OTP for existing active user")
        void forgotPassword_success() {
            User user = activeUser("user@nexbuy.com");
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("user@nexbuy.com");

            when(userRepository.findByEmail("user@nexbuy.com")).thenReturn(Optional.of(user));
            when(otpRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.forgotPassword(req);

            verify(emailService).sendOtp(eq("user@nexbuy.com"), anyString(), eq("reset"));
        }

        @Test
        @DisplayName("Forgot password for unknown email throws NOT_FOUND")
        void forgotPassword_userNotFound() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("ghost@nexbuy.com");
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.forgotPassword(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("Reset password with same password throws BAD_REQUEST")
        void resetPassword_samePassword() {
            User user = activeUser("user@nexbuy.com");
            Otp otp = new Otp();
            otp.setCode("123456");
            otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
            otp.setUsed(false);

            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setEmail("user@nexbuy.com");
            req.setOtp("123456");
            req.setNewPassword("SamePass1!");
            req.setConfirmPassword("SamePass1!");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
            when(otpRepository.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(anyLong(), anyString()))
                    .thenReturn(Optional.of(otp));
            when(otpRepository.save(any())).thenReturn(otp);
            when(passwordEncoder.matches("SamePass1!", "hashed")).thenReturn(true);

            assertThatThrownBy(() -> authService.resetPassword(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("Reset password with mismatched confirm throws BAD_REQUEST")
        void resetPassword_mismatch() {
            User user = activeUser("user@nexbuy.com");
            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setEmail("user@nexbuy.com");
            req.setOtp("123456");
            req.setNewPassword("NewPass1!");
            req.setConfirmPassword("Different!");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.resetPassword(req))
                    .isInstanceOf(CustomException.class)
                    .satisfies(e -> assertThat(((CustomException) e).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }
}
