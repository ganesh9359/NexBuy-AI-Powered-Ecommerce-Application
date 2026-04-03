package com.nexbuy.util;

import java.security.SecureRandom;

public class OtpGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private OtpGenerator() {}

    public static String generate(int digits) {
        int bound = (int) Math.pow(10, digits);
        int code = RANDOM.nextInt(bound);
        return String.format("%0" + digits + "d", code);
    }
}
