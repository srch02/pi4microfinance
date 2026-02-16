package pi.db.piversionbd.repositories.score;

import org.springframework.data.jpa.repository.JpaRepository;
import pi.db.piversionbd.entities.groups.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
}

