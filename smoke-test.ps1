$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8091/api'

function Post($path, $body) {
    $json = $body | ConvertTo-Json -Compress
    return Invoke-RestMethod -Uri "$base$path" -Method Post -Body $json -ContentType 'application/json'
}
function GetApi($path) {
    return Invoke-RestMethod -Uri "$base$path" -Method Get
}
function Assert($cond, $msg) {
    if ($cond) { Write-Host "  PASS $msg" -ForegroundColor Green }
    else { Write-Host "  FAIL $msg" -ForegroundColor Red; $script:failed++ }
}

$failed = 0

Write-Host "=== 0. Reset to clean seed state ==="
Post '/reset' @{} | Out-Null
$customers = (GetApi '/customers').data
$products = (GetApi '/products').data
$customerId = $customers[0].id
$productId = ($products | Where-Object { $_.productCode -eq 'LB-001' }).id
Write-Host ("  customerId={0} productId={1}" -f $customerId, $productId)

Write-Host "=== 1. Apply policy (underwriting -> issue -> commission accrual) ==="
$p = (Post '/policies/apply' @{ customerId = $customerId; productId = $productId; coverageYuan = 1000000; insuredAge = 35; idempotencyKey = 'IDEM-TEST-001' }).data
Write-Host ("  policy={0} status={1} premium={2} commission={3}" -f $p.policyNo, $p.status, $p.premiumCent, $p.commissionCent)
Assert ($p.status -eq 'ACTIVE') 'policy is ACTIVE'
Assert ($p.premiumCent -eq 500000) 'premium = 5000.00 yuan in cents'
Assert ($p.commissionCent -eq 125000) 'commission = 25% of premium'

Write-Host "=== 2. Idempotent apply (same idempotency key) ==="
$p2 = (Post '/policies/apply' @{ customerId = $customerId; productId = $productId; coverageYuan = 1000000; insuredAge = 35; idempotencyKey = 'IDEM-TEST-001' }).data
Assert ($p2.policyNo -eq $p.policyNo) 'duplicate request returns same policy'

Write-Host "=== 3. Claim submit ==="
$c = (Post '/claims' @{ policyNo = $p.policyNo; accidentType = 'medical'; claimYuan = 10000 }).data
Write-Host ("  claim={0} status={1}" -f $c.claimNo, $c.status)
Assert ($c.status -eq 'SUBMITTED') 'claim is SUBMITTED'

Write-Host "=== 4. Claim review (approve) ==="
$r = (Post "/claims/$($c.claimNo)/review" @{ approve = $true; approvedYuan = 8000; remark = 'ok' }).data
Assert ($r.status -eq 'APPROVED') 'claim is APPROVED'
Assert ($r.approvedCent -eq 800000) 'approved amount = 8000.00 yuan'

Write-Host "=== 5. Claim payout (ledger) ==="
$pay = (Post "/claims/$($c.claimNo)/pay" @{}).data
Assert ($pay.status -eq 'PAID') 'claim is PAID'

Write-Host "=== 6. Commission settlement ==="
$s = (Post '/commissions/settle' @{}).data
Write-Host ("  settled records={0}" -f $s)
Assert ($s -ge 1) 'at least one commission settled'

Write-Host "=== 7. Dashboard / ledger balance ==="
$d = (GetApi '/dashboard').data
Write-Host ("  policies={0} premium={1} commission={2} paidClaim={3} debit={4} credit={5} balanced={6}" -f `
    $d.policyCount, $d.totalPremiumCent, $d.totalCommissionCent, $d.paidClaimCent, $d.debitCent, $d.creditCent, $d.balanced)
Assert ($d.balanced -eq $true) 'debit equals credit'

Write-Host ""
if ($failed -eq 0) { Write-Host "ALL CHECKS PASSED" -ForegroundColor Green }
else { Write-Host "$failed CHECK(S) FAILED" -ForegroundColor Red; exit 1 }
