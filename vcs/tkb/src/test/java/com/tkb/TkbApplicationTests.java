package com.tkb;

import com.tkb.config.GitlabConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Optional;

@SpringBootTest

class TkbApplicationTests {

    @Autowired
    private GitlabConfig gitlabConfig;

    @Test
    void contextLoads() {

        Long test = gitlabConfig.getProjects()
                .stream()
                .filter(project -> project.getName().equals("test"))
                .findFirst()
                .map(GitlabConfig.ProjectItem::getId)
                .orElse(-1L);
    }
    @Test
    void compareVersions() {
        String version1 = "1.1.0" ;
        String version2 = "1.0.5" ;
        // 假設版本格式已在前面檢查過 (x.x.x)
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        System.out.println(Arrays.toString(parts1));
        System.out.println(Arrays.toString(parts2));

        // 只比較 Major, Minor, Patch 三個部分
        int length = Math.min(parts1.length, parts2.length);


        for (int i = 0; i < length; i++) {
            int v1 = Integer.parseInt(parts1[i]);
            int v2 = Integer.parseInt(parts2[i]);
            System.out.println(v1 + " and "+ v2);

            if (v1 != v2) {
                System.out.println(v1 - v2);
                break;
            }
        }
        // 如果前面部分都相同 (例如 1.0.0 vs 1.0.0.1)
        // 根據您的需求，我們假設都是 x.x.x 格式，此處回傳 0
        System.out.println(0);

    }
}
