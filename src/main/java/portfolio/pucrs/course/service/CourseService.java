package portfolio.pucrs.course.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portfolio.pucrs.course.dto.CourseResponse;
import portfolio.pucrs.course.repository.CourseRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<CourseResponse> getActiveCourses() {
        return courseRepository.findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(course -> new CourseResponse(course.getId(), course.getName()))
                .toList();
    }
}
