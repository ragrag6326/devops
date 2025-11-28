package com.tkb.api.gitlab;

import com.tkb.api.gitlab.dto.GitlabDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;


@FeignClient(name = "gitlabApi" , url = "${gitlab.url}")
public interface GitlabApiClient {

    @GetMapping("/projects/{projectId}/merge_requests")
    List<GitlabDto> getMRInfo (
            @PathVariable("projectId") Long projectId,
            @RequestHeader("PRIVATE-TOKEN") String token
    );
}
