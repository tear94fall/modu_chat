package com.example.modumessenger.Global;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

public final class HashUtil {

    public static String getSHA256Hash(String input) {
        return Hashing.sha256()
                .hashString(input, StandardCharsets.UTF_8)
                .toString();
    }
}
