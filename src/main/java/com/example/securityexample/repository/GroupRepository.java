package com.example.securityexample.repository;

import com.example.securityexample.entity.Group;
import org.springframework.data.ldap.repository.LdapRepository;
import org.springframework.data.ldap.repository.Query;

import javax.naming.Name;
import java.util.Collection;

public interface GroupRepository extends LdapRepository<Group>, GroupRepositoryExtension {
    public final static String USER_GROUP = "ROLE_USER";

    Group findByName(String groupName);

    @Query("(member={0})")
    Collection<Group> findByMember(Name member);
}
