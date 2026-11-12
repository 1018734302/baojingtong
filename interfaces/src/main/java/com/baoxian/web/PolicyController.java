package com.baoxian.web;

import com.baoxian.app.service.PolicyService;
import com.baoxian.common.MoneyUtils;
import com.baoxian.common.R;
import com.baoxian.infra.entity.Policy;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public R<List<Policy>> list() {
        return R.ok(policyService.list());
    }

    @PostMapping("/apply")
    public R<Policy> apply(@RequestBody Map<String, Object> body) {
        Long customerId = toLong(body.get("customerId"));
        Long productId = toLong(body.get("productId"));
        Integer insuredAge = body.get("insuredAge") == null ? null : toLong(body.get("insuredAge")).intValue();
        long coverageCent = MoneyUtils.toCent(body.get("coverageYuan"));
        String insuredName = body.get("insuredName") == null ? null : body.get("insuredName").toString();
        String idemKey = body.get("idempotencyKey") == null ? null : body.get("idempotencyKey").toString();
        return R.ok(policyService.apply(customerId, productId, coverageCent, insuredAge, insuredName, idemKey));
    }

    @PostMapping("/{policyNo}/surrender")
    public R<Policy> surrender(@PathVariable String policyNo) {
        return R.ok(policyService.surrender(policyNo));
    }

    private Long toLong(Object o) {
        return o == null ? null : Long.valueOf(String.valueOf(o).trim());
    }
}
