package pi.db.piversionbd.controller.health;

import pi.db.piversionbd.entities.health.Doctor;
import pi.db.piversionbd.entities.health.Specialite;
import pi.db.piversionbd.service.health.IDoctorService;
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
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*")
@Tag(name = "Doctors", description = "API for managing doctors")
public class DoctorController {

    @Autowired
    private IDoctorService doctorService;

    @PostMapping
    @Operation(summary = "Create a new doctor", description = "Creates a new doctor with the provided information")
    @ApiResponse(responseCode = "201", description = "Doctor created successfully")
    public ResponseEntity<Doctor> createDoctor(@RequestBody Doctor doctor) {
        Doctor savedDoctor = doctorService.saveDoctors(doctor);
        return new ResponseEntity<>(savedDoctor, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get doctor by ID", description = "Retrieves a doctor by their ID")
    @ApiResponse(responseCode = "200", description = "Doctor found")
    @ApiResponse(responseCode = "404", description = "Doctor not found")
    public ResponseEntity<Doctor> getDoctorById(@PathVariable Long id) {
        Optional<Doctor> doctor = doctorService.getDoctorById(id);
        if (doctor.isPresent()) {
            return new ResponseEntity<>(doctor.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping
    @Operation(summary = "Get all doctors", description = "Retrieves a list of all doctors")
    @ApiResponse(responseCode = "200", description = "Doctors retrieved successfully")
    public ResponseEntity<List<Doctor>> getAllDoctors() {
        List<Doctor> doctors = doctorService.getAllDoctors();
        return new ResponseEntity<>(doctors, HttpStatus.OK);
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get doctor by email", description = "Retrieves a doctor by their email address")
    @ApiResponse(responseCode = "200", description = "Doctor found")
    @ApiResponse(responseCode = "404", description = "Doctor not found")
    public ResponseEntity<Doctor> getDoctorByEmail(@PathVariable String email) {
        Optional<Doctor> doctor = doctorService.getDoctorByEmail(email);
        if (doctor.isPresent()) {
            return new ResponseEntity<>(doctor.get(), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/specialite/{specialite}")
    @Operation(summary = "Get doctors by specialty", description = "Retrieves all doctors with a specific specialty")
    @ApiResponse(responseCode = "200", description = "Doctors retrieved successfully")
    @ApiResponse(responseCode = "400", description = "Invalid specialty")
    public ResponseEntity<List<Doctor>> getDoctorsBySpecialite(@PathVariable String specialite) {
        try {
            Specialite spec = Specialite.valueOf(specialite);
            List<Doctor> doctors = doctorService.getDoctorsBySpecialite(spec);
            return new ResponseEntity<>(doctors, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update doctor", description = "Updates an existing doctor's information")
    @ApiResponse(responseCode = "200", description = "Doctor updated successfully")
    @ApiResponse(responseCode = "404", description = "Doctor not found")
    public ResponseEntity<Doctor> updateDoctor(@PathVariable Long id, @RequestBody Doctor doctor) {
        Doctor updatedDoctor = doctorService.updateDoctor(id, doctor);
        if (updatedDoctor != null) {
            return new ResponseEntity<>(updatedDoctor, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete doctor", description = "Deletes a doctor by their ID")
    @ApiResponse(responseCode = "204", description = "Doctor deleted successfully")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

