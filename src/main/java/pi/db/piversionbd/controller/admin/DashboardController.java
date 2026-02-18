package pi.db.piversionbd.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pi.db.piversionbd.service.admin.AdminUserService;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final AdminUserService adminUserService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", adminUserService.dashboardStats());
        return "dashboard";
    }
}

