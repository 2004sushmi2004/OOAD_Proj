package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByFacultyId(Long facultyId);
}
