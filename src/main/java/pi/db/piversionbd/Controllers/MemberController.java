package pi.db.piversionbd.Controllers;

import pi.db.piversionbd.entities.groups.Member;
import pi.db.piversionbd.services.IMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/members")
@CrossOrigin(origins = "*")
@Tag(name = "Members", description = "API for managing members")
public class MemberController {

    @Autowired
    private IMemberService memberService;

    @PostMapping
    @Operation(summary = "Create a new member", description = "Creates a new member with the provided information")
    @ApiResponse(responseCode = "201", description = "Member created successfully")
    public ResponseEntity<Member> createMember(@RequestBody Member member) {
        Member savedMember = memberService.saveMember(member);
        return new ResponseEntity<>(savedMember, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get member by ID", description = "Retrieves a member by their ID")
    @ApiResponse(responseCode = "200", description = "Member found")
    @ApiResponse(responseCode = "404", description = "Member not found")
    public ResponseEntity<Member> getMemberById(@PathVariable Long id) {
        Optional<Member> member = memberService.getMemberById(id);
        if (member.isPresent()) {
            return new ResponseEntity<>(member.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping
    @Operation(summary = "Get all members", description = "Retrieves a list of all members")
    @ApiResponse(responseCode = "200", description = "Members retrieved successfully")
    public ResponseEntity<List<Member>> getAllMembers() {
        List<Member> members = memberService.getAllMembers();
        return new ResponseEntity<>(members, HttpStatus.OK);
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get member by email", description = "Retrieves a member by their email address")
    @ApiResponse(responseCode = "200", description = "Member found")
    @ApiResponse(responseCode = "404", description = "Member not found")
    public ResponseEntity<Member> getMemberByEmail(@PathVariable String email) {
        Optional<Member> member = memberService.getMemberByEmail(email);
        if (member.isPresent()) {
            return new ResponseEntity<>(member.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/cin/{cinNumber}")
    @Operation(summary = "Get member by CIN number", description = "Retrieves a member by their CIN (National ID) number")
    @ApiResponse(responseCode = "200", description = "Member found")
    @ApiResponse(responseCode = "404", description = "Member not found")
    public ResponseEntity<Member> getMemberByCinNumber(@PathVariable String cinNumber) {
        Optional<Member> member = memberService.getMemberByCinNumber(cinNumber);
        if (member.isPresent()) {
            return new ResponseEntity<>(member.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update member", description = "Updates an existing member's information")
    @ApiResponse(responseCode = "200", description = "Member updated successfully")
    @ApiResponse(responseCode = "404", description = "Member not found")
    public ResponseEntity<Member> updateMember(@PathVariable Long id, @RequestBody Member member) {
        Member updatedMember = memberService.updateMember(id, member);
        if (updatedMember != null) {
            return new ResponseEntity<>(updatedMember, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete member", description = "Deletes a member by their ID")
    @ApiResponse(responseCode = "204", description = "Member deleted successfully")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

