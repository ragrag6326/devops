package com.tkb.utils;

import com.tkb.api.gitlab.dto.GitlabMrChangeDto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

public final class DiffHashUtil {

    private DiffHashUtil() {
    }

    public static String hashChanges(List<GitlabMrChangeDto.ChangeItem> changes) {
        if (changes == null || changes.isEmpty()) {
            return hashString("");
        }
        StringBuilder sb = new StringBuilder();
        for (GitlabMrChangeDto.ChangeItem item : changes) {
            sb.append(item.getOld_path()).append('|')
                    .append(item.getNew_path()).append('|')
                    .append(item.getDiff()).append('\n');
        }
        return hashString(sb.toString());
    }

    private static String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
