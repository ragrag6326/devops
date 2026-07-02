package com.tkb.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Shell 腳本執行工具
 * <p>
 * 統一封裝 ProcessBuilder 的啟動、讀取輸出、等待結束邏輯，
 * 讓 Service 層只關注業務參數，不直接接觸 Process API。
 * </p>
 */
@Slf4j
public class ShellExecutor {

    private ShellExecutor() {}

    /**
     * 執行 shell 腳本，回傳所有非空白輸出行。
     * stdout 與 stderr 分開（stderr 僅寫 log，不加入 result）。
     *
     * @param args 第一個元素為腳本路徑，後續為參數
     * @return 標準輸出的每一非空白行
     */
    public static List<String> exec(String... args) {
        List<String> result = new ArrayList<>();
        ProcessBuilder pb = new ProcessBuilder("bash");
        // 把所有 args 放入指令列表
        pb.command("bash");
        List<String> cmd = new ArrayList<>();
        cmd.add("bash");
        for (String a : args) cmd.add(a);
        pb.command(cmd);

        log.debug("ShellExecutor exec: {}", String.join(" ", cmd));

        try {
            Process process = pb.start();

            // 非同步讀 stderr，避免緩衝區滿造成 deadlock
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = err.readLine()) != null) {
                        log.warn("[stderr] {}", line);
                    }
                } catch (Exception ignored) {}
            });
            stderrThread.setDaemon(true);
            stderrThread.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) result.add(line.trim());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("ShellExecutor exitCode={} cmd={}", exitCode, String.join(" ", cmd));
            }
        } catch (Exception e) {
            log.error("ShellExecutor failed cmd={}", String.join(" ", cmd), e);
        }
        return result;
    }

    /**
     * 執行腳本並合併 stdout+stderr，回傳合併輸出（供需要完整輸出的操作使用，例如 delete）。
     *
     * @param args 第一個元素為腳本路徑，後續為參數
     * @return (exitCode, 合併輸出字串)
     */
    public static ExecResult execMerged(String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("bash");
        for (String a : args) cmd.add(a);

        log.debug("ShellExecutor execMerged: {}", String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        StringBuilder output = new StringBuilder();
        int exitCode = -1;

        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[shell] {}", line);
                    output.append(line).append('\n');
                }
            }
            exitCode = process.waitFor();
        } catch (Exception e) {
            log.error("ShellExecutor execMerged failed", e);
            output.append(e.getMessage());
        }
        return new ExecResult(exitCode, output.toString().trim());
    }

    public record ExecResult(int exitCode, String output) {
        public boolean isSuccess() { return exitCode == 0; }
    }
}
