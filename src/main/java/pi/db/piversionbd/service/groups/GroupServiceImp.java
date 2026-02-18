package pi.db.piversionbd.service.groups;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.exception.ResourceNotFoundException;
import pi.db.piversionbd.repository.groups.GroupRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupServiceImp implements IGroupService {

    private final GroupRepository groupRepository;

    private static final String JOIN_PUBLIC = "public";
    private static final String JOIN_PRIVATE = "private";

    @Override
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    @Override
    public List<Group> getAllGroups(String typeFilter) {
        if (typeFilter == null || typeFilter.isBlank()) {
            return groupRepository.findAll();
        }
        return groupRepository.findByType(typeFilter.trim());
    }

    @Override
    public Group getGroupById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id " + id));
    }

    @Override
    public Group createGroup(Group group) {
        group.setId(null);
        if (group.getJoinPolicy() == null || group.getJoinPolicy().isBlank()) {
            group.setJoinPolicy(JOIN_PUBLIC);
        }
        String jp = group.getJoinPolicy().trim().toLowerCase();
        group.setJoinPolicy(jp);
        if (JOIN_PRIVATE.equals(jp)) {
            if (group.getInviteCode() == null || group.getInviteCode().isBlank()) {
                group.setInviteCode(generateUniqueInviteCode());
            }
        } else {
            group.setInviteCode(null);
        }
        // currentMemberCount is managed automatically by membership operations
        group.setCurrentMemberCount(0);
        return groupRepository.save(group);
    }

    @Override
    public Group updateGroup(Long id, Group updated) {
        Group existing = getGroupById(id);
        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getType() != null) existing.setType(updated.getType());
        if (updated.getRegion() != null) existing.setRegion(updated.getRegion());
        if (updated.getJoinPolicy() != null) {
            String jp = updated.getJoinPolicy().trim().toLowerCase();
            existing.setJoinPolicy(jp);
            if (JOIN_PRIVATE.equals(jp)) {
                if (existing.getInviteCode() == null || existing.getInviteCode().isBlank()) {
                    existing.setInviteCode(generateUniqueInviteCode());
                }
            } else {
                existing.setInviteCode(null);
            }
        }
        if (updated.getMinMembers() != null) existing.setMinMembers(updated.getMinMembers());
        if (updated.getMaxMembers() != null) existing.setMaxMembers(updated.getMaxMembers());
        // currentMemberCount is NOT updatable via API; it is incremented/decremented automatically
        return groupRepository.save(existing);
    }

    @Override
    public void deleteGroup(Long id) {
        groupRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Group> getGroupSuggestions(Integer age, String profession, String region, String packageType) {
        Stream<Group> stream = groupRepository.findAll().stream();
        // Suggestions are for PUBLIC groups only
        stream = stream.filter(g -> g.getJoinPolicy() == null || JOIN_PUBLIC.equalsIgnoreCase(g.getJoinPolicy()));
        if (region != null && !region.isBlank()) {
            String r = region.trim();
            stream = stream.filter(g -> r.equalsIgnoreCase(g.getRegion()));
        }
        if (profession != null && !profession.isBlank()) {
            String compatibleType = mapProfessionToGroupType(profession.trim());
            if (compatibleType != null) {
                stream = stream.filter(g -> compatibleType.equalsIgnoreCase(g.getType()));
            }
        }
        stream = stream.filter(g -> {
            Integer max = g.getMaxMembers();
            if (max == null) return true;
            int current = g.getCurrentMemberCount() != null ? g.getCurrentMemberCount() : 0;
            return current < max;
        });
        return stream.toList();
    }

    private String generateUniqueInviteCode() {
        // UUID is plenty for uniqueness; we still check collisions just in case.
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "");
        } while (groupRepository.existsByInviteCode(code));
        return code;
    }

    /** Simple rule: profession → group type. Students→STUDENTS, workers→WORKERS, family→FAMILY, else no filter. */
    private static String mapProfessionToGroupType(String profession) {
        if (profession == null) return null;
        String p = profession.toLowerCase();
        if (p.contains("student") || p.contains("étudiant")) return "STUDENTS";
        if (p.contains("worker") || p.contains("travail") || p.contains("employ")) return "WORKERS";
        if (p.contains("family") || p.contains("famille")) return "FAMILY";
        return "MIXED";
    }
}

