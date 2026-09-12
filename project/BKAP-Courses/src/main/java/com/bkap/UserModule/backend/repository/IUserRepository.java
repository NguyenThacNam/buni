package com.bkap.UserModule.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bkap.UserModule.entity.User;
import com.bkap.UserModule.entity.UserRole;

public interface IUserRepository extends JpaRepository<User, Short> {

	boolean existsByPhone(String phone);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	Optional<User> findByUsername(String username);

	Optional<User> findById(Integer instructorId);

	long countByRole(UserRole role);

	/** Tìm theo họ tên, tên đăng nhập, email hoặc số điện thoại. q = "" thì khớp tất cả. */
	@Query("""
			SELECT u FROM User u
			WHERE LOWER(u.fullname) LIKE LOWER(CONCAT('%', :q, '%'))
			   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))
			   OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%'))
			   OR u.phone           LIKE CONCAT('%', :q, '%')
			""")
	Page<User> timKiem(@Param("q") String q, Pageable pageable);

}
