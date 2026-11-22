package com.budgetbuddy.api.summary;

import com.budgetbuddy.api.auth.JwtUserId;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/monthly")
    public MonthlySummary getMonthlySummary(@AuthenticationPrincipal Jwt jwt) {
        return summaryService.getCurrentMonthSummary(JwtUserId.from(jwt));
    }

    @GetMapping("/period")
    public PeriodSummary getPeriodSummary(@AuthenticationPrincipal Jwt jwt,
                                          @RequestParam String period) {
        if (!period.equalsIgnoreCase("weekly") && !period.equalsIgnoreCase("monthly")
                && !period.equalsIgnoreCase("yearly")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "period must be weekly, monthly, or yearly");
        }
        return summaryService.getPeriodSummary(JwtUserId.from(jwt), period.toLowerCase());
    }
}
