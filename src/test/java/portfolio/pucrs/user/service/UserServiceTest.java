package portfolio.pucrs.user.service;

import org.junit.jupiter.api.Test;
import portfolio.pucrs.user.dto.UserSummaryResponse;
import portfolio.pucrs.user.entity.User;
import portfolio.pucrs.user.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = new UserService(userRepository);

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
}
