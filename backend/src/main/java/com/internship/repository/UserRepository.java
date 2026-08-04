package com.internship.repository;

import com.internship.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findFirstByRoleAndIdCard(String role, String idCard);
    List<User> findByRoleAndIdCard(String role, String idCard);
    Optional<User> findFirstByRoleAndOrganizationCode(String role, String organizationCode);
    List<User> findByRoleAndOrganizationCode(String role, String organizationCode);
    Optional<User> findFirstByOrganizationCodeAndRoleIn(String organizationCode, Collection<String> roles);
    List<User> findByRoleAndRealName(String role, String realName);
    List<User> findByOrganizationNameAndRoleIn(String organizationName, Collection<String> roles);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsernameAndIdNot(String username, Long id);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByPhone(String phone);
    boolean existsByPhoneAndIdNot(String phone, Long id);
    List<User> findByStatusAndApproved(String status, Boolean approved);
    List<User> findByRole(String role);
    List<User> findBySchoolId(Long schoolId);
    List<User> findByRoleAndApproved(String role, Boolean approved);
    List<User> findBySchoolIdAndRole(Long schoolId, String role);
}
