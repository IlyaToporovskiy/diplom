package com.example.securityexample.service;


import com.example.securityexample.entity.DirectoryType;
import com.example.securityexample.entity.Group;
import com.example.securityexample.entity.User;
import com.example.securityexample.repository.GroupRepository;
import com.example.securityexample.repository.UserRepository;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.support.BaseLdapNameAware;

import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.ldap.support.LdapUtils;

import javax.naming.Name;
import javax.naming.ldap.LdapName;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class UserService implements BaseLdapNameAware {
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private LdapName baseLdapPath;
    private DirectoryType directoryType;

    @Autowired
    public UserService(UserRepository userRepository, GroupRepository groupRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    public Group getUserGroup() {
        return groupRepository.findByName(GroupRepository.USER_GROUP);
    }

    public void setDirectoryType(DirectoryType directoryType) {
        this.directoryType = directoryType;
    }

    @Override
    public void setBaseLdapPath(LdapName baseLdapPath) {
        this.baseLdapPath = baseLdapPath;
    }

    public Iterable<User> findAll() {
        return userRepository.findAll();
    }

    public User findUser(String userId) {
        return  userRepository.findOne(LdapUtils.newLdapName(userId));
    }


    public User createUser(User user) {
        User savedUser = userRepository.save(user);

        Group userGroup = getUserGroup();

        // The DN the member attribute must be absolute
        userGroup.addMember(toAbsoluteDn(savedUser.getId()));
        groupRepository.save(userGroup);

        return savedUser;
    }

    public LdapName toAbsoluteDn(Name relativeName) {
        return LdapNameBuilder.newInstance(baseLdapPath)
                .add(relativeName)
                .build();
    }

    /**
     * This method expects absolute DNs of group members. In order to find the actual users
     * the DNs need to have the base LDAP path removed.
     *
     * @param absoluteIds
     * @return
     */
    public Set<User> findAllMembers(Iterable<Name> absoluteIds) {
        return Sets.newLinkedHashSet(userRepository.findAll(toRelativeIds(absoluteIds)));
    }

    public Iterable<Name> toRelativeIds(Iterable<Name> absoluteIds) {
        return Iterables.transform(absoluteIds, new Function<Name, Name>() {
            @Override
            public Name apply(Name input) {
                return LdapUtils.removeFirst(input, baseLdapPath);
            }
        });
    }

    public User updateUser(String userId, User user) {
        LdapName originalId = LdapUtils.newLdapName(userId);
        User existingUser = userRepository.findOne(originalId);

        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setFullName(user.getFullName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setTitle(user.getTitle());
        existingUser.setDepartment(user.getDepartment());
        existingUser.setUnit(user.getUnit());

        if (directoryType == DirectoryType.AD) {
            return updateUserAd(originalId, existingUser);
        } else {
            return updateUserStandard(originalId, existingUser);
        }
    }


    /**
     * Update the user and - if its id changed - update all group references to the user.
     *
     * @param originalId the original id of the user.
     * @param existingUser the user, populated with new data
     *
     * @return the updated entry
     */
    private User updateUserStandard(LdapName originalId, User existingUser) {
        User savedUser = userRepository.save(existingUser);

        if(!originalId.equals(savedUser.getId())) {
            // The user has moved - we need to update group references.
            LdapName oldMemberDn = toAbsoluteDn(originalId);
            LdapName newMemberDn = toAbsoluteDn(savedUser.getId());

            Collection<Group> groups = groupRepository.findByMember(oldMemberDn);
            updateGroupReferences(groups, oldMemberDn, newMemberDn);
        }
        return savedUser;
    }

    private User updateUserAd(LdapName originalId, User existingUser) {
        LdapName oldMemberDn = toAbsoluteDn(originalId);
        Collection<Group> groups = groupRepository.findByMember(oldMemberDn);

        User savedUser = userRepository.save(existingUser);
        LdapName newMemberDn = toAbsoluteDn(savedUser.getId());

        if(!originalId.equals(savedUser.getId())) {
            // The user has moved - we need to update group references.
            updateGroupReferences(groups, oldMemberDn, newMemberDn);
        }
        return savedUser;
    }


    private void updateGroupReferences(Collection<Group> groups, Name originalId, Name newId) {
        for (Group group : groups) {
            group.removeMember(originalId);
            group.addMember(newId);

            groupRepository.save(group);
        }
    }

    public List<User> searchByNameName(String lastName) {
        return userRepository.findByFullNameContains(lastName);
    }

}
