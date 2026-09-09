"use client";

import { ChangeEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { AppIcon } from "../components/app-icon";

type Page<T> = { items: T[]; page: number; size: number; total: number };
type Summary = {
  total: number;
  notSubmitted: number;
  processing: number;
  invalid: number;
  pendingConfirmation: number;
  pendingApproval: number;
  approved: number;
  locked: number;
  portalSubmitted: number;
  overdue: number;
};
type CalendarItem = {
  id: string;
  templateCode: string;
  reportName: string;
  department: string;
  reportingDate: string;
  dueAt: string;
  status: string;
  batchId?: string;
  fileName?: string;
  totalRows: number;
  validRows: number;
  errorRows: number;
  warningRows: number;
  updatedAt?: string;
};
type Batch = {
  id: string;
  templateCode?: string;
  templateName?: string;
  reportingDate: string;
  originalName: string;
  status: string;
  totalRows: number;
  validRows: number;
  errorRows: number;
  warningRows: number;
  submittedBy: string;
  duplicateOf?: string;
  createdAt: string;
  updatedAt: string;
};
type Issue = {
  id: string;
  severity: string;
  code: string;
  sheet?: string;
  row?: number;
  cell?: string;
  field?: string;
  rawValue?: string;
  message: string;
};
type ImportRecord = {
  id: string;
  sheet: string;
  row: number;
  status: string;
  values: Record<string, unknown>;
};
type InternalDataSummary = {
  releases: number;
  sourceRows: number;
  metricPoints: number;
  distinctProducts: number;
  distinctPartners: number;
  unresolvedReferences: number;
  latestReportingDate?: string;
  metricTotals: Record<string, number>;
};
type InternalMetric = {
  id: string;
  reportingDate: string;
  domain: string;
  templateCode: string;
  metricCode: string;
  metricLabel: string;
  value: number;
  unitCode: string;
  productCode?: string;
  productName?: string;
  partnerType?: string;
  partnerCode?: string;
  partnerName?: string;
  qualityStatus: string;
  batchId: string;
  sourceSheet: string;
  sourceRow: number;
  contractVersion: number;
  contractHash: string;
  publishedAt: string;
};
type PortalCapability = {
  mode: string;
  status: string;
  manualTrackingEnabled: boolean;
  automaticDispatchEnabled: boolean;
  message: string;
};
type RefreshedSession = { accessToken: string; refreshToken: string };

const EMPTY_SUMMARY: Summary = {
  total: 0,
  notSubmitted: 0,
  processing: 0,
  invalid: 0,
  pendingConfirmation: 0,
  pendingApproval: 0,
  approved: 0,
  locked: 0,
  portalSubmitted: 0,
  overdue: 0,
};

const EMPTY_INTERNAL_SUMMARY: InternalDataSummary = {
  releases: 0,
  sourceRows: 0,
  metricPoints: 0,
  distinctProducts: 0,
  distinctPartners: 0,
  unresolvedReferences: 0,
  metricTotals: {},
};

const BATCH_LABELS: Record<string, string> = {
  UPLOADED: "Đã tải lên",
  IDENTIFYING: "Đang nhận diện",
  IDENTIFIED: "Đã nhận diện",
  VALIDATING: "Đang kiểm tra",
  INVALID: "Có lỗi dữ liệu",
  PENDING_CONFIRMATION: "Chờ xác nhận",
  PENDING_APPROVAL: "Chờ phê duyệt",
  APPROVED: "Đã phê duyệt",
  LOCKED: "Đã khóa",
  REJECTED: "Bị trả lại",
  UNKNOWN_TEMPLATE: "Không nhận diện được mẫu",
  SUPERSEDED: "Bản trùng",
  ERROR: "Lỗi xử lý",
};

const CALENDAR_LABELS: Record<string, string> = {
  NOT_SUBMITTED: "Chưa nộp",
  PROCESSING: "Đang xử lý",
  INVALID: "Có lỗi dữ liệu",
  PENDING_CONFIRMATION: "Chờ xác nhận",
  PENDING_APPROVAL: "Chờ phê duyệt",
  APPROVED: "Đã phê duyệt",
  LOCKED: "Đã khóa",
  PORTAL_SUBMITTED: "Đã nộp Portal",
  PORTAL_ACCEPTED: "Portal đã tiếp nhận",
  OVERDUE: "Quá hạn",
};

const STATUS_LABELS = { ...BATCH_LABELS, ...CALENDAR_LABELS };
const PROCESSING = new Set(["UPLOADED", "IDENTIFYING", "IDENTIFIED", "VALIDATING"]);
const QUALITY_LABELS: Record<string, string> = {
  VALID: "Đạt chuẩn",
  RESOLVED: "Đã chuẩn hóa",
  UNRESOLVED: "Chưa đối chiếu",
  UNRESOLVED_REFERENCE: "Chưa khớp danh mục",
  WARNING: "Cần kiểm tra",
  ERROR: "Có lỗi",
};
let sessionRefresh: Promise<boolean> | null = null;

function today() {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Ho_Chi_Minh" }).format(new Date());
}

function validReportingDate(value: string) {
  return /^\d{4}-\d{2}-\d{2}$/.test(value) && !Number.isNaN(Date.parse(`${value}T00:00:00Z`));
}

function storedToken() {
  return window.localStorage.getItem("core-access-token") || window.sessionStorage.getItem("core-access-token") || "";
}

function storedRefreshToken() {
  return window.localStorage.getItem("core-refresh-token") || window.sessionStorage.getItem("core-refresh-token") || "";
}

function persistRefreshedSession(session: RefreshedSession) {
  const remember = Boolean(window.localStorage.getItem("core-refresh-token"));
  const target = remember ? window.localStorage : window.sessionStorage;
  const other = remember ? window.sessionStorage : window.localStorage;
  other.removeItem("core-access-token");
  other.removeItem("core-refresh-token");
  target.setItem("core-access-token", session.accessToken);
  target.setItem("core-refresh-token", session.refreshToken);
}

function clearStoredSession() {
  window.localStorage.removeItem("core-access-token");
  window.localStorage.removeItem("core-refresh-token");
  window.sessionStorage.removeItem("core-access-token");
  window.sessionStorage.removeItem("core-refresh-token");
}

async function refreshAccessToken(apiUrl: string) {
  if (sessionRefresh) return sessionRefresh;
  const refreshToken = storedRefreshToken();
  if (!refreshToken) return false;
  sessionRefresh = fetch(`${apiUrl}/api/v1/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  })
    .then(async (response) => {
      if (!response.ok) {
        clearStoredSession();
        return false;
      }
      persistRefreshedSession((await response.json()) as RefreshedSession);
      return true;
    })
    .catch(() => {
      clearStoredSession();
      return false;
    })
    .finally(() => {
      sessionRefresh = null;
    });
  return sessionRefresh;
}

function statusLabel(value: string) {
  return STATUS_LABELS[value] || value;
}

function statusTone(value: string) {
  if (["APPROVED", "LOCKED", "PORTAL_SUBMITTED", "PORTAL_ACCEPTED", "VALID", "RESOLVED"].includes(value)) return "green";
  if (["INVALID", "REJECTED", "UNKNOWN_TEMPLATE", "ERROR", "OVERDUE", "UNRESOLVED_REFERENCE"].includes(value)) return "red";
  if (["PENDING_CONFIRMATION", "PENDING_APPROVAL", "PROCESSING", "UNRESOLVED", "WARNING", ...PROCESSING].includes(value)) return "amber";
  return "blue";
}

function qualityLabel(value: string) {
  return QUALITY_LABELS[value] || value;
}

function formatDateTime(value?: string) {
  return value
    ? new Intl.DateTimeFormat("vi-VN", {
        dateStyle: "short",
        timeStyle: "short",
        timeZone: "Asia/Ho_Chi_Minh",
      }).format(new Date(value))
    : "—";
}

function formatDate(value?: string) {
  if (!value) return "—";
  const parsed = new Date(`${value}T00:00:00Z`);
  return Number.isNaN(parsed.getTime()) ? value : new Intl.DateTimeFormat("vi-VN", { timeZone: "UTC" }).format(parsed);
}

function formatNumber(value: number) {
  return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 3 }).format(value);
}

function displayValue(value: unknown) {
  if (value === null || value === undefined || value === "") return "—";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

export default function DailyReportingCenter({ apiUrl }: { apiUrl: string }) {
  const [date, setDate] = useState(today());
  const [status, setStatus] = useState("");
  const [summary, setSummary] = useState<Summary>(EMPTY_SUMMARY);
  const [calendar, setCalendar] = useState<CalendarItem[]>([]);
  const [batches, setBatches] = useState<Batch[]>([]);
  const [activeTab, setActiveTab] = useState<"calendar" | "batches" | "canonical">("calendar");
  const [internalSummary, setInternalSummary] = useState<InternalDataSummary>(EMPTY_INTERNAL_SUMMARY);
  const [internalMetrics, setInternalMetrics] = useState<Page<InternalMetric>>({ items: [], page: 0, size: 50, total: 0 });
  const [internalLoading, setInternalLoading] = useState(false);
  const [internalError, setInternalError] = useState("");
  const [portalCapability, setPortalCapability] = useState<PortalCapability>({
    mode: "DISABLED",
    status: "DISABLED",
    manualTrackingEnabled: false,
    automaticDispatchEnabled: false,
    message: "Tích hợp Portal đang tạm dừng; dữ liệu đã khóa chỉ được sử dụng nội bộ.",
  });
  const [busy, setBusy] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [modalError, setModalError] = useState("");
  const [uploadError, setUploadError] = useState("");
  const [selected, setSelected] = useState<Batch>();
  const [issues, setIssues] = useState<Issue[]>([]);
  const [records, setRecords] = useState<Page<ImportRecord>>({ items: [], page: 0, size: 25, total: 0 });
  const [uploadOpen, setUploadOpen] = useState(false);
  const [file, setFile] = useState<File>();
  const fileRef = useRef<HTMLInputElement>(null);
  const selectedRef = useRef<Batch | undefined>(undefined);

  useEffect(() => {
    selectedRef.current = selected;
  }, [selected]);

  const request = useCallback(
    async (path: string, init?: RequestInit) => {
      const send = () =>
        fetch(`${apiUrl}${path}`, {
          ...init,
          headers: { Authorization: `Bearer ${storedToken()}`, ...(init?.headers || {}) },
        });
      let response = await send();
      if (response.status === 401 && (await refreshAccessToken(apiUrl))) response = await send();
      if (!response.ok) {
        const text = await response.text();
        let message = `Yêu cầu thất bại (${response.status})`;
        try {
          const body = JSON.parse(text);
          message = body.detail || body.message || message;
        } catch {
          if (text) message = text;
        }
        throw new Error(message);
      }
      if (response.status === 204) return undefined;
      const text = await response.text();
      return text ? JSON.parse(text) : undefined;
    },
    [apiUrl],
  );

  const load = useCallback(
    async (silent = false) => {
      if (!validReportingDate(date)) {
        setError("Hãy chọn ngày báo cáo hợp lệ.");
        return;
      }
      if (!silent) setBusy(true);
      setError("");
      try {
        const query = new URLSearchParams({ date, page: "0", size: "50" });
        if (status) query.set("status", status);
        const currentBatchId = selectedRef.current?.id;
        const [summaryResult, calendarResult, batchResult, selectedResult] = await Promise.all([
          request(`/api/v1/mes/reporting-calendar/summary?date=${date}`),
          request(`/api/v1/mes/reporting-calendar?${query}`),
          request("/api/v1/mes/import-batches?page=0&size=25"),
          currentBatchId ? request(`/api/v1/mes/import-batches/${currentBatchId}`) : Promise.resolve(undefined),
        ]);
        setSummary(summaryResult as Summary);
        setCalendar((calendarResult as Page<CalendarItem>).items);
        setBatches((batchResult as Page<Batch>).items);
        if (selectedResult) {
          const fresh = selectedResult as Batch;
          setSelected((current) => (current?.id === fresh.id ? fresh : current));
        }
      } catch (reason) {
        setError(reason instanceof Error ? reason.message : "Không tải được dữ liệu báo cáo.");
      } finally {
        if (!silent) setBusy(false);
      }
    },
    [date, request, status],
  );

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    let active = true;
    void request("/api/v1/mes/integrations/portal/status")
      .then((result) => { if (active) setPortalCapability(result as PortalCapability); })
      .catch(() => undefined);
    return () => { active = false; };
  }, [request]);

  const loadInternalData = useCallback(async () => {
    if (!validReportingDate(date)) return;
    setInternalLoading(true);
    setInternalError("");
    const query = new URLSearchParams({ from: date, to: date });
    try {
      const [summaryResult, metricResult] = await Promise.all([
        request(`/api/v1/mes/internal-data/summary?${query}`),
        request(`/api/v1/mes/internal-data/metrics?${query}&page=0&size=50`),
      ]);
      setInternalSummary(summaryResult as InternalDataSummary);
      setInternalMetrics(metricResult as Page<InternalMetric>);
    } catch (reason) {
      setInternalError(reason instanceof Error ? reason.message : "Không tải được dữ liệu chuẩn hóa.");
    } finally {
      setInternalLoading(false);
    }
  }, [date, request]);

  useEffect(() => {
    if (activeTab === "canonical") void loadInternalData();
  }, [activeTab, loadInternalData]);

  useEffect(() => {
    if (!batches.some((batch) => PROCESSING.has(batch.status)) && !PROCESSING.has(selected?.status || "")) return;
    const timer = window.setInterval(() => void load(true), 1800);
    return () => window.clearInterval(timer);
  }, [batches, load, selected?.status]);

  const closeUpload = useCallback(() => {
    setUploadOpen(false);
    setFile(undefined);
    setUploadError("");
    if (fileRef.current) fileRef.current.value = "";
  }, []);

  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      if (selectedRef.current) setSelected(undefined);
      else if (uploadOpen) closeUpload();
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [closeUpload, uploadOpen]);

  const loadBatchDetails = useCallback(
    async (batchId: string) => {
      const [issueResult, recordResult] = await Promise.all([
        request(`/api/v1/mes/import-batches/${batchId}/issues`),
        request(`/api/v1/mes/import-batches/${batchId}/records?page=0&size=25`),
      ]);
      setIssues(issueResult as Issue[]);
      setRecords(recordResult as Page<ImportRecord>);
    },
    [request],
  );

  const openBatch = async (batchOrId: Batch | string) => {
    setDetailLoading(true);
    setModalError("");
    setIssues([]);
    setRecords({ items: [], page: 0, size: 25, total: 0 });
    try {
      const batch =
        typeof batchOrId === "string"
          ? ((await request(`/api/v1/mes/import-batches/${batchOrId}`)) as Batch)
          : batchOrId;
      setSelected(batch);
      await loadBatchDetails(batch.id);
    } catch (reason) {
      setModalError(reason instanceof Error ? reason.message : "Không tải được chi tiết lô.");
      if (typeof batchOrId !== "string") setSelected(batchOrId);
    } finally {
      setDetailLoading(false);
    }
  };

  const upload = async () => {
    if (!validReportingDate(date)) {
      setUploadError("Hãy chọn ngày báo cáo hợp lệ.");
      return;
    }
    if (!file) {
      setUploadError("Hãy chọn một tệp Excel .xlsx.");
      return;
    }
    setBusy(true);
    setUploadError("");
    try {
      const data = new FormData();
      data.append("file", file);
      const batch = (await request(`/api/v1/mes/import-batches?reportingDate=${date}`, {
        method: "POST",
        body: data,
      })) as Batch;
      closeUpload();
      setActiveTab("batches");
      setNotice(
        batch.status === "SUPERSEDED"
          ? `${batch.originalName} trùng với lô đã có và không được xử lý lại.`
          : `Đã tiếp nhận ${batch.originalName}; hệ thống đang nhận diện và kiểm tra dữ liệu.`,
      );
      await load(true);
    } catch (reason) {
      setUploadError(reason instanceof Error ? reason.message : "Không thể tải tệp lên.");
    } finally {
      setBusy(false);
    }
  };

  const transition = async (action: "confirm" | "approve" | "lock" | "reject", body?: object) => {
    if (!selected) return;
    setBusy(true);
    setModalError("");
    try {
      const updated = (await request(`/api/v1/mes/import-batches/${selected.id}/${action}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body || {}),
      })) as Batch;
      setSelected(updated);
      setNotice("Đã cập nhật trạng thái lô báo cáo.");
      await Promise.all([load(true), loadBatchDetails(updated.id)]);
    } catch (reason) {
      setModalError(reason instanceof Error ? reason.message : "Không thể cập nhật trạng thái.");
    } finally {
      setBusy(false);
    }
  };

  const reject = () => {
    const reason = window.prompt("Nhập lý do trả lại lô báo cáo (tối thiểu 3 ký tự):");
    if (reason === null) return;
    if (reason.trim().length < 3) {
      setModalError("Lý do trả lại phải có ít nhất 3 ký tự.");
      return;
    }
    void transition("reject", { reason: reason.trim() });
  };

  const cards = useMemo(
    () =>
      [
        ["Tổng biểu mẫu", summary.total, "files", "blue"],
        ["Chưa nộp", summary.notSubmitted, "clock", "amber"],
        ["Có lỗi", summary.invalid, "circle-alert", "red"],
        ["Chờ xác nhận", summary.pendingConfirmation, "clipboard-check", "amber"],
        ["Chờ phê duyệt", summary.pendingApproval, "shield", "blue"],
        ["Đã khóa nội bộ", summary.locked, "lock", "green"],
      ] as const,
    [summary],
  );

  const recordColumns = useMemo(() => {
    const columns: string[] = [];
    for (const record of records.items) {
      for (const key of Object.keys(record.values || {})) {
        if (!columns.includes(key)) columns.push(key);
        if (columns.length === 8) return columns;
      }
    }
    return columns;
  }, [records.items]);

  return (
    <div className="mes-workspace mes-daily-center" aria-busy={busy}>
      <div className="page-heading mes-page-heading">
        <div>
          <p className="eyebrow">MES Than Mạo Khê · Dữ liệu vận hành</p>
          <h1>Trung tâm báo cáo</h1>
          <p className="page-description">Tiếp nhận Excel, kiểm tra theo Data Contract, phê duyệt, khóa và phát hành dữ liệu phục vụ vận hành nội bộ.</p>
        </div>
        <div className="mes-heading-actions">
          <button type="button" className="secondary-button" disabled={busy} onClick={() => void load()}><AppIcon name="refresh" size={14} />{busy ? "Đang tải" : "Làm mới"}</button>
          <button type="button" className="primary-button" onClick={() => { setUploadError(""); setFile(undefined); setUploadOpen(true); }}><AppIcon name="upload" size={14} />Nhập báo cáo Excel</button>
        </div>
      </div>

      {notice && <p className="operation-message" role="status">{notice}</p>}
      {error && <p className="operation-message error" role="alert">{error}</p>}

      <section className="mes-portal-pending" aria-label="Trạng thái tích hợp Portal">
        <span><AppIcon name="circle-alert" size={17} /></span>
        <div><strong>Tích hợp Portal: Tạm dừng</strong><p>{portalCapability.message}</p></div>
        <em>{portalCapability.status === "DISABLED" ? "CHƯA CẤU HÌNH" : portalCapability.status}</em>
      </section>

      <section className="mes-report-summary" aria-label="Tổng hợp trạng thái báo cáo ngày">
        {cards.map(([label, value, icon, tone]) => <article className="panel" key={label}><span className={`mes-kpi-icon ${tone}`}><AppIcon name={icon} size={18} /></span><div><small>{label}</small><strong>{value}</strong></div></article>)}
      </section>

      <section className="panel mes-report-toolbar" aria-label="Bộ lọc lịch báo cáo">
        <label>Ngày báo cáo<input type="date" required value={date} onChange={(event) => setDate(event.target.value)} /></label>
        <label>Trạng thái lịch<select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Tất cả trạng thái</option>{Object.entries(CALENDAR_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
        <p className="mes-filter-scope">Bộ lọc chỉ áp dụng cho tab <strong>Lịch báo cáo</strong>. Lô nhập gần đây luôn hiển thị 25 lô mới nhất.</p>
        <span>{summary.overdue > 0 ? `${summary.overdue} báo cáo quá hạn cần xử lý` : "Không có báo cáo quá hạn"}</span>
      </section>

      <div className="mes-tabs" role="tablist" aria-label="Dữ liệu báo cáo ngày">
        <button type="button" id="mes-calendar-tab" role="tab" aria-selected={activeTab === "calendar"} aria-controls="mes-report-table-panel" className={activeTab === "calendar" ? "active" : ""} onClick={() => setActiveTab("calendar")}>Lịch báo cáo</button>
        <button type="button" id="mes-batches-tab" role="tab" aria-selected={activeTab === "batches"} aria-controls="mes-report-table-panel" className={activeTab === "batches" ? "active" : ""} onClick={() => setActiveTab("batches")}>Lô nhập gần đây</button>
        <button type="button" id="mes-canonical-tab" role="tab" aria-selected={activeTab === "canonical"} aria-controls="mes-report-table-panel" className={activeTab === "canonical" ? "active" : ""} onClick={() => setActiveTab("canonical")}>Dữ liệu chuẩn hóa</button>
      </div>

      {activeTab === "canonical" && <section className="mes-internal-summary" aria-label="Mức sẵn sàng dữ liệu nội bộ">
        <article className="panel"><small>Bản phát hành nội bộ</small><strong>{internalSummary.releases}</strong><span>Chỉ dữ liệu đã khóa</span></article>
        <article className="panel"><small>Dòng nguồn đã chuẩn hóa</small><strong>{internalSummary.sourceRows}</strong><span>{internalSummary.distinctProducts} sản phẩm</span></article>
        <article className="panel"><small>Điểm chỉ tiêu</small><strong>{internalSummary.metricPoints}</strong><span>{internalSummary.distinctPartners} đối tác</span></article>
        <article className={`panel ${internalSummary.unresolvedReferences ? "warning" : ""}`}><small>Tham chiếu cần xác nhận</small><strong>{internalSummary.unresolvedReferences}</strong><span>{internalSummary.latestReportingDate ? `Mới nhất ${formatDate(internalSummary.latestReportingDate)}` : "Chưa có bản phát hành"}</span></article>
      </section>}

      {activeTab === "canonical" && internalError && <p className="operation-message error" role="alert">{internalError}</p>}

      <section id="mes-report-table-panel" className="panel table-panel mes-table-panel mes-report-table" role="tabpanel" aria-busy={activeTab === "canonical" && internalLoading} aria-labelledby={activeTab === "calendar" ? "mes-calendar-tab" : activeTab === "batches" ? "mes-batches-tab" : "mes-canonical-tab"}>
        <div className="mes-table-scroll"><table><caption className="sr-only">{activeTab === "calendar" ? "Lịch báo cáo theo ngày" : activeTab === "batches" ? "Các lô nhập dữ liệu gần đây" : "Dữ liệu MES đã chuẩn hóa và phát hành nội bộ"}</caption><thead><tr>{activeTab === "calendar" ? <><th>STT</th><th>Biểu mẫu</th><th>Bộ phận</th><th>Hạn nộp</th><th>Tệp dữ liệu</th><th>Dòng hợp lệ / lỗi</th><th>Trạng thái</th><th>Thao tác</th></> : activeTab === "batches" ? <><th>STT</th><th>Tệp dữ liệu</th><th>Biểu mẫu</th><th>Ngày báo cáo</th><th>Người nhập</th><th>Dòng hợp lệ / lỗi</th><th>Trạng thái</th><th>Thao tác</th></> : <><th>STT</th><th>Ngày / miền</th><th>Chỉ tiêu</th><th>Giá trị chuẩn</th><th>Sản phẩm</th><th>Đối tác</th><th>Chất lượng</th><th>Nguồn & truy vết</th></>}</tr></thead><tbody>
          {activeTab === "calendar" && calendar.map((item, index) => <tr key={item.id}><td>{index + 1}</td><td><strong>{item.reportName}</strong><small>{item.templateCode}</small></td><td>{item.department}</td><td>{formatDateTime(item.dueAt)}</td><td>{item.fileName || "—"}</td><td>{item.validRows} / <b className={item.errorRows ? "mes-error-text" : ""}>{item.errorRows}</b></td><td><span className={`mes-status ${statusTone(item.status)}`}>{statusLabel(item.status)}</span></td><td>{item.batchId ? <button type="button" className="mes-icon-button" title="Xem lô dữ liệu" aria-label={`Xem lô dữ liệu ${item.reportName}`} onClick={() => void openBatch(item.batchId as string)}><AppIcon name="eye" size={15} /></button> : "—"}</td></tr>)}
          {activeTab === "batches" && batches.map((batch, index) => <tr key={batch.id}><td>{index + 1}</td><td><strong>{batch.originalName}</strong><small>{formatDateTime(batch.createdAt)}</small></td><td>{batch.templateName || "Chưa nhận diện"}</td><td>{batch.reportingDate}</td><td>{batch.submittedBy}</td><td>{batch.validRows} / <b className={batch.errorRows ? "mes-error-text" : ""}>{batch.errorRows}</b></td><td><span className={`mes-status ${statusTone(batch.status)}`}>{statusLabel(batch.status)}</span></td><td><button type="button" className="mes-icon-button" title="Xem chi tiết" aria-label={`Xem chi tiết lô ${batch.originalName}`} onClick={() => void openBatch(batch)}><AppIcon name="eye" size={15} /></button></td></tr>)}
          {activeTab === "canonical" && internalMetrics.items.map((metric, index) => <tr key={metric.id}><td>{index + 1}</td><td><strong>{formatDate(metric.reportingDate)}</strong><small>{metric.domain}</small></td><td><strong>{metric.metricLabel}</strong><small>{metric.metricCode}</small></td><td className="numeric"><strong>{formatNumber(Number(metric.value))}</strong><small>{metric.unitCode}</small></td><td><strong>{metric.productName || "—"}</strong><small>{metric.productCode || ""}</small></td><td><strong>{metric.partnerName || "—"}</strong><small>{[metric.partnerType, metric.partnerCode].filter(Boolean).join(" · ")}</small></td><td><span className={`mes-status ${statusTone(metric.qualityStatus)}`}>{qualityLabel(metric.qualityStatus)}</span></td><td><button type="button" className="mes-lineage-button" title="Mở lô nguồn" onClick={() => void openBatch(metric.batchId)}><AppIcon name="eye" size={14} /><span>{metric.templateCode}<small>{metric.sourceSheet} · dòng {metric.sourceRow} · contract v{metric.contractVersion}</small></span></button></td></tr>)}
          {((activeTab === "calendar" && calendar.length === 0) || (activeTab === "batches" && batches.length === 0) || (activeTab === "canonical" && !internalLoading && internalMetrics.items.length === 0)) && <tr><td className="mes-empty" colSpan={8}>Không có dữ liệu phù hợp.</td></tr>}
          {activeTab === "canonical" && internalLoading && <tr><td className="mes-empty" colSpan={8}>Đang tải dữ liệu chuẩn hóa…</td></tr>}
        </tbody></table></div>
      </section>

      {uploadOpen && <div className="modal-backdrop" onMouseDown={closeUpload}><section className="panel mes-modal" role="dialog" aria-modal="true" aria-labelledby="mes-upload-title" onMouseDown={(event) => event.stopPropagation()}><header><div><p className="eyebrow">Slice 0 · Data Contract</p><h2 id="mes-upload-title">Nhập báo cáo Excel</h2></div><button type="button" onClick={closeUpload} aria-label="Đóng hộp nhập báo cáo"><AppIcon name="x" size={16} /></button></header><div className="mes-upload-body">{uploadError && <p className="operation-message error mes-modal-message" role="alert">{uploadError}</p>}<label>Ngày báo cáo<input type="date" required value={date} onChange={(event) => setDate(event.target.value)} /></label><label>Tệp Excel .xlsx<input ref={fileRef} type="file" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" onChange={(event: ChangeEvent<HTMLInputElement>) => setFile(event.target.files?.[0])} /></label><p>Hệ thống nhận diện mẫu theo tên tệp, worksheet và các cột bắt buộc; tệp tối đa 25 MB. Dữ liệu chỉ được ghi nhận sau khi vượt qua bước kiểm tra.</p></div><footer className="mes-detail-actions"><button type="button" className="secondary-button" onClick={closeUpload}>Hủy</button><button type="button" className="primary-button" disabled={busy || !file || !validReportingDate(date)} onClick={() => void upload()}><AppIcon name="upload" size={14} />Tải lên và kiểm tra</button></footer></section></div>}

      {selected && <div className="modal-backdrop" onMouseDown={() => setSelected(undefined)}><section className="panel mes-modal mes-batch-modal" role="dialog" aria-modal="true" aria-labelledby="mes-batch-title" aria-busy={detailLoading} onMouseDown={(event) => event.stopPropagation()}><header><div><p className="eyebrow">{selected.templateCode || "Chưa nhận diện"}</p><h2 id="mes-batch-title">{selected.originalName}</h2></div><button type="button" onClick={() => setSelected(undefined)} aria-label="Đóng chi tiết lô"><AppIcon name="x" size={16} /></button></header>{modalError && <p className="operation-message error mes-modal-message" role="alert">{modalError}</p>}<div className="mes-detail-grid"><div><span>Ngày báo cáo</span><strong>{selected.reportingDate}</strong></div><div><span>Trạng thái</span><strong>{statusLabel(selected.status)}</strong></div><div><span>Dòng hợp lệ</span><strong>{selected.validRows}</strong></div><div><span>Lỗi / cảnh báo</span><strong>{selected.errorRows} / {selected.warningRows}</strong></div></div>

        <div className="mes-record-preview"><h3>Dữ liệu xem trước <span>{records.total} dòng</span></h3>{detailLoading ? <p>Đang tải dữ liệu xem trước…</p> : records.items.length ? <div className="mes-table-scroll"><table><caption className="sr-only">25 dòng dữ liệu đầu tiên của lô nhập</caption><thead><tr><th>Dòng</th><th>Trạng thái</th>{recordColumns.map((column) => <th key={column}>{column}</th>)}</tr></thead><tbody>{records.items.map((record) => <tr key={record.id}><td>{record.row}</td><td><span className={`mes-status ${record.status === "VALID" ? "green" : "red"}`}>{record.status === "VALID" ? "Hợp lệ" : statusLabel(record.status)}</span></td>{recordColumns.map((column) => <td key={column}>{displayValue(record.values?.[column])}</td>)}</tr>)}</tbody></table></div> : <p>Chưa có dòng dữ liệu để xem trước.</p>}{records.total > records.items.length && <small>Đang hiển thị {records.items.length} / {records.total} dòng đầu tiên.</small>}</div>

        <div className="mes-issue-list"><h3>Kết quả kiểm tra</h3>{detailLoading ? <p>Đang tải kết quả kiểm tra…</p> : issues.length ? <table><caption className="sr-only">Lỗi và cảnh báo của lô nhập</caption><thead><tr><th>Mức độ</th><th>Vị trí</th><th>Nội dung</th></tr></thead><tbody>{issues.map((issue) => <tr key={issue.id}><td><span className={`mes-status ${issue.severity === "ERROR" ? "red" : "amber"}`}>{issue.severity === "ERROR" ? "Lỗi" : "Cảnh báo"}</span></td><td>{[issue.sheet, issue.cell || issue.row].filter(Boolean).join(" · ") || "Toàn tệp"}</td><td>{issue.message}</td></tr>)}</tbody></table> : <p>Không có lỗi hoặc cảnh báo dữ liệu.</p>}</div>

        <footer className="mes-detail-actions mes-workflow-actions"><button type="button" className="secondary-button" onClick={() => setSelected(undefined)}>Đóng</button>{selected.status === "PENDING_CONFIRMATION" && <button type="button" className="primary-button" disabled={busy} onClick={() => void transition("confirm")}><AppIcon name="check-circle" size={14} />Xác nhận số liệu</button>}{selected.status === "PENDING_APPROVAL" && <><button type="button" className="danger-button" disabled={busy} onClick={reject}>Trả lại</button><button type="button" className="primary-button" disabled={busy} onClick={() => void transition("approve")}><AppIcon name="shield" size={14} />Phê duyệt</button></>}{selected.status === "APPROVED" && <button type="button" className="primary-button" disabled={busy} onClick={() => void transition("lock")}><AppIcon name="lock" size={14} />Khóa và phát hành nội bộ</button>}{selected.status === "LOCKED" && <span className="mes-internal-ready"><AppIcon name="check-circle" size={14} />Đã phát hành dữ liệu nội bộ</span>}</footer>
      </section></div>}
    </div>
  );
}
