package com.springboot.MyTodoList.model;

public class BranchRequest {
    private String repo;
    private String owner;
    private String baseBranch;
    private String newBranchName;

    public BranchRequest() {}

    public BranchRequest(String repo, String owner, String baseBranch, String newBranchName) {
        this.repo = repo;
        this.owner = owner;
        this.baseBranch = baseBranch;
        this.newBranchName = newBranchName;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getbaseBranch() {
        return baseBranch;
    }

    public void setbaseBranch(String baseBranch) {
        this.baseBranch = baseBranch;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getnewBranchName() {
        return newBranchName;
    }

    public void setnewBranchName(String newBranchName) {
        this.newBranchName = newBranchName;
    }
}
