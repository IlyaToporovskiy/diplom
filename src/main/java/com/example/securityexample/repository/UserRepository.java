package com.example.securityexample.repository;

import com.example.securityexample.entity.User;
import org.springframework.data.ldap.repository.LdapRepository;

import java.util.List;

public interface UserRepository extends LdapRepository {
    User findByEmployeeNumber(int employeeNumber);
    List<User> findByFullNameContains(String name);

}
