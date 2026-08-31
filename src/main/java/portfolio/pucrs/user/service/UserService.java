package portfolio.pucrs.user.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import portfolio.pucrs.user.dto.UserSummaryResponse;
import portfolio.pucrs.user.repository.UserRepository;

import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }

    public UserSummaryResponse getUserSummaryByEmail(String email) {
        var user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return new UserSummaryResponse(user.getId(), user.getFullName());
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
