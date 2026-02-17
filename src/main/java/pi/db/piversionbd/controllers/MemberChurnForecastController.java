package pi.db.piversionbd.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pi.db.piversionbd.entities.admin.MemberChurnForecast;
import pi.db.piversionbd.repositories.MemberChurnForecastRepository;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/member-churn-forecasts")
public class MemberChurnForecastController {

    @Autowired
    private MemberChurnForecastRepository memberChurnForecastRepository;

    // ✅ Get all forecasts
    @GetMapping
    public List<MemberChurnForecast> getAllForecasts() {
        return memberChurnForecastRepository.findAll();
    }

    // ✅ Get forecast by ID
    @GetMapping("/{id}")
    public ResponseEntity<MemberChurnForecast> getForecastById(@PathVariable Long id) {

        Optional<MemberChurnForecast> forecast =
                memberChurnForecastRepository.findById(id);

        return forecast.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✅ Create new forecast
    @PostMapping
    public ResponseEntity<MemberChurnForecast> createForecast(
            @RequestBody MemberChurnForecast forecast) {

        MemberChurnForecast saved =
                memberChurnForecastRepository.save(forecast);

        return ResponseEntity.ok(saved);
    }

    // ✅ Update forecast
    @PutMapping("/{id}")
    public ResponseEntity<MemberChurnForecast> updateForecast(
            @PathVariable Long id,
            @RequestBody MemberChurnForecast forecastDetails) {

        Optional<MemberChurnForecast> optionalForecast =
                memberChurnForecastRepository.findById(id);

        if (optionalForecast.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MemberChurnForecast forecast = optionalForecast.get();

        forecast.setMember(forecastDetails.getMember());
        forecast.setChurnProbability(forecastDetails.getChurnProbability());
        forecast.setRiskLevel(forecastDetails.getRiskLevel());
        forecast.setRiskFactors(forecastDetails.getRiskFactors());
        forecast.setRecommendation(forecastDetails.getRecommendation());

        MemberChurnForecast updated =
                memberChurnForecastRepository.save(forecast);

        return ResponseEntity.ok(updated);
    }

    // ✅ Delete forecast
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteForecast(@PathVariable Long id) {

        Optional<MemberChurnForecast> forecast =
                memberChurnForecastRepository.findById(id);

        if (forecast.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        memberChurnForecastRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }

}
