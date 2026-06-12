package com.tkb.api.gitlab;

import com.tkb.api.gitlab.dto.GitlabDto;
import com.tkb.api.gitlab.dto.GitlabMrChangeDto;
import com.tkb.api.gitlab.dto.GitlabMrNoteRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "gitlabApi", url = "${gitlab.url}")
public interface GitlabApiClient {

    @GetMapping("/projects/{projectId}/merge_requests")
    List<GitlabDto> getMRInfo(
            @PathVariable("projectId") Long projectId,
            @RequestHeader("PRIVATE-TOKEN") String token,
            @RequestParam(value = "state", required = false) String state
    );

    @GetMapping("/projects/{projectId}/merge_requests/{mrIid}")
    GitlabDto getMrByIid(
            @PathVariable("projectId") Long projectId,
            @PathVariable("mrIid") Integer mrIid,
            @RequestHeader("PRIVATE-TOKEN") String token
    );

    @GetMapping("/projects/{projectId}/merge_requests/{mrIid}/changes")
    GitlabMrChangeDto getMrChanges(
            @PathVariable("projectId") Long projectId,
            @PathVariable("mrIid") Integer mrIid,
            @RequestHeader("PRIVATE-TOKEN") String token
    );

    @PostMapping("/projects/{projectId}/merge_requests/{mrIid}/notes")
    void createMrNote(
            @PathVariable("projectId") Long projectId,
            @PathVariable("mrIid") Integer mrIid,
            @RequestHeader("PRIVATE-TOKEN") String token,
            @RequestBody GitlabMrNoteRequest body
    );
}
