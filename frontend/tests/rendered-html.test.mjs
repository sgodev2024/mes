import assert from "node:assert/strict";
import { spawn } from "node:child_process";
import { cp, mkdir, readFile } from "node:fs/promises";
import test from "node:test";
import { fileURLToPath } from "node:url";

const PORT = 3311;
const ROOT = fileURLToPath(new URL("..", import.meta.url));
const BASE_URL = `http://127.0.0.1:${PORT}`;

async function waitForServer(attempts = 60) {
  for (let i = 0; i < attempts; i++) {
    try {
      const response = await fetch(BASE_URL, { headers: { accept: "text/html" } });
      if (response.status < 500) return;
    } catch {
      // server chưa sẵn sàng, thử lại
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error(`standalone server not ready at ${BASE_URL}`);
}

test("control plane console server-renders the login shell", async () => {
  await mkdir(new URL("../.next/standalone/.next", import.meta.url), { recursive: true });
  await cp(new URL("../.next/static", import.meta.url), new URL("../.next/standalone/.next/static", import.meta.url), { recursive: true });
  await cp(new URL("../public", import.meta.url), new URL("../.next/standalone/public", import.meta.url), { recursive: true });
  const server = spawn(process.execPath, [".next/standalone/server.js"], {
    cwd: ROOT,
    env: { ...process.env, PORT: String(PORT), HOSTNAME: "127.0.0.1" },
    stdio: "ignore",
  });
  try {
    await waitForServer();
    const response = await fetch(BASE_URL, { headers: { accept: "text/html" } });
    assert.equal(response.status, 200);
    assert.match(response.headers.get("content-type") ?? "", /^text\/html\b/i);

    const html = await response.text();
    assert.match(html, /<title>Core Platform — Control Plane<\/title>/i);
    assert.match(html, /<html lang="vi">/);
    assert.match(html, /Đang kiểm tra phiên đăng nhập/);
    assert.match(html, /auth-loading/);
    assert.doesNotMatch(html, /Your site is taking shape|react-loading-skeleton|codex-preview/i);
  } finally {
    server.kill();
  }
});

test("personal task navigation is not hard-coded in the application shell", async () => {
  const source = await readFile(new URL("../app/page.tsx", import.meta.url), "utf8");
  assert.doesNotMatch(source, /Công việc của tôi/i);
  assert.match(source, /api\/v1\/navigation\/me/);
});

test("home page does not render the deployment environment summary strip", async () => {
  const source = await readFile(new URL("../app/page.tsx", import.meta.url), "utf8");
  const styles = await readFile(new URL("../app/globals.css", import.meta.url), "utf8");
  assert.doesNotMatch(source, /environment-panel|Mô hình vận hành|Dedicated deployment<\/small>/i);
  assert.doesNotMatch(styles, /environment-panel|environment-label/i);
});

test("approval demo is isolated from the production application shell", async () => {
  const shell = await readFile(new URL("../app/page.tsx", import.meta.url), "utf8");
  const demo = await readFile(new URL("../app/demo/approval-workspace.tsx", import.meta.url), "utf8");
  assert.doesNotMatch(shell, /api\/v1\/approvals/);
  assert.match(shell, /dynamic\(\(\) => import\("\.\/demo\/approval-workspace"\)/);
  assert.match(demo, /api\/v1\/approvals/);
});

test("home is a standalone top-level entry and business owns module navigation", async () => {
  const source = await readFile(new URL("../app/page.tsx", import.meta.url), "utf8");
  const styles = await readFile(new URL("../app/globals.css", import.meta.url), "utf8");
  assert.match(source, /if\(section\.key==="home"\).*renderPageButton\(home\)/s);
  assert.match(source, /className={`nav-section-trigger/);
  assert.match(source, /rootItems\.length===0.*Chưa có module được cấp quyền/s);
  assert.match(styles, /\.nav-section-trigger\s*\{/);
  assert.match(styles, /\.nav-section-children\s*\{/);
});

test("MES baseline v1 exposes the approved 18 screens inside the existing Core shell", async () => {
  const shell = await readFile(new URL("../app/page.tsx", import.meta.url), "utf8");
  const workspace = await readFile(new URL("../app/mes/mes-workspace.tsx", import.meta.url), "utf8");
  const dailyReporting = await readFile(new URL("../app/mes/daily-reporting-center.tsx", import.meta.url), "utf8");
  const fixtures = await readFile(new URL("../app/mes/mes-demo-data.ts", import.meta.url), "utf8");
  const route = await readFile(new URL("../app/mes/[...path]/page.tsx", import.meta.url), "utf8");

  assert.match(shell, /import MesWorkspace/);
  assert.match(shell, /view\.startsWith\("mes-"\).*<MesWorkspace/s);
  for (const view of [
    "mes-dashboard", "mes-planning-performance", "mes-production-operations", "mes-coal-flow",
    "mes-quality-acceptance", "mes-sales-logistics", "mes-materials-equipment", "mes-occupational-safety",
    "mes-finance-accounting", "mes-workforce-labor", "mes-investment-projects", "mes-science-digital",
    "mes-alerts-directives", "mes-esg", "mes-reporting-center", "mes-portal-tkv", "mes-data-mapping",
    "mes-integration-quality",
  ]) assert.match(workspace, new RegExp(`"${view}"`));
  for (const label of ["Kế hoạch & hiệu quả","Sản xuất & điều hành","Chất lượng & nghiệm thu","An toàn lao động","Tài chính & Kế toán","ESG","Portal\/TKV","Tích hợp & chất lượng dữ liệu"])
    assert.match(fixtures, new RegExp(label));
  assert.doesNotMatch(workspace, /Prototype · Chờ API/);
  assert.match(workspace, /Dữ liệu hoạt động/);
  assert.match(workspace, /\/api\/v1\/mes\/modules\/\$\{module\}\/overview/);
  assert.match(workspace, /\/api\/v1\/mes\/modules\/\$\{module\}\/records/);
  assert.match(workspace, /\/workflow/);
  assert.match(workspace, /START:"Bắt đầu"/);
  assert.match(workspace, /APPROVE:"Phê duyệt"/);
  assert.match(workspace, /function ColumnChart/);
  assert.match(shell, /LEGACY_MES_ROUTES/);
  assert.match(shell, /"\/mes\/warehouse":"\/mes\/coal-flow"/);
  assert.match(route, /export \{ default \} from "\.\.\/\.\.\/page"/);
  assert.doesNotMatch(workspace, /Quản trị hệ thống|Quản trị & phân quyền/);
  assert.match(shell, /<MesWorkspace view=\{view as MesView\} apiUrl=\{API_URL\}/);
  assert.match(dailyReporting, /\/api\/v1\/mes\/reporting-calendar/);
  assert.match(dailyReporting, /\/api\/v1\/mes\/import-batches/);
  assert.match(dailyReporting, /\/records\?page=0&size=25/);
  assert.match(dailyReporting, /Dữ liệu xem trước/);
  assert.match(dailyReporting, /\/api\/v1\/auth\/refresh/);
  assert.match(dailyReporting, /CALENDAR_LABELS/);
  assert.match(dailyReporting, /Xác nhận số liệu|Phê duyệt/);
  assert.match(dailyReporting, /Tích hợp Portal: Tạm dừng/);
  assert.match(dailyReporting, /Dữ liệu chuẩn hóa/);
  assert.match(dailyReporting, /\/api\/v1\/mes\/internal-data\/summary/);
  assert.match(dailyReporting, /\/api\/v1\/mes\/internal-data\/metrics/);
  assert.match(dailyReporting, /Khóa và phát hành nội bộ/);
  assert.doesNotMatch(dailyReporting, /\/portal-submissions/);
  assert.doesNotMatch(dailyReporting, /Ghi nhận đã nộp Portal/);
});

test("green transformation background tokens are applied to the shell and authentication", async () => {
  const styles = await readFile(new URL("../app/globals.css", import.meta.url), "utf8");
  assert.match(styles, /--transition-green-950:\s*#062f24/i);
  assert.match(styles, /--transition-green-600:\s*#238558/i);
  assert.match(styles, /body\s*\{[^}]*linear-gradient\(145deg,var\(--transition-green-50\),var\(--canvas\)\)/s);
  assert.match(styles, /\.main-area\s*\{[^}]*rgba\(225,240,229,\.62\)/s);
  assert.match(styles, /\.auth-page\s*\{[^}]*var\(--transition-green-100\)/s);
});

test("MES executive dashboard, inventory reconciliation and master data use live canonical APIs", async () => {
  const workspace = await readFile(new URL("../app/mes/mes-workspace.tsx", import.meta.url), "utf8");
  const dashboard = await readFile(new URL("../app/mes/mes-executive-dashboard.tsx", import.meta.url), "utf8");
  const reconciliation = await readFile(new URL("../app/mes/mes-inventory-reconciliation.tsx", import.meta.url), "utf8");
  const masterData = await readFile(new URL("../app/mes/mes-master-data.tsx", import.meta.url), "utf8");
  const api = await readFile(new URL("../app/mes/mes-live-api.ts", import.meta.url), "utf8");
  assert.match(workspace, /if\(view==="mes-dashboard"\)\s*return <MesExecutiveDashboard/);
  assert.match(workspace, /if\(view==="mes-coal-flow"\)\s*return <MesInventoryReconciliation/);
  assert.match(workspace, /if\(view==="mes-data-mapping"\)\s*return <MesMasterData/);
  assert.match(workspace, /if\(view==="mes-reporting-center"\)\s*return <DailyReportingCenter/);
  assert.match(dashboard, /\/api\/v1\/mes\/internal-data\/summary/);
  assert.match(dashboard, /\/api\/v1\/mes\/reconciliation\/inventory/);
  assert.match(dashboard, /Dashboard điều hành/);
  assert.match(dashboard, /Điều hành tổng hợp/);
  assert.match(dashboard, /Nhịp sản xuất 14 ngày gần nhất/);
  assert.match(dashboard, /Quyết định cần ban hành/);
  assert.match(dashboard, /Hiệu quả theo sản phẩm/);
  assert.match(dashboard, /An toàn & rủi ro vận hành/);
  assert.doesNotMatch(dashboard, /Dashboard Ban lãnh đạo/);
  assert.doesNotMatch(dashboard, /mesDemoTables/);
  assert.match(reconciliation, /BALANCED|VARIANCE|INCOMPLETE|MASTER_UNVERIFIED/);
  assert.match(masterData, /\/api\/v1\/mes\/master-data\/\$\{path\}/);
  assert.match(masterData, /tab==="products"\?"products":"partners"/);
  assert.match(api, /INVALID_JSON_RESPONSE/);
});

test("ESG login copy, semantic icons and real operations APIs are enforced", async () => {
  const shell = await readFile(new URL("../app/page.tsx", import.meta.url), "utf8");
  const styles = await readFile(new URL("../app/globals.css", import.meta.url), "utf8");
  const packageJson = JSON.parse(await readFile(new URL("../package.json", import.meta.url), "utf8"));
  assert.equal(packageJson.dependencies.next, "16.3.1");
  assert.match(shell, /Giải pháp tối ưu hóa vận hành doanh nghiệp/);
  assert.match(shell, /Quản trị vận hành, tài nguyên, phân quyền từ một trung tâm duy nhất/);
  assert.match(styles, /\.auth-message h1\s*\{[^}]*color:\s*#fff/i);
  assert.match(styles, /--navy-950:\s*#092e28/i);
  assert.match(shell, /control-plane\/jobs/);
  assert.match(shell, /control-plane\/outbox/);
  assert.match(shell, /api\/v1\/auth\/refresh/);
  assert.doesNotMatch(shell, /<strong>12<\/strong>|<strong>0\.8s<\/strong>/);
});
