package com.course_learning.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.course_learning.backend.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
