package portfolio.pucrs.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portfolio.pucrs.auth.entity.EmailVerification;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findFirstByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(Long userId);

    List<EmailVerification> findAllByUserIdAndConsumedAtIsNull(Long userId);
}
