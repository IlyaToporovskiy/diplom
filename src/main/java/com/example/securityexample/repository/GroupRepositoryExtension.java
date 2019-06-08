package com.example.securityexample.repository;



import com.example.securityexample.entity.Group;

import java.util.List;

public interface GroupRepositoryExtension {
    List<String> getAllGroupNames();
    void create(Group group);
}
