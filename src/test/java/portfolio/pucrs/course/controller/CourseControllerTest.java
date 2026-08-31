package portfolio.pucrs.course.controller;

import org.junit.jupiter.api.Test;
import portfolio.pucrs.course.dto.CourseResponse;
import portfolio.pucrs.course.service.CourseService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseControllerTest {

    private final CourseService courseService = mock(CourseService.class);
    private final CourseController courseController = new CourseController(courseService);

    @Test
    void returnsThePublicCoursesProvidedByTheService() {
        List<CourseResponse> expected = List.of(new CourseResponse(1L, "Ciência da Computação"));
        when(courseService.getActiveCourses()).thenReturn(expected);

        List<CourseResponse> response = courseController.getCourses();

        assertEquals(expected, response);
    }
}
