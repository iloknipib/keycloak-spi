package com.dehaat.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author sushil
 */
public class MobileNumberValidator {
    private final static String PHONE_REGEX = "^[7-9][0-9]{9}$";

    public static boolean isValid(String mobile) {
        if (mobile.length() != 10) {
            return false;
        }
        Pattern pattern = Pattern.compile(PHONE_REGEX);
        Matcher matcher = pattern.matcher(mobile);
        boolean isValid = matcher.matches();
        return isValid;
    }
}
