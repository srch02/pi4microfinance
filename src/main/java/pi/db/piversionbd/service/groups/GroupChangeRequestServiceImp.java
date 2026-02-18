package pi.db.piversionbd.service.groups;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.admin.SystemAlert;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.GroupChangeRequest;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.exception.ResourceNotFoundException;
import pi.db.piversionbd.repository.admin.SystemAlertRepository;
import pi.db.piversionbd.repository.groups.GroupChangeRequestRepository;
import pi.db.piversionbd.repository.groups.GroupRepository;
import pi.db.piversionbd.repository.groups.MemberRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupChangeRequestServiceImp implements IGroupChangeRequestService {

    private final GroupChangeRequestRepository groupChangeRequestRepository;
    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;
    private final SystemAlertRepository systemAlertRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GroupChangeRequest> findByStatus(String status) {
        return groupChangeRequestRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupChangeRequest getById(Long id) {
        return groupChangeRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Group change request not found with id " + id));
    }

    @Override
    public GroupChangeRequest approve(Long requestId) {
        GroupChangeRequest r = getById(requestId);
        if (!GroupChangeRequest.STATUS_PENDING.equals(r.getStatus())) {
            throw new IllegalArgumentException("Request is not pending (status: " + r.getStatus() + ")");
        }
        r.setStatus(GroupChangeRequest.STATUS_APPROVED);
        r.setReviewedAt(Instant.now());
        return groupChangeRequestRepository.save(r);
    }

    @Override
    public GroupChangeRequest reject(Long requestId) {
        GroupChangeRequest r = getById(requestId);
        if (!GroupChangeRequest.STATUS_PENDING.equals(r.getStatus())) {
            throw new IllegalArgumentException("Request is not pending (status: " + r.getStatus() + ")");
        }
        r.setStatus(GroupChangeRequest.STATUS_REJECTED);
        r.setReviewedAt(Instant.now());
        return groupChangeRequestRepository.save(r);
    }

    @Override
    public GroupChangeRequest createRequest(Long memberId, Long fromGroupId, Long toGroupId, String requestedPackageType) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id " + memberId));
        Group fromGroup = groupRepository.findById(fromGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id " + fromGroupId));
        Group toGroup = groupRepository.findById(toGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id " + toGroupId));

        if (groupChangeRequestRepository.existsByMember_IdAndToGroup_IdAndStatus(memberId, toGroupId, GroupChangeRequest.STATUS_PENDING)) {
            throw new IllegalArgumentException("A pending group change request to this group already exists for this member");
        }

        GroupChangeRequest r = new GroupChangeRequest();
        r.setMember(member);
        r.setFromGroup(fromGroup);
        r.setToGroup(toGroup);
        r.setStatus(GroupChangeRequest.STATUS_PENDING);
        r.setRequestedPackageType(requestedPackageType != null ? requestedPackageType.trim().toUpperCase() : null);
        r = groupChangeRequestRepository.save(r);

        // Notify admin: create system alert for pending group-change request
        SystemAlert alert = new SystemAlert();
        alert.setAlertType("GROUP_CHANGE_REQUEST");
        alert.setSeverity("medium");
        alert.setRegion(toGroup.getRegion());
        alert.setTitle("Group change request pending");
        alert.setMessage(String.format("Member %d (%s) requested to move from group \"%s\" (id=%d) to \"%s\" (id=%d). Requires admin approval.",
                member.getId(),
                member.getCinNumber() != null ? member.getCinNumber() : "—",
                fromGroup.getName() != null ? fromGroup.getName() : "—",
                fromGroup.getId(),
                toGroup.getName() != null ? toGroup.getName() : "—",
                toGroup.getId()));
        alert.setActive(true);
        alert.setSourceEntityType("GROUP_CHANGE_REQUEST");
        alert.setSourceEntityId(r.getId());
        systemAlertRepository.save(alert);

        return r;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GroupChangeRequest> findApprovedRequest(Long memberId, Long fromGroupId, Long toGroupId) {
        return groupChangeRequestRepository.findByMember_IdAndFromGroup_IdAndToGroup_IdAndStatus(
                memberId, fromGroupId, toGroupId, GroupChangeRequest.STATUS_APPROVED);
    }

    @Override
    public void markCompleted(GroupChangeRequest request) {
        request.setStatus(GroupChangeRequest.STATUS_COMPLETED);
        groupChangeRequestRepository.save(request);
    }
}
