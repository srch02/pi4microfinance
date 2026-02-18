package pi.db.piversionbd.repository.groups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.groups.Member;

import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByCurrentGroup_Id(Long groupId);

    boolean existsByCinNumber(String cinNumber);

    boolean existsByCinNumberAndIdNot(String cinNumber, Long id);
}
