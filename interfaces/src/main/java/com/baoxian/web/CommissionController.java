package com.baoxian.web;

import com.baoxian.app.service.CommissionService;
import com.baoxian.common.R;
import com.baoxian.infra.entity.CommissionRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping
    public R<List<CommissionRecord>> list() {
        return R.ok(commissionService.list());
    }

    @PostMapping("/settle")
    public R<Integer> settleAll() {
        return R.ok(commissionService.settleAll());
    }

    @PostMapping("/settle/{policyNo}")
    public R<CommissionRecord> settleByPolicy(@PathVariable String policyNo) {
        return R.ok(commissionService.settleByPolicy(policyNo));
    }
}
