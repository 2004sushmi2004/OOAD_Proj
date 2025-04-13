package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAllById(Iterable<Long> ids);

}
