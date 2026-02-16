package pi.db.piversionbd.repositories.score;


import org.springframework.data.jpa.repository.JpaRepository;
import pi.db.piversionbd.entities.groups.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {
}