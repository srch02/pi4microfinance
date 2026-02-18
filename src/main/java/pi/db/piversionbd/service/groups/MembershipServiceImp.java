package pi.db.piversionbd.service.groups;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pi.db.piversionbd.entities.groups.Group;
import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.entities.groups.Membership;
import pi.db.piversionbd.exception.ResourceNotFoundException;
import pi.db.piversionbd.entities.groups.GroupChangeRequest;
import pi.db.piversionbd.repository.groups.GroupRepository;
import pi.db.piversionbd.repository.groups.MemberRepository;
import pi.db.piversionbd.repository.groups.MembershipRepository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MembershipServiceImp implements IMembershipService {

    private static final String DEFAULT_PACKAGE = "BASIC";
    private static final String JOIN_PRIVATE = "private";

    private final MembershipRepository membershipRepository;
    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;
    private final IGroupChangeRequestService groupChangeRequestService;

    @Override
    public AddMembershipResult addMemberToGroup(Long groupId, Long memberId, String packageType) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id " + groupId));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id " + memberId));

        // Private groups must be joined via invite code (QR)
        if (group.getJoinPolicy() != null && JOIN_PRIVATE.equalsIgnoreCase(group.getJoinPolicy())) {
            throw new IllegalArgumentException("Group is private; join requires invite code");
        }

        return addMemberToGroupOrRequest(group, member, packageType);
    }

    @Override
    public AddMembershipResult addMemberToGroupByInviteCode(String inviteCode, Long memberId, String packageType) {
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new IllegalArgumentException("inviteCode is required");
        }
        Group group = groupRepository.findByInviteCode(inviteCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Invite code not found: " + inviteCode));
        if (group.getJoinPolicy() == null || !JOIN_PRIVATE.equalsIgnoreCase(group.getJoinPolicy())) {
            throw new IllegalArgumentException("Invite code can be used only for private groups");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id " + memberId));
        return addMemberToGroupOrRequest(group, member, packageType);
    }

    /**
     * If member is already in another group: create a PENDING group-change request (admin must approve),
     * or consume an APPROVED request by ending old membership and creating new one.
     */
    private AddMembershipResult addMemberToGroupOrRequest(Group toGroup, Member member, String packageType) {
        Long memberId = member.getId();
        Long toGroupId = toGroup.getId();

        List<Membership> currentMemberships = membershipRepository.findByMember_IdAndEndedAtIsNull(memberId);
        // Same group: already handled in createMembership (throws "already in group").
        Membership inTarget = currentMemberships.stream()
                .filter(m -> toGroupId.equals(m.getGroup().getId()))
                .findFirst()
                .orElse(null);
        if (inTarget != null) {
            throw new IllegalArgumentException("Member " + memberId + " is already in group " + toGroupId);
        }

        // Member in another group? Need approved request or create new request.
        List<Membership> inOtherGroups = currentMemberships.stream()
                .filter(m -> !toGroupId.equals(m.getGroup().getId()))
                .toList();

        if (!inOtherGroups.isEmpty()) {
            // Use first "from" group (member can only be in one current group in practice).
            Long fromGroupId = inOtherGroups.get(0).getGroup().getId();
            var approvedOpt = groupChangeRequestService.findApprovedRequest(memberId, fromGroupId, toGroupId);
            if (approvedOpt.isPresent()) {
                GroupChangeRequest approved = approvedOpt.get();
                // End membership in fromGroup (the one in the approved request), then create in toGroup.
                removeMemberFromGroup(fromGroupId, memberId);
                groupChangeRequestService.markCompleted(approved);
                Membership created = createMembership(toGroup, member, packageType != null ? packageType : approved.getRequestedPackageType());
                return IMembershipService.AddMembershipResult.membership(created);
            }
            // No approved request: create PENDING request (or throw if already pending).
            GroupChangeRequest request = groupChangeRequestService.createRequest(memberId, fromGroupId, toGroupId, packageType);
            return IMembershipService.AddMembershipResult.groupChangeRequest(request);
        }

        Membership created = createMembership(toGroup, member, packageType);
        return IMembershipService.AddMembershipResult.membership(created);
    }

    private Membership createMembership(Group group, Member member, String packageType) {
        Long memberId = member.getId();
        Long groupId = group.getId();
        if (membershipRepository.findByMember_IdAndGroup_IdAndEndedAtIsNull(memberId, groupId).isPresent()) {
            throw new IllegalArgumentException("Member " + memberId + " is already in group " + groupId);
        }
        // Capacity check
        Integer max = group.getMaxMembers();
        int current = group.getCurrentMemberCount() != null ? group.getCurrentMemberCount() : 0;
        if (max != null && current >= max) {
            throw new IllegalArgumentException("Group is full (maxMembers=" + max + ")");
        }

        String pkg = packageType != null && !packageType.isBlank() ? packageType.trim().toUpperCase() : DEFAULT_PACKAGE;
        Membership m = new Membership();
        m.setMember(member);
        m.setGroup(group);
        m.setPackageType(pkg);
        m.setMonthlyAmount(getMonthlyAmountForPackage(member, pkg));
        // Derive consultations_limit and annual_limit dynamically from package + personalized monthly amount
        m.applyPersonalizedCoverage();
        m.setStatus(Membership.STATUS_PENDING);
        m.setEndedAt(null);
        Membership saved = membershipRepository.save(m);

        // Do NOT set member.currentGroup here — member stays current_group_id=NULL until first payment (status → active)

        group.setCurrentMemberCount(current + 1);
        groupRepository.save(group);

        return saved;
    }

    @Override
    public void removeMemberFromGroup(Long groupId, Long memberId) {
        Membership membership = membershipRepository
                .findByMember_IdAndGroup_IdAndEndedAtIsNull(memberId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active membership found for member " + memberId + " in group " + groupId));

        membership.setStatus(Membership.STATUS_CANCELLED);
        membership.setEndedAt(Instant.now());
        membershipRepository.save(membership);

        Member member = membership.getMember();
        if (member.getCurrentGroup() != null && member.getCurrentGroup().getId().equals(groupId)) {
            member.setCurrentGroup(null);
            memberRepository.save(member);
        }

        Group group = membership.getGroup();
        int count = group.getCurrentMemberCount() != null ? group.getCurrentMemberCount() : 0;
        if (count > 0) {
            group.setCurrentMemberCount(count - 1);
            groupRepository.save(group);
        }
    }

    @Override
    public List<Group> getGroupsForMember(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Member not found with id " + memberId);
        }
        return membershipRepository.findByMember_IdAndStatusAndEndedAtIsNull(memberId, Membership.STATUS_ACTIVE).stream()
                .map(Membership::getGroup)
                .collect(Collectors.toList());
    }

    /** Monthly amount from member's price for the given package (price_basic/price_confort/price_premium), fallback to personalized_monthly_price. */
    private static float getMonthlyAmountForPackage(Member member, String pkg) {
        Float price = null;
        if ("BASIC".equalsIgnoreCase(pkg) && member.getPriceBasic() != null) price = member.getPriceBasic();
        else if ("CONFORT".equalsIgnoreCase(pkg) && member.getPriceConfort() != null) price = member.getPriceConfort();
        else if ("PREMIUM".equalsIgnoreCase(pkg) && member.getPricePremium() != null) price = member.getPricePremium();
        if (price == null) price = member.getPersonalizedMonthlyPrice();
        return price != null ? price : 0f;
    }

    @Override
    public List<Membership> getMembershipsForMember(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("Member not found with id " + memberId);
        }
        return membershipRepository.findByMember_IdOrderByIdDesc(memberId).stream()
                .map(this::ensureCoverageComputed)
                .collect(Collectors.toList());
    }

    @Override
    public Membership updateMembershipStatus(Long membershipId, String status) {
        Membership m = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found with id " + membershipId));
        if (status == null || !List.of(Membership.STATUS_PENDING, Membership.STATUS_ACTIVE, Membership.STATUS_SUSPENDED, Membership.STATUS_CANCELLED).contains(status)) {
            throw new IllegalArgumentException("Invalid membership status: " + status);
        }
        m.setStatus(status);
        if (Membership.STATUS_ACTIVE.equals(status)) {
            if (m.getActivatedAt() == null) {
                m.setActivatedAt(Instant.now());
            }
            Member member = m.getMember();
            if (member != null && (member.getCurrentGroup() == null || !member.getCurrentGroup().getId().equals(m.getGroup().getId()))) {
                member.setCurrentGroup(m.getGroup());
                memberRepository.save(member);
            }
        }
        return membershipRepository.save(m);
    }

    @Override
    public Membership getMembershipById(Long membershipId) {
        Membership m = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found with id " + membershipId));
        return ensureCoverageComputed(m);
    }

    /**
     * Backfill coverage for legacy memberships where consultations_limit or annual_limit is null.
     * Uses the current packageType and monthlyAmount to compute limits, then persists once.
     */
    private Membership ensureCoverageComputed(Membership m) {
        if (m == null) return null;
        if ((m.getAnnualLimit() == null || m.getConsultationsLimit() == null)
                && m.getMonthlyAmount() != null
                && m.getPackageType() != null) {
            m.applyPersonalizedCoverage();
            return membershipRepository.save(m);
        }
        return m;
    }
}