package com.nexbuy.modules.auth.service.impl;

import com.nexbuy.enums.Role;
import com.nexbuy.enums.UserStatus;
import com.nexbuy.exception.CustomException;
import com.nexbuy.integration.email.EmailService;
import com.nexbuy.integration.sms.SmsService;
import com.nexbuy.modules.auth.dto.AuthResponse;
import com.nexbuy.modules.auth.dto.ForgotPasswordRequest;
import com.nexbuy.modules.auth.dto.LoginRequest;
import com.nexbuy.modules.auth.dto.OtpRequest;
import com.nexbuy.modules.auth.dto.RegisterRequest;
import com.nexbuy.modules.auth.dto.ResendOtpRequest;
import com.nexbuy.modules.auth.dto.ResetPasswordRequest;
import com.nexbuy.modules.auth.dto.UpdatePendingEmailRequest;
import com.nexbuy.modules.auth.entity.Otp;
import com.nexbuy.modules.auth.entity.PendingRegistration;
import com.nexbuy.modules.auth.entity.User;
import com.nexbuy.modules.auth.repository.OtpRepository;
import com.nexbuy.modules.auth.repository.PendingRegistrationRepository;
import com.nexbuy.modules.auth.repository.UserRepository;
import com.nexbuy.modules.auth.service.AuthService;
import com.nexbuy.security.JwtUtil;
import com.nexbuy.util.OtpGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final SmsService smsService;
    private final JdbcTemplate jdbcTemplate;

    public AuthServiceImpl(UserRepository userRepository,
                           PendingRegistrationRepository pendingRegistrationRepository,
                           OtpRepository otpRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           EmailService emailService,
                           SmsService smsService,
                           JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.smsService = smsService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhone());

        if (request.getConfirmPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new CustomException("Passwords do not match", HttpStatus.BAD_REQUEST);
        }

        cleanupLegacyUnverifiedUser(email, phone);

        PendingRegistration pending = findReusablePendingRegistration(email, phone)
                .orElseGet(PendingRegistration::new);

        ensureEmailAndPhoneAvailableForRegistration(email, phone, pending.getId());

        pending.setEmail(email);
        pending.setPhone(phone);
        pending.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        pending.setFirstName(emptyToNull(request.getFirstName()));
        pending.setLastName(emptyToNull(request.getLastName()));
        pending.setLine1(emptyToNull(request.getLine1()));
        pending.setCity(emptyToNull(request.getCity()));
        pending.setState(emptyToNull(request.getState()));
        pending.setPostalCode(emptyToNull(request.getPostalCode()));
        pending.setCountry(emptyToNull(request.getCountry()));
        refreshPendingRegistrationOtp(pending);
        pendingRegistrationRepository.save(pending);

        sendRegisterOtp(pending);
        return buildRegisterOtpResponse(pending);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new CustomException("Invalid credentials", HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new CustomException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
        ensureUserCanAuthenticate(user);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), 3600_000);
        AuthResponse res = new AuthResponse(token, null, user.getEmail());
        res.setRequiresOtp(false);
        res.setUserId(user.getId());
        res.setRole(user.getRole().name());
        return res;
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(OtpRequest request) {
        if ("register".equalsIgnoreCase(request.getPurpose())) {
            return verifyPendingRegistration(request);
        }

        validateOtp(request.getUserId(), request.getPurpose(), request.getOtp());
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        ensureUserCanAuthenticate(user);
        String token = jwtUtil.generateToken(user.getEmail(), 3600_000);
        AuthResponse res = new AuthResponse(token, null, user.getEmail());
        res.setUserId(user.getId());
        res.setRequiresOtp(false);
        res.setRole(user.getRole().name());
        return res;
    }

    @Override
    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        if ("register".equalsIgnoreCase(request.getPurpose())) {
            PendingRegistration pending = pendingRegistrationRepository.findById(request.getUserId())
                    .orElseThrow(() -> new CustomException("Pending registration not found", HttpStatus.NOT_FOUND));
            refreshPendingRegistrationOtp(pending);
            pendingRegistrationRepository.save(pending);
            sendRegisterOtp(pending);
            return;
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        createOtp(user.getId(), request.getPurpose(), user.getEmail(), user.getPhone());
    }

    @Override
    @Transactional
    public void updatePendingRegistrationEmail(UpdatePendingEmailRequest request) {
        PendingRegistration pending = pendingRegistrationRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomException("Pending registration not found", HttpStatus.NOT_FOUND));

        String updatedEmail = normalizeEmail(request.getEmail());
        ensureEmailAndPhoneAvailableForRegistration(updatedEmail, pending.getPhone(), pending.getId());

        pending.setEmail(updatedEmail);
        refreshPendingRegistrationOtp(pending);
        pendingRegistrationRepository.save(pending);
        sendRegisterOtp(pending);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        ensureUserCanAuthenticate(user);
        createOtp(user.getId(), "reset", user.getEmail(), user.getPhone());
    }

    @Override
    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        ensureUserCanAuthenticate(user);
        if (request.getConfirmPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new CustomException("Passwords do not match", HttpStatus.BAD_REQUEST);
        }
        validateOtp(user.getId(), "reset", request.getOtp());
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new CustomException("New password must be different from your old password", HttpStatus.BAD_REQUEST);
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        emailService.sendPasswordResetSuccess(user.getEmail(), null);
        String token = jwtUtil.generateToken(user.getEmail(), 3600_000);
        AuthResponse res = new AuthResponse(token, null, user.getEmail());
        res.setUserId(user.getId());
        res.setRole(user.getRole().name());
        return res;
    }

    private AuthResponse verifyPendingRegistration(OtpRequest request) {
        PendingRegistration pending = pendingRegistrationRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomException("Pending registration not found", HttpStatus.NOT_FOUND));

        validatePendingRegistrationOtp(pending, request.getOtp());
        cleanupLegacyUnverifiedUser(pending.getEmail(), pending.getPhone());
        ensureEmailAndPhoneAvailableForRegistration(pending.getEmail(), pending.getPhone(), pending.getId());

        User user = new User();
        user.setEmail(pending.getEmail());
        user.setPhone(pending.getPhone());
        user.setPasswordHash(pending.getPasswordHash());
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(Role.USER);
        user.setLastLoginAt(LocalDateTime.now());
        User savedUser = userRepository.save(user);

        createProfileAndAddress(savedUser.getId(), pending);
        pendingRegistrationRepository.delete(pending);

        emailService.sendWelcome(savedUser.getEmail(), pending.getFirstName());
        String token = jwtUtil.generateToken(savedUser.getEmail(), 3600_000);
        AuthResponse res = new AuthResponse(token, null, savedUser.getEmail());
        res.setUserId(savedUser.getId());
        res.setRequiresOtp(false);
        res.setRole(savedUser.getRole().name());
        return res;
    }

    private void createProfileAndAddress(Long userId, PendingRegistration pending) {
        if (pending.getFirstName() != null || pending.getLastName() != null) {
            jdbcTemplate.update(
                    "insert into user_profiles (user_id, first_name, last_name, created_at, updated_at) values (?, ?, ?, current_timestamp, current_timestamp)",
                    userId,
                    pending.getFirstName(),
                    pending.getLastName()
            );
        }

        if (hasText(pending.getLine1()) && hasText(pending.getCity()) && hasText(pending.getState())
                && hasText(pending.getPostalCode()) && hasText(pending.getCountry())) {
            jdbcTemplate.update(
                    "insert into addresses (user_id, label, line1, line2, city, state, postal_code, country, is_default, created_at, updated_at) values (?, ?, ?, null, ?, ?, ?, ?, true, current_timestamp, current_timestamp)",
                    userId,
                    "Home",
                    pending.getLine1(),
                    pending.getCity(),
                    pending.getState(),
                    pending.getPostalCode(),
                    pending.getCountry()
            );
        }
    }

    private Optional<PendingRegistration> findReusablePendingRegistration(String email, String phone) {
        Optional<PendingRegistration> byEmail = pendingRegistrationRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            return byEmail;
        }
        if (phone != null) {
            return pendingRegistrationRepository.findByPhone(phone);
        }
        return Optional.empty();
    }

    private void ensureEmailAndPhoneAvailableForRegistration(String email, String phone, Long pendingId) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException("Email already registered", HttpStatus.CONFLICT);
        }
        if (pendingId == null) {
            if (pendingRegistrationRepository.existsByEmail(email)) {
                throw new CustomException("Email already pending verification", HttpStatus.CONFLICT);
            }
        } else if (pendingRegistrationRepository.existsByEmailAndIdNot(email, pendingId)) {
            throw new CustomException("Email already pending verification", HttpStatus.CONFLICT);
        }

        if (phone == null) {
            return;
        }
        if (userRepository.existsByPhone(phone)) {
            throw new CustomException("Phone number already registered", HttpStatus.CONFLICT);
        }
        if (pendingId == null) {
            if (pendingRegistrationRepository.existsByPhone(phone)) {
                throw new CustomException("Phone number already pending verification", HttpStatus.CONFLICT);
            }
        } else if (pendingRegistrationRepository.existsByPhoneAndIdNot(phone, pendingId)) {
            throw new CustomException("Phone number already pending verification", HttpStatus.CONFLICT);
        }
    }

    private void refreshPendingRegistrationOtp(PendingRegistration pending) {
        pending.setOtpCode(OtpGenerator.generate(6));
        pending.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
    }

    private void validatePendingRegistrationOtp(PendingRegistration pending, String code) {
        if (pending.getOtpExpiresAt() == null || pending.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CustomException("OTP expired", HttpStatus.BAD_REQUEST);
        }
        if (!pending.getOtpCode().equals(code)) {
            throw new CustomException("Invalid OTP", HttpStatus.BAD_REQUEST);
        }
    }

    private void sendRegisterOtp(PendingRegistration pending) {
        if (hasText(pending.getEmail())) {
            emailService.sendOtp(pending.getEmail(), pending.getOtpCode(), "register");
        }
        if (hasText(pending.getPhone())) {
            smsService.sendOtp(pending.getPhone(), pending.getOtpCode(), "register");
        }
    }

    private AuthResponse buildRegisterOtpResponse(PendingRegistration pending) {
        AuthResponse res = new AuthResponse();
        res.setRequiresOtp(true);
        res.setUserId(pending.getId());
        res.setEmail(pending.getEmail());
        res.setRole(Role.USER.name());
        res.setToken(null);
        return res;
    }

    private Otp createOtp(Long userId, String purpose, String email, String phone) {
        Otp otp = new Otp();
        otp.setUserId(userId);
        otp.setPurpose(purpose);
        otp.setCode(OtpGenerator.generate(6));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        otpRepository.save(otp);

        if (hasText(email)) {
            emailService.sendOtp(email, otp.getCode(), purpose);
        }
        if (hasText(phone)) {
            smsService.sendOtp(phone, otp.getCode(), purpose);
        }
        return otp;
    }

    private void validateOtp(Long userId, String purpose, String code) {
        Otp otp = otpRepository.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(userId, purpose)
                .orElseThrow(() -> new CustomException("OTP not found", HttpStatus.BAD_REQUEST));
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CustomException("OTP expired", HttpStatus.BAD_REQUEST);
        }
        if (!otp.getCode().equals(code)) {
            throw new CustomException("Invalid OTP", HttpStatus.BAD_REQUEST);
        }
        otp.setUsed(true);
        otpRepository.save(otp);
    }

    private void cleanupLegacyUnverifiedUser(String email, String phone) {
        Optional<User> byEmail = userRepository.findByEmail(email).filter(this::isLegacyUnverifiedUser);
        byEmail.ifPresent(userRepository::delete);

        if (phone != null) {
            userRepository.findByPhone(phone)
                    .filter(this::isLegacyUnverifiedUser)
                    .filter(user -> byEmail.map(existing -> !existing.getId().equals(user.getId())).orElse(true))
                    .ifPresent(userRepository::delete);
        }
    }

    private boolean isLegacyUnverifiedUser(User user) {
        return user.getStatus() == UserStatus.INACTIVE
                && user.getRole() == Role.USER
                && user.getOauthProvider() == null
                && user.getLastLoginAt() == null;
    }

    private void ensureUserCanAuthenticate(User user) {
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new CustomException("Your account is blocked", HttpStatus.FORBIDDEN);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new CustomException("Please verify your email before signing in", HttpStatus.FORBIDDEN);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String value = phone.trim();
        return value.isEmpty() ? null : value;
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}