# Groups module – organization and API summary

## Structure

- **Controllers:** `GroupController`, `MemberController`, `MembershipController`, `GroupChangeRequestController`, `PaymentController`
- **Services:** `IGroupService` / `GroupServiceImp`, `IMembershipService` / `MembershipServiceImp`, `IGroupChangeRequestService` / `GroupChangeRequestServiceImp`, `IPaymentService` / `PaymentServiceImp`, `IMemberService` / `MemberServiceImp`
- **DTOs:** `GroupsModuleDto` (nested: `GroupDto`, `MembershipDto`, `GroupPoolDto`, `GroupChangeRequestDto`, etc.)
- **Result type:** `IMembershipService.AddMembershipResult` – nested in the membership interface; used when adding a member to a group or joining by invite (membership created or group-change request created).

## API redundancy check

- **No redundant APIs.** The two “add member” entry points serve different flows:
  - **POST /api/groups/{groupId}/members** – add by group ID (public groups only; private groups reject with 400).
  - **POST /api/memberships/join-by-invite** – join by invite code (private groups only; same backend logic, different trigger).
- Other endpoints are distinct (CRUD groups, pool, members, memberships, group-change requests, payments).

## Organization change

- **AddMembershipResult** is now **nested inside `IMembershipService`** (`IMembershipService.AddMembershipResult`), so the add-membership result type lives with the contract that returns it. The standalone `AddMembershipResult.java` file was removed.
