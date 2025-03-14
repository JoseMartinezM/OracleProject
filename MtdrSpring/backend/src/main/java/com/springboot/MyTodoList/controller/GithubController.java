package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.service.GithubService;

import io.swagger.models.Response;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.springboot.MyTodoList.model.BranchRequest;


@RestController
@RequestMapping("/api/github")
public class GithubController {

    @Autowired
    private GithubService githubService;

    // Create a new branch in a GitHub repository
    @PostMapping("/create-branch")
    public String createBranch(@RequestBody BranchRequest request) {
        return githubService.createBranch(request.getOwner(), request.getRepo(), request.getbaseBranch(), request.getnewBranchName());
    }
    @GetMapping("/get-branches")
    public ResponseEntity<List<Map<String, String>>> getAllBranches(@RequestParam String owner, @RequestParam String repo) {
        return githubService.getAllBranches(owner, repo);
    }
    

}
