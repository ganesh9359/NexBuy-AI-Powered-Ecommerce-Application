package com.nexbuy.integration.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {
    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    public void sendOtp(String phone, String code, String purpose) {
        // Stub: integrate Twilio or other provider here
        log.info("SMS OTP [{}] to {} for {}", code, phone, purpose);
    }
}
