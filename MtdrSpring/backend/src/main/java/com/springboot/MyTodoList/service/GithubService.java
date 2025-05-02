package com.springboot.MyTodoList.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.json.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class GithubService {
    private final String GITHUB_TOKEN;
    private final RestTemplate restTemplate = new RestTemplate();
    
    
    public GithubService() {
        GITHUB_TOKEN = "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";
    }

    public String getGITHUB_TOKEN() {
        return GITHUB_TOKEN;
    }

    // Create a branch in a repository
    public String createBranch(String owner, String repo, String baseBranch, String newBranchName) {
        try {
            System.out.println("Creating branch " + newBranchName + "from " + baseBranch + " in " + owner + "/" + repo);
            // 1️⃣ Get the latest SHA of the default branch
            String urlGetSHA = String.format(
                "https://api.github.com/repos/%s/%s/git/ref/heads/%s",
                owner, repo, baseBranch
            );

            // url to create a new branch
            String urlCreateBranch = "https://api.github.com/repos/" + owner + "/" + repo + "/git/refs";
            
            //set the headers for the request
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + GITHUB_TOKEN);
            headers.set("Accept","application/vnd.github.v3+json");

             // Make the GET request
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(urlGetSHA, HttpMethod.GET, entity, String.class);

            // validations
            if (!response.getStatusCode().is2xxSuccessful()) {
                return "❌ Failed to fetch branch SHA: " + response.getBody();
            }

            JSONObject jsonResponse = new JSONObject(response.getBody());
            String sha = jsonResponse.getJSONObject("object").getString("sha");

            // 2️⃣ Create a new branch

            JSONObject branchBody = new JSONObject();
            branchBody.put("ref", "refs/heads/" + newBranchName);
            branchBody.put("sha", sha);

            HttpEntity<String> request = new HttpEntity<>(branchBody.toString(), headers);
            ResponseEntity<String> responseCreateBranch = restTemplate.exchange(urlCreateBranch, HttpMethod.POST, request, String.class);

            if(responseCreateBranch.getStatusCode().is2xxSuccessful()) {
                return "✅ Successfully created branch: " + newBranchName;
            } else {
                return "❌ Failed to create branch: " + responseCreateBranch.getBody();
            }

        } catch (Exception e) {
            return "❌ Error: " + e.getMessage();
        }
    }
     
    public ResponseEntity<List<Map<String, String>>> getAllBranches(String owner, String repo) {
        try {
            String url = "https://api.github.com/repos/" + owner + "/" + repo + "/branches";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + GITHUB_TOKEN);
            headers.set("Accept","application/vnd.github.v3+json");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                 // Use Jackson ObjectMapper to parse JSON response
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode branches = objectMapper.readTree(response.getBody());

                List<Map<String, String>> resultList = new ArrayList<>();

                // StringBuilder to hold the result
                StringBuilder result = new StringBuilder();

                // Iterate over branches to extract name and commit url
                for (JsonNode branch : branches) {
                    String name = branch.path("name").asText();  // Get branch name
                    String commitUrl = branch.path("commit").path("url").asText();  // Get commit URL

                    // Create a map for each branch
                    Map<String, String> branchMap = new HashMap<>();
                    branchMap.put("name", name);
                    branchMap.put("commit_url", commitUrl);

                    // Add the map to the result list
                    resultList.add(branchMap);
                }

                return ResponseEntity.ok(resultList);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(List.of(Map.of("error", "Failed to fetch branches: " + response.getBody())));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(List.of(Map.of("error", "Error: " + e.getMessage())));
        }
    }
}
