package com.baoxian.web;

import com.baoxian.app.service.DashboardService;
import com.baoxian.app.service.EventService;
import com.baoxian.app.service.ResetService;
import com.baoxian.common.R;
import com.baoxian.infra.entity.EventLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final EventService eventService;
    private final ResetService resetService;

    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        return R.ok(dashboardService.summary());
    }

    @GetMapping("/events")
    public R<List<EventLog>> events(@RequestParam(defaultValue = "30") int limit) {
        return R.ok(eventService.recent(limit));
    }

    @PostMapping("/reset")
    public R<Void> reset() {
        resetService.reset();
        return R.ok();
    }
}
