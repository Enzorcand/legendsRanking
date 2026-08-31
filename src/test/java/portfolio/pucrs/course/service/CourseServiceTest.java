package portfolio.pucrs.course.service;

import org.junit.jupiter.api.Test;
import portfolio.pucrs.course.dto.CourseResponse;
import portfolio.pucrs.course.entity.Course;
import portfolio.pucrs.course.repository.CourseRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseServiceTest {

    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final CourseService courseService = new CourseService(courseRepository);

    @Test
    void returnsTheActiveCoursesAsPublicResponses() {
        Course course = new Course();
        course.setId(1L);
        course.setName("Ciência da Computação");
        course.setActive(true);
        when(courseRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(course));

        List<CourseResponse> response = courseService.getActiveCourses();

        assertEquals(List.of(new CourseResponse(1L, "Ciência da Computação")), response);
    }

    @Test
    void returnsAnEmptyListWhenThereAreNoActiveCourses() {
        when(courseRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of());

        List<CourseResponse> response = courseService.getActiveCourses();

        assertTrue(response.isEmpty());
    }
}
