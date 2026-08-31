package portfolio.pucrs.auth.exception;

public class InvalidCourseException extends RuntimeException {

    public InvalidCourseException(Long courseId) {
        super("Course not found or inactive: " + courseId);
    }
}
