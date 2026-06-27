package com.cdm.util;

/** Small shared sanitizers for untrusted free-text crossing the client -> server boundary. */
public final class Sanitize {
    private Sanitize() {}

    /** Trim to {@code maxLen} and strip control characters and the formatting section sign. */
    public static String title(String text, int maxLen) {
        if (text == null) return "";
        String trimmed = text.length() > maxLen ? text.substring(0, maxLen) : text;
        return trimmed.replaceAll("[\\p{Cntrl}§]", "");
    }
}
