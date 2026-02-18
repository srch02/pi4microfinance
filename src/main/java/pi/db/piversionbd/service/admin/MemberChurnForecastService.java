package pi.db.piversionbd.service.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pi.db.piversionbd.entities.admin.MemberChurnForecast;
import pi.db.piversionbd.repository.admin.MemberChurnForecastRepository;

import java.util.List;
import java.util.Optional;

@Service
public class MemberChurnForecastService {

    @Autowired
    private MemberChurnForecastRepository memberChurnForecastRepository;

    // ✅ CREATE
    public MemberChurnForecast createForecast(MemberChurnForecast forecast) {
        return memberChurnForecastRepository.save(forecast);
    }

    // ✅ READ ALL
    public List<MemberChurnForecast> getAllForecasts() {
        return memberChurnForecastRepository.findAll();
    }

    // ✅ READ BY ID
    public Optional<MemberChurnForecast> getForecastById(Long id) {
        return memberChurnForecastRepository.findById(id);
    }

    // ✅ READ BY MEMBER ID
    public List<MemberChurnForecast> getForecastsByMemberId(Long memberId) {
        return memberChurnForecastRepository.findByMemberId(memberId);
    }

    // ✅ READ BY RISK LEVEL
    public List<MemberChurnForecast> getForecastsByRiskLevel(String riskLevel) {
        return memberChurnForecastRepository.findByRiskLevel(riskLevel);
    }

    // ✅ UPDATE
    public MemberChurnForecast updateForecast(Long id, MemberChurnForecast forecastDetails) {

        Optional<MemberChurnForecast> optionalForecast =
                memberChurnForecastRepository.findById(id);

        if (optionalForecast.isPresent()) {

            MemberChurnForecast forecast = optionalForecast.get();

            forecast.setMember(forecastDetails.getMember());
            forecast.setChurnProbability(forecastDetails.getChurnProbability());
            forecast.setRiskLevel(forecastDetails.getRiskLevel());
            forecast.setRiskFactors(forecastDetails.getRiskFactors());
            forecast.setRecommendation(forecastDetails.getRecommendation());

            return memberChurnForecastRepository.save(forecast);
        }

        return null;
    }

    // ✅ DELETE
    public boolean deleteForecast(Long id) {

        if (memberChurnForecastRepository.existsById(id)) {
            memberChurnForecastRepository.deleteById(id);
            return true;
        }

        return false;
    }
}

