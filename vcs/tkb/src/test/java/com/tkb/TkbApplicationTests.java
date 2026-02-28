package com.tkb;

import com.tkb.config.GitlabConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Optional;

@SpringBootTest

class TkbApplicationTests {

//    @Autowired
//    private GitlabConfig gitlabConfig;
//
//    @Test
//    void contextLoads() {
//
//        Long test = gitlabConfig.getProjects()
//                .stream()
//                .filter(project -> project.getName().equals("test"))
//                .findFirst()
//                .map(GitlabConfig.ProjectItem::getId)
//                .orElse(-1L);
//    }

}
