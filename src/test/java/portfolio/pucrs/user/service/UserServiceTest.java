package portfolio.pucrs.user.service;

import org.junit.jupiter.api.Test;
import portfolio.pucrs.auth.exception.InvalidCourseException;
import portfolio.pucrs.auth.exception.InvalidRegistrationException;
import portfolio.pucrs.course.entity.Course;
import portfolio.pucrs.course.repository.CourseRepository;
import portfolio.pucrs.riot.entity.Region;
import portfolio.pucrs.riot.entity.RiotAccount;
import portfolio.pucrs.user.dto.MeResponse;
import portfolio.pucrs.user.dto.UpdateMeRequest;
import portfolio.pucrs.user.dto.UserSummaryResponse;
import portfolio.pucrs.user.entity.User;
import portfolio.pucrs.user.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CourseRepository courseRepository = mock(CourseRepository.class);
    private final UserService userService = new UserService(userRepository, courseRepository);

    private Course course(long id, String name) {
        Course course = new Course();
        course.setId(id);
        course.setName(name);
        course.setActive(true);
        return course;
    }

    private User userWithCourse() {
        User user = new User();
        user.setId(1L);
        user.setFullName("Ana Raposa");
        user.setEmail("ana@pucrs.br");
        user.setCourse(course(1L, "Ciência da Computação"));
        user.setEmailVerified(true);
        user.setRankingEligible(true);
        return user;
    }

    @Test
    void normalizesEmailBeforeCheckingExistence() {
        when(userRepository.existsByEmailIgnoreCase("aluno@pucrs.br")).thenReturn(true);

        boolean exists = userService.existsByEmail("  ALUNO@PUCRS.BR  ");

        assertTrue(exists);
        verify(userRepository).existsByEmailIgnoreCase("aluno@pucrs.br");
    }

    @Test
    void returnsAUserSummaryWithoutPrivateFields() {
        User user = new User();
        user.setId(10L);
        user.setFullName("Ana Raposa");
        when(userRepository.findByEmailIgnoreCase("ana@pucrs.br")).thenReturn(Optional.of(user));

        UserSummaryResponse response = userService.getUserSummaryByEmail(" ANA@PUCRS.BR ");

        assertEquals(new UserSummaryResponse(10L, "Ana Raposa"), response);
        verify(userRepository).findByEmailIgnoreCase("ana@pucrs.br");
    }

    @Test
    void rejectsBlankEmail() {
        assertThrows(IllegalArgumentException.class, () -> userService.existsByEmail("  "));
    }

    @Test
    void buildsTheFullProfileWithoutARiotAccount() {
        User user = userWithCourse();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        MeResponse response = userService.getMe(1L);

        assertEquals(1L, response.id());
        assertEquals("Ana Raposa", response.fullName());
        assertTrue(response.emailVerified());
        assertTrue(response.rankingEligible());
        assertNull(response.riotAccount());
    }

    @Test
    void buildsTheFullProfileWithALinkedRiotAccount() {
        User user = userWithCourse();
        RiotAccount riotAccount = new RiotAccount();
        riotAccount.setGameName("Raposa");
        riotAccount.setTagLine("BR1");
        riotAccount.setRegion(Region.BR1);
        user.setRiotAccount(riotAccount);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        MeResponse response = userService.getMe(1L);

        assertEquals("Raposa", response.riotAccount().gameName());
        assertEquals("BR1", response.riotAccount().tagLine());
        assertEquals(Region.BR1, response.riotAccount().region());
    }

    @Test
    void updatesOnlyTheFullNameWhenOnlyTheFullNameIsSent() {
        User user = userWithCourse();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        MeResponse response = userService.updateMe(1L, new UpdateMeRequest("Novo Nome", null));

        assertEquals("Novo Nome", response.fullName());
        assertEquals(1L, response.course().id());
    }

    @Test
    void updatesOnlyTheCourseWhenOnlyTheCourseIdIsSent() {
        User user = userWithCourse();
        Course newCourse = course(2L, "Engenharia de Software");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(newCourse));
        when(userRepository.save(user)).thenReturn(user);

        MeResponse response = userService.updateMe(1L, new UpdateMeRequest(null, 2L));

        assertEquals("Ana Raposa", response.fullName());
        assertEquals(2L, response.course().id());
    }

    @Test
    void rejectsAnUnknownOrInactiveCourse() {
        User user = userWithCourse();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(InvalidCourseException.class, () -> userService.updateMe(1L, new UpdateMeRequest(null, 99L)));
    }

    @Test
    void rejectsAnUpdateWithNoFieldsProvided() {
        assertThrows(InvalidRegistrationException.class,
                () -> userService.updateMe(1L, new UpdateMeRequest(null, null)));
    }

    @Test
    void togglesRankingParticipationOnEachCall() {
        User user = userWithCourse();
        user.setRankingEligible(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        MeResponse firstToggle = userService.toggleRankingParticipation(1L);
        assertFalse(firstToggle.rankingEligible());
        assertFalse(user.isRankingEligible());

        MeResponse secondToggle = userService.toggleRankingParticipation(1L);
        assertTrue(secondToggle.rankingEligible());
        assertTrue(user.isRankingEligible());

        verify(userRepository, times(2)).save(user);
    }
}
