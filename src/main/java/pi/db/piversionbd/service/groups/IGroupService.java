package pi.db.piversionbd.service.groups;

import pi.db.piversionbd.entities.groups.Group;

import java.util.List;

public interface IGroupService {

    List<Group> getAllGroups();

    /** When typeFilter is null, returns all groups; otherwise filters by type. */
    List<Group> getAllGroups(String typeFilter);

    Group getGroupById(Long id);

    Group createGroup(Group group);

    Group updateGroup(Long id, Group updated);

    void deleteGroup(Long id);

    /**
     * Groups that match the member's criteria for joining (same region, compatible type, not full). Rule-based for now; can be replaced with ML later.
     */
    List<Group> getGroupSuggestions(Integer age, String profession, String region, String packageType);
}

