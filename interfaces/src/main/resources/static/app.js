const $ = (s, r = document) => r.querySelector(s);

async function api(path, method = 'GET', body) {
  const res = await fetch(path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined
  });
  const json = await res.json();
  if (json.code !== 0) throw new Error(json.message || '请求失败');
  return json.data;
}

let toastTimer;
function toast(msg, err = false) {
  const el = $('#toast');
  el.textContent = msg;
  el.className = 'toast show' + (err ? ' err' : '');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (el.className = 'toast'), 2800);
}

const fmt = (c) => (Number(c || 0) / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const STATUS = {
  ACTIVE: '生效', UNDERWRITING: '核保中', ISSUED: '已承保', SURRENDERED: '已退保', EXPIRED: '已到期',
  REJECTED: '拒保/拒赔', SUBMITTED: '已报案', APPROVED: '已审核', PAID: '已赔付',
  ACCRUED: '待结算', SETTLED: '已结算', ON_SALE: '在售'
};
const tag = (s) => `<span class="tag ${String(s || '').toLowerCase()}">${STATUS[s] || s || '-'}</span>`;

function empty(text) {
  return `<div class="empty">${text}</div>`;
}

/* ===== KPI ===== */
function renderKpis(d) {
  const items = [
    ['保单总数', d.policyCount, '张'],
    ['生效保单', d.activeCount, '张'],
    ['拒保件数', d.rejectedCount, '件'],
    ['总保费', fmt(d.totalPremiumCent), '元'],
    ['佣金总额', fmt(d.totalCommissionCent), '元'],
    ['待结佣金', fmt(d.accruedCommissionCent), '元'],
    ['理赔案件', d.claimCount, '件'],
    ['已赔付', fmt(d.paidClaimCent), '元']
  ];
  $('#kpis').innerHTML = items.map(([k, v, u]) =>
    `<div class="kpi"><span>${k}</span><strong>${v}</strong> <em>${u}</em></div>`).join('');

  const badge = $('#balanceBadge');
  const diff = Number(d.debitCent) - Number(d.creditCent);
  badge.className = 'badge ' + (d.balanced ? 'ok' : 'bad');
  badge.textContent = d.balanced
    ? `借贷平衡 ✓ 借 ${fmt(d.debitCent)} / 贷 ${fmt(d.creditCent)}`
    : `借贷不平 ✗ 差额 ${fmt(diff)}`;
}

async function loadDashboard() {
  try {
    renderKpis(await api('/api/dashboard'));
  } catch (e) { /* 静默 */ }
}

/* ===== 下拉选择 ===== */
async function loadSelectors() {
  const [customers, products] = await Promise.all([api('/api/customers'), api('/api/products')]);
  $('#customerSelect').innerHTML = customers.length
    ? customers.map(c => `<option value="${c.id}">${c.name}（${c.age}岁）</option>`).join('')
    : '<option value="">暂无客户</option>';
  $('#productSelect').innerHTML = products.length
    ? products.map(p => `<option value="${p.id}">${p.productName} · ${fmt(p.premiumCent)}元</option>`).join('')
    : '<option value="">暂无产品</option>';
}

/* ===== 数据面板 ===== */
let activeTab = 'policy';

function switchTab(tab) {
  activeTab = tab;
  if (location.hash !== '#' + tab) {
    history.replaceState(null, '', '#' + tab);
  }
  document.querySelectorAll('#dataTabs button').forEach(b =>
    b.classList.toggle('active', b.dataset.tab === tab));
  loadData();
}

async function loadData() {
  try {
    if (activeTab === 'policy') await renderPolicies();
    else if (activeTab === 'claim') await renderClaims();
    else if (activeTab === 'commission') await renderCommissions();
    else if (activeTab === 'account') await renderAccounts();
    else if (activeTab === 'event') await renderEvents();
  } catch (e) { /* 静默 */ }
}

async function renderPolicies() {
  const list = await api('/api/policies');
  if (!list.length) { $('#dataBody').innerHTML = empty('暂无保单，请在左侧「投保出单」'); return; }
  $('#dataBody').innerHTML = `<table><thead><tr>
    <th>保单号</th><th>产品</th><th>被保人</th><th>年龄</th><th>保额(元)</th>
    <th>保费(元)</th><th>佣金(元)</th><th>核保</th><th>状态</th><th>操作</th></tr></thead><tbody>
    ${list.map(p => `<tr>
      <td><code>${p.policyNo}</code></td>
      <td>${p.productName}</td>
      <td>${p.insuredName}</td>
      <td>${p.insuredAge}</td>
      <td class="amount">${fmt(p.coverageCent)}</td>
      <td class="amount">${fmt(p.premiumCent)}</td>
      <td class="amount">${fmt(p.commissionCent)}</td>
      <td class="muted">${p.underwritingResult || '-'}</td>
      <td>${tag(p.status)}</td>
      <td>${p.status === 'ACTIVE' ? `<button class="mini" data-act="surrender" data-no="${p.policyNo}">退保</button>` : '-'}</td>
    </tr>`).join('')}</tbody></table>`;
}

async function renderClaims() {
  const list = await api('/api/claims');
  if (!list.length) { $('#dataBody').innerHTML = empty('暂无理赔案件，请在左侧「理赔报案」'); return; }
  $('#dataBody').innerHTML = `<table><thead><tr>
    <th>理赔单号</th><th>保单号</th><th>出险类型</th><th>申请(元)</th>
    <th>核定(元)</th><th>状态</th><th>备注</th><th>操作</th></tr></thead><tbody>
    ${list.map(c => `<tr>
      <td><code>${c.claimNo}</code></td>
      <td><code>${c.policyNo}</code></td>
      <td>${c.accidentType}</td>
      <td class="amount">${fmt(c.claimCent)}</td>
      <td class="amount">${fmt(c.approvedCent)}</td>
      <td>${tag(c.status)}</td>
      <td class="muted">${c.reviewRemark || '-'}</td>
      <td>${c.status === 'APPROVED' ? `<button class="mini" data-act="pay" data-no="${c.claimNo}">赔付</button>` : '-'}</td>
    </tr>`).join('')}</tbody></table>`;
}

async function renderCommissions() {
  const list = await api('/api/commissions');
  if (!list.length) { $('#dataBody').innerHTML = empty('暂无佣金记录，出单后自动计提'); return; }
  $('#dataBody').innerHTML = `<table><thead><tr>
    <th>佣金单号</th><th>保单号</th><th>保费(元)</th><th>佣金率</th>
    <th>佣金(元)</th><th>状态</th><th>结算时间</th></tr></thead><tbody>
    ${list.map(c => `<tr>
      <td><code>${c.commissionNo}</code></td>
      <td><code>${c.policyNo}</code></td>
      <td class="amount">${fmt(c.premiumCent)}</td>
      <td>${c.commissionRate}</td>
      <td class="amount">${fmt(c.commissionCent)}</td>
      <td>${tag(c.status)}</td>
      <td class="muted">${c.settledAt || '-'}</td>
    </tr>`).join('')}</tbody></table>`;
}

async function renderAccounts() {
  const d = await api('/api/dashboard');
  $('#dataBody').innerHTML = `<table><thead><tr>
    <th>账户编号</th><th>账户名称</th><th>科目性质</th><th>余额(元)</th></tr></thead><tbody>
    ${d.accounts.map(a => `<tr>
      <td><code>${a.accountNo}</code></td>
      <td>${a.accountName}</td>
      <td class="muted">${a.accountType}</td>
      <td class="amount"><strong>${fmt(a.balanceCent)}</strong></td>
    </tr>`).join('')}</tbody></table>`;
}

async function renderEvents() {
  const list = await api('/api/events?limit=60');
  if (!list.length) { $('#dataBody').innerHTML = empty('暂无事件'); return; }
  $('#dataBody').innerHTML = `<table><thead><tr>
    <th>时间</th><th>主题</th><th>业务单号</th><th>事件</th><th>状态</th><th>详情</th></tr></thead><tbody>
    ${list.map(e => `<tr>
      <td class="muted">${e.createdAt || ''}</td>
      <td>${e.topic}</td>
      <td><code>${e.bizNo || '-'}</code></td>
      <td>${e.title}</td>
      <td>${tag(e.status)}</td>
      <td class="muted">${e.detail || ''}</td>
    </tr>`).join('')}</tbody></table>`;
}

/* ===== 表单绑定 ===== */
$('#customerForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = e.target;
  try {
    await api('/api/customers', 'POST', {
      name: f.name.value, age: Number(f.age.value),
      gender: f.gender.value, occupation: f.occupation.value
    });
    toast('客户已创建');
    f.reset();
    await loadSelectors();
    loadDashboard();
  } catch (err) { toast(err.message, true); }
});

$('#productForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = e.target;
  try {
    await api('/api/products', 'POST', {
      productName: f.productName.value,
      productType: f.productType.value,
      insurer: f.insurer.value,
      coverageCent: Math.round(Number(f.coverageYuan.value) * 100),
      premiumCent: Math.round(Number(f.premiumYuan.value) * 100),
      commissionRate: Number(f.commissionRate.value),
      riskLevel: Number(f.riskLevel.value),
      maxAge: Number(f.maxAge.value),
      termYears: 1
    });
    toast('产品已创建');
    f.reset();
    await loadSelectors();
    loadDashboard();
  } catch (err) { toast(err.message, true); }
});

$('#policyForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = e.target;
  try {
    const p = await api('/api/policies/apply', 'POST', {
      customerId: Number(f.customerId.value),
      productId: Number(f.productId.value),
      coverageYuan: Number(f.coverageYuan.value),
      insuredAge: Number(f.insuredAge.value),
      insuredName: f.insuredName.value || null
    });
    if (p.status === 'REJECTED') {
      toast('核保未通过：' + (p.underwritingReason || '拒保'), true);
    } else {
      toast(`出单成功 ${p.policyNo}，保费 ${fmt(p.premiumCent)} 元`);
      $('#claimPolicyNo').value = p.policyNo;
    }
    loadDashboard();
    loadData();
  } catch (err) { toast(err.message, true); }
});

$('#claimForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = e.target;
  try {
    const c = await api('/api/claims', 'POST', {
      policyNo: f.policyNo.value,
      accidentType: f.accidentType.value,
      claimYuan: Number(f.claimYuan.value)
    });
    toast(`报案成功 ${c.claimNo}`);
    $('#reviewClaimNo').value = c.claimNo;
    loadDashboard();
    loadData();
  } catch (err) { toast(err.message, true); }
});

$('#reviewForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const f = e.target;
  try {
    const c = await api(`/api/claims/${f.claimNo.value}/review`, 'POST', {
      approve: f.approve.value === 'true',
      approvedYuan: Number(f.approvedYuan.value),
      remark: f.remark.value
    });
    toast(`审核完成，状态 ${STATUS[c.status] || c.status}`);
    loadDashboard();
    loadData();
  } catch (err) { toast(err.message, true); }
});

$('#payBtn').addEventListener('click', async () => {
  const no = $('#payClaimNo').value.trim();
  if (!no) { toast('请输入理赔单号', true); return; }
  try {
    await api(`/api/claims/${no}/pay`, 'POST');
    toast('赔付完成，已记账');
    loadDashboard();
    loadData();
  } catch (err) { toast(err.message, true); }
});

$('#settleBtn').addEventListener('click', async () => {
  try {
    const n = await api('/api/commissions/settle', 'POST');
    toast(`已结算 ${n} 笔佣金`);
    loadDashboard();
    loadData();
  } catch (err) { toast(err.message, true); }
});

$('#resetBtn').addEventListener('click', async () => {
  if (!confirm('确定清空全部业务数据并重置为种子状态？')) return;
  try {
    await api('/api/reset', 'POST');
    toast('已重置');
    await loadSelectors();
    loadDashboard();
    loadData();
  } catch (err) { toast(err.message, true); }
});

document.querySelectorAll('#dataTabs button').forEach(b =>
  b.addEventListener('click', () => switchTab(b.dataset.tab)));

/* 表格内操作按钮事件委托 */
document.addEventListener('click', async (e) => {
  const btn = e.target.closest('button[data-act]');
  if (!btn) return;
  const no = btn.dataset.no;
  try {
    if (btn.dataset.act === 'surrender') {
      if (!confirm(`确定对保单 ${no} 办理退保？`)) return;
      await api(`/api/policies/${no}/surrender`, 'POST');
      toast('退保完成');
    } else if (btn.dataset.act === 'pay') {
      await api(`/api/claims/${no}/pay`, 'POST');
      toast('赔付完成，已记账');
    }
    loadDashboard();
    loadData();
  } catch (err) { toast(err.message, true); }
});

/* ===== 初始化与轮询 ===== */
const HASH_TABS = ['policy', 'claim', 'commission', 'account', 'event'];
loadSelectors().catch(() => {});
loadDashboard();
const initialTab = (location.hash || '').replace('#', '');
if (HASH_TABS.includes(initialTab)) {
  switchTab(initialTab);
} else {
  loadData();
}
setInterval(loadDashboard, 3000);
setInterval(loadData, 3000);
setInterval(() => { if (activeTab === 'event') renderEvents(); }, 1500);
