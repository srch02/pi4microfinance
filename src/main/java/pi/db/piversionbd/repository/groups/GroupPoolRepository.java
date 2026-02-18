package pi.db.piversionbd.repository.groups;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pi.db.piversionbd.entities.groups.GroupPool;

import java.util.Optional;

@Repository
public interface GroupPoolRepository extends JpaRepository<GroupPool, Long> {

    Optional<GroupPool> findByGroup_Id(Long groupId);
}
