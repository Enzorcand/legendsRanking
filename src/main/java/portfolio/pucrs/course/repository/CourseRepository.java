package portfolio.pucrs.course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portfolio.pucrs.course.entity.Course;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByActiveTrueOrderByNameAsc();
}
