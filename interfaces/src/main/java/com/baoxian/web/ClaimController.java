package com.baoxian.web;

import com.baoxian.app.service.ClaimService;
import com.baoxian.common.MoneyUtils;
import com.baoxian.common.R;
import com.baoxian.infra.entity.Claim;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @GetMapping
    public R<List<Claim>> list() {
        return R.ok(claimService.list());
    }

    @PostMapping
    public R<Claim> submit(@RequestBody Map<String, Object> body) {
        String policyNo = String.valueOf(body.get("policyNo"));
        String accidentType = body.get("accidentType") == null ? "其他" : body.get("accidentType").toString();
        long claimCent = MoneyUtils.toCent(body.get("claimYuan"));
        return R.ok(claimService.submit(policyNo, accidentType, claimCent));
    }

    @PostMapping("/{claimNo}/review")
    public R<Claim> review(@PathVariable String claimNo, @RequestBody Map<String, Object> body) {
        boolean approve = body.get("approve") == null || Boolean.parseBoolean(String.valueOf(body.get("approve")));
        long approvedCent = MoneyUtils.toCent(body.get("approvedYuan"));
        String remark = body.get("remark") == null ? null : body.get("remark").toString();
        return R.ok(claimService.review(claimNo, approve, approvedCent, remark));
    }

    @PostMapping("/{claimNo}/pay")
    public R<Claim> pay(@PathVariable String claimNo) {
        return R.ok(claimService.pay(claimNo));
    }
}
