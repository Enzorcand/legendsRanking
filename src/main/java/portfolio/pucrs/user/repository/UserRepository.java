package portfolio.pucrs.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import portfolio.pucrs.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);
}
