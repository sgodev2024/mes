package vn.coreplatform.mes.reporting;

import static vn.coreplatform.shared.ApiExceptionHandler.ApiProblem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.coreplatform.audit.AuditService;
import vn.coreplatform.eventing.OutboxService;
import vn.coreplatform.filemanagement.FileStorageService;
import vn.coreplatform.jobs.JobService;
import vn.coreplatform.permission.PermissionService;

/** Application service cho Data Contract pilot và Trung tâm báo cáo ngày. */
@Service
public class MesDailyReportingService {
  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final long MAX_FILE_SIZE = 25L * 1024 * 1024;
  private static final Set<String> PROTECTED_CALENDAR_STATUSES = Set.of(
      "LOCKED", "PORTAL_SUBMITTED", "PORTAL_ACCEPTED", "PORTAL_REJECTED");

  private final JdbcTemplate jdbc;
  private final PermissionService permissions;
  private final FileStorageService storage;
  private final JobService jobs;
  private final MesWorkbookParser parser;
  private final AuditService audits;
  private final OutboxService outbox;
  private final ObjectMapper json;
  private final MesPortalConfiguration portal;
  private final MesCanonicalDataService canonicalData;

  public MesDailyReportingService(
      JdbcTemplate jdbc,
      PermissionService permissions,
      FileStorageService storage,
      JobService jobs,
      MesWorkbookParser parser,
      AuditService audits,
      OutboxService outbox,
      ObjectMapper json,
      MesPortalConfiguration portal,
      MesCanonicalDataService canonicalData) {
    this.jdbc = jdbc;
    this.permissions = permissions;
    this.storage = storage;
    this.jobs = jobs;
    this.parser = parser;
    this.audits = audits;
    this.outbox = outbox;
    this.json = json;
    this.portal = portal;
    this.canonicalData = canonicalData;
  }

  public record TemplateView(
      UUID id, String code, String name, String department, String frequency, String filenamePattern,
      String worksheetName, String parserType, int version, LocalTime dueTime, boolean active,
      JsonNode fieldContract, String contractHash, LocalDate effectiveFrom, String lifecycleStatus) {}

  public record BatchView(
      UUID id, String templateCode, String templateName, LocalDate reportingDate, String periodKey,
      String originalName, String checksumSha256, String status, int totalRows, int validRows,
      int errorRows, int warningRows, String submittedBy, UUID duplicateOf, Instant createdAt, Instant updatedAt) {}

  public record CalendarItemView(
      UUID id, String templateCode, String reportName, String department, LocalDate reportingDate,
      Instant dueAt, String status, UUID batchId, String fileName, int totalRows, int validRows,
      int errorRows, int warningRows, Instant updatedAt) {}

  public record CalendarSummary(
      long total, long notSubmitted, long processing, long invalid, long pendingConfirmation,
      long pendingApproval, long approved, long locked, long portalSubmitted, long overdue) {}

  public record ImportRecordView(UUID id, String sheet, int row, String status, JsonNode values) {}

  public record IssueView(
      UUID id, String severity, String code, String sheet, Integer row, String cell,
      String field, String rawValue, String message) {}

  public record PageResult<T>(List<T> items, int page, int size, long total) {}

  public record PortalSubmissionView(
      UUID id, UUID calendarItemId, UUID batchId, int attemptNo, String status,
      String receiptCode, String note, String submittedBy, Instant submittedAt) {}

  public MesPortalConfiguration.Capability portalCapability() {
    return portal.capability();
  }

  private record PilotTemplate(
      String code, String name, String department, String filenamePattern, String sheet,
      String parserType, List<String> tokens, List<MesWorkbookParser.FieldSpec> fields, LocalTime dueTime) {}

  private static final List<PilotTemplate> PILOT_TEMPLATES = List.of(
      new PilotTemplate("MES-DAY-PRODUCTION-PTCB", "Thực hiện pha trộn, chế biến ngày", "Ban Sản xuất - Tiêu thụ than",
          "^4100_TT_Ngay_PTCB_01\\.xlsx$", "Template_Import", "MULTI_HEADER",
          List.of("Mã sản phẩm", "Nhập mua về PTCB", "Điểm PTCB"), List.of(
              textField("product_code", "Mã sản phẩm", true),
              textField("product_name", "Tên sản phẩm", true),
              textField("unit", "ĐVT", true),
              textField("origin", "Xuất xứ", false),
              amountField("purchase_for_processing", "Nhập mua về PTCB", "quantity"),
              amountField("resale_to_tkv", "Bán lại TKV", "quantity"),
              amountField("closing_stock", "Tồn kho", "quantity"),
              textField("category_name", "Tên loại", false),
              numberField("processing_point", "Điểm PTCB", false, 0d, null, null)), LocalTime.of(17, 0)),
      new PilotTemplate("MES-DAY-RAW-COAL-RECEIPT", "Nhập kho than nguyên khai ngày", "Ban Sản xuất - Tiêu thụ than",
          "^4100_TT_NhapKho_Ngay_thanNK\\.xlsx$", "Template_Import", "MULTI_HEADER",
          List.of("Mã nhà cung cấp", "Nhập khai thác", "Thông số kỹ thuật", "Wtp (%)"), List.of(
              textField("supplier_code", "Mã nhà cung cấp", false),
              textField("supplier_name", "Tên nhà cung cấp", false),
              textField("product_code", "Mã sản phẩm", true),
              textField("product_name", "Tên sản phẩm", true),
              textField("unit", "ĐVT", true),
              amountField("external_purchase", "Nhập mua ngoài TKV", "quantity"),
              amountField("internal_purchase", "Nhập mua trong TKV", "quantity"),
              amountField("mined_receipt", "Nhập khai thác", "quantity"),
              textField("mining_method", "PT Khai thác", false),
              textField("origin", "Xuất xứ", false),
              numberField("dry_quantity", "SL quy ẩm", false, 0d, null, null),
              percentField("lump_percent", "% Cục"),
              percentField("rock_percent", "% Đá"),
              percentField("waste_percent", "% Kẹp sít"),
              percentField("moisture_percent", "Wtp (%)"),
              percentField("ash_percent", "Ak (%)"),
              numberField("calorific_value", "Nhiệt năng", false, 0d, null, null),
              percentField("volatile_matter", "Chất bốc"),
              textField("vessel_name", "Tên tàu", false)), LocalTime.of(17, 0)),
      new PilotTemplate("MES-DAY-CLEAN-COAL-RECEIPT", "Nhập kho than sạch ngày", "Ban Sản xuất - Tiêu thụ than",
          "^4100_TT_NhapKho_Ngay_than_sach\\.xlsx$", "Template_Import", "MULTI_HEADER",
          List.of("Mã nhà cung cấp", "Mã sản phẩm", "Nhập từ chế biến", "Tên tàu"), List.of(
              textField("supplier_code", "Mã nhà cung cấp", false),
              textField("supplier_name", "Tên nhà cung cấp", false),
              textField("product_code", "Mã sản phẩm", true),
              textField("product_name", "Tên sản phẩm", true),
              textField("unit", "ĐVT", true),
              amountField("external_purchase", "Nhập mua ngoài TKV", "quantity"),
              amountField("internal_purchase", "Nhập mua trong TKV", "quantity"),
              amountField("processing_receipt", "Nhập từ chế biến", "quantity"),
              textField("origin", "Xuất xứ", false),
              textField("vessel_name", "Tên tàu", false)), LocalTime.of(17, 0)),
      new PilotTemplate("MES-DAY-STOCK", "Tồn kho than ngày", "Ban Sản xuất - Tiêu thụ than",
          "^4100_TT_Tonkho_Ngay\\.xlsx$", "Template_Import", "TABULAR",
          List.of("Mã sản phẩm", "Số lượng tồn kho cuối ngày", "Số lượng đi đường"), List.of(
              textField("product_code", "Mã sản phẩm", true),
              textField("product_name", "Tên sản phẩm", true),
              textField("unit", "ĐVT", true),
              numberField("closing_stock", "Số lượng tồn kho cuối ngày", true, 0d, null, null),
              numberField("in_transit", "Số lượng đi đường", false, 0d, null, null)), LocalTime.of(17, 0)),
      new PilotTemplate("MES-DAY-CLEAN-COAL-ISSUE", "Xuất kho than sạch ngày", "Ban Sản xuất - Tiêu thụ than",
          "^4100_TT_XuatKho_Ngay_than_sach\\.xlsx$", "Template_Import", "TABULAR",
          List.of("Mã khách hàng", "Mã sản phẩm", "Số lượng xuất bán", "Số lượng xuất chế biến"), List.of(
              textField("customer_code", "Mã khách hàng", false),
              textField("customer_name", "Tên khách hàng", false),
              textField("product_code", "Mã sản phẩm", true),
              textField("product_name", "Tên sản phẩm", true),
              textField("unit", "ĐVT", true),
              amountField("sale_issue", "Số lượng xuất bán", "quantity"),
              amountField("processing_issue", "Số lượng xuất chế biến", "quantity"),
              textField("vessel_name", "Tên tàu", false),
              textField("movement_type", "Loại hình", false)), LocalTime.of(17, 0)),
      new PilotTemplate("MES-DAY-RAW-COAL-ISSUE", "Xuất kho than nguyên khai ngày", "Ban Sản xuất - Tiêu thụ than",
          "^4100_TT_XuatKho_Ngay_thanNK\\.xlsx$", "Template_Import", "TABULAR",
          List.of("Mã khách hàng", "Mã sản phẩm", "Số lượng xuất Giao tuyển", "Tên tàu"), List.of(
              textField("customer_code", "Mã khách hàng", false),
              textField("customer_name", "Tên khách hàng", false),
              textField("product_code", "Mã sản phẩm", true),
              textField("product_name", "Tên sản phẩm", true),
              textField("unit", "ĐVT", true),
              amountField("sale_issue", "Số lượng xuất bán", "quantity"),
              amountField("screening_issue", "Số lượng xuất Giao tuyển", "quantity"),
              amountField("processing_issue", "Số lượng xuất chế biến", "quantity"),
              textField("vessel_name", "Tên tàu", false),
              textField("movement_type", "Loại hình", false)), LocalTime.of(17, 0)));

  private static MesWorkbookParser.FieldSpec textField(String code, String label, boolean required) {
    return new MesWorkbookParser.FieldSpec(code, label, "TEXT", required, null, null, null);
  }

  private static MesWorkbookParser.FieldSpec amountField(String code, String label, String group) {
    return numberField(code, label, false, 0d, null, group);
  }

  private static MesWorkbookParser.FieldSpec percentField(String code, String label) {
    return numberField(code, label, false, 0d, 100d, null);
  }

  private static MesWorkbookParser.FieldSpec numberField(
      String code, String label, boolean required, Double minimum, Double maximum, String group) {
    return new MesWorkbookParser.FieldSpec(code, label, "NUMBER", required, minimum, maximum, group);
  }

  @Transactional
  public List<TemplateView> templates(Authentication authentication) {
    UUID tenant = permissions.tenant(authentication);
    ensurePilotTemplates(tenant);
    return jdbc.query("""
        select id,template_code,name,department,frequency,filename_pattern,worksheet_name,parser_type,version,due_time,active,
               field_contract::text,contract_hash,effective_from,lifecycle_status
        from mes.template_definition where tenant_id=? order by active desc,department,name,version desc
        """, (row, index) -> templateView(row), tenant);
  }

  /**
   * Seed/backfill một tenant trong transaction riêng. Caller phải đặt TenantContext
   * trước khi gọi để TenantAwareDataSource gắn GUC và RLS tiếp tục fail-closed.
   */
  @Transactional
  public void ensurePilotTemplatesForTenant(UUID tenant) {
    ensurePilotTemplates(tenant);
  }

  @Transactional
  public PageResult<CalendarItemView> calendar(
      Authentication authentication, LocalDate date, String status, int page, int size) {
    UUID tenant = permissions.tenant(authentication);
    ensurePilotTemplates(tenant);
    ensureCalendar(tenant, date);
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(100, size));
    String safeStatus = Optional.ofNullable(status).orElse("").trim().toUpperCase();
    long total = jdbc.queryForObject("""
        select count(*) from mes.reporting_calendar_item c
        where c.tenant_id=? and c.reporting_date=? and (?='' or c.status=?)
        """, Long.class, tenant, date, safeStatus, safeStatus);
    var items = jdbc.query("""
        select c.id,t.template_code,t.name,t.department,c.reporting_date,c.due_at,c.status,
               b.id batch_id,b.original_name,b.total_rows,b.valid_rows,b.error_rows,b.warning_rows,c.updated_at
        from mes.reporting_calendar_item c
        join mes.template_definition t on t.id=c.template_id and t.tenant_id=c.tenant_id
        left join mes.import_batch b on b.id=c.current_batch_id and b.tenant_id=c.tenant_id
        where c.tenant_id=? and c.reporting_date=? and (?='' or c.status=?)
        order by c.due_at,t.name limit ? offset ?
        """, (row, index) -> calendarItem(row), tenant, date, safeStatus, safeStatus, safeSize, safePage * safeSize);
    return new PageResult<>(items, safePage, safeSize, total);
  }

  @Transactional
  public CalendarSummary summary(Authentication authentication, LocalDate date) {
    UUID tenant = permissions.tenant(authentication);
    ensurePilotTemplates(tenant);
    ensureCalendar(tenant, date);
    return jdbc.queryForObject("""
        select count(*) total,
          count(*) filter(where status='NOT_SUBMITTED') not_submitted,
          count(*) filter(where status='PROCESSING') processing,
          count(*) filter(where status='INVALID') invalid,
          count(*) filter(where status='PENDING_CONFIRMATION') pending_confirmation,
          count(*) filter(where status='PENDING_APPROVAL') pending_approval,
          count(*) filter(where status='APPROVED') approved,
          count(*) filter(where status='LOCKED') locked,
          count(*) filter(where status in ('PORTAL_SUBMITTED','PORTAL_ACCEPTED')) portal_submitted,
          count(*) filter(where status='OVERDUE') overdue
        from mes.reporting_calendar_item where tenant_id=? and reporting_date=?
        """, (row, index) -> new CalendarSummary(
        row.getLong("total"), row.getLong("not_submitted"), row.getLong("processing"),
        row.getLong("invalid"), row.getLong("pending_confirmation"), row.getLong("pending_approval"),
        row.getLong("approved"), row.getLong("locked"), row.getLong("portal_submitted"), row.getLong("overdue")), tenant, date);
  }

  @Transactional
  public BatchView upload(Authentication authentication, MultipartFile file, LocalDate reportingDate) throws Exception {
    if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE)
      throw new ApiProblem(HttpStatus.BAD_REQUEST, "MES_FILE_SIZE", "File Excel rỗng hoặc vượt 25 MB");
    String originalName = sanitize(file.getOriginalFilename());
    if (!originalName.toLowerCase().endsWith(".xlsx"))
      throw new ApiProblem(HttpStatus.BAD_REQUEST, "MES_FILE_TYPE", "Slice 1 chỉ nhận file .xlsx không có macro");

    UUID tenant = permissions.tenant(authentication);
    UUID actor = permissions.account(authentication);
    String tenantKey = permissions.tenantKey(authentication);
    ensurePilotTemplates(tenant);
    UUID batchId = UUID.randomUUID();
    UUID fileId = storage.createSession(tenant, tenantKey, actor, originalName,
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "CONFIDENTIAL", "MES_IMPORT", batchId.toString());
    var uploaded = storage.writeContent(fileId, file.getInputStream());
    storage.finalizeUpload(fileId);

    advisoryTransactionLock(
        "mes:checksum:" + tenant + ":" + reportingDate + ":" + uploaded.checksumSha256());
    var duplicate = jdbc.query("""
        select id from mes.import_batch
        where tenant_id=? and reporting_date=? and checksum_sha256=? and status<>'SUPERSEDED'
        order by created_at desc limit 1
        """, (row, index) -> row.getObject(1, UUID.class), tenant, reportingDate, uploaded.checksumSha256());
    String status = duplicate.isEmpty() ? "UPLOADED" : "SUPERSEDED";
    UUID duplicateOf = duplicate.isEmpty() ? null : duplicate.getFirst();
    jdbc.update("""
        insert into mes.import_batch(id,tenant_id,file_id,duplicate_of,reporting_date,period_key,original_name,
          checksum_sha256,status,submitted_by)
        values(?,?,?,?,?,?,?,?,?,?)
        """, batchId, tenant, fileId, duplicateOf, reportingDate, reportingDate.toString(), originalName,
        uploaded.checksumSha256(), status, actor);

    audits.record(tenantKey, actor, authentication.getName(), "MES_REPORT_UPLOADED", "MES_IMPORT_BATCH",
        batchId.toString(), "SUCCESS", json(Map.of("fileName", originalName, "reportingDate", reportingDate.toString(),
            "checksum", uploaded.checksumSha256(), "duplicate", duplicateOf != null)));
    if (duplicateOf == null) {
      jobs.enqueue(tenantKey, MesImportJobHandler.JOB_TYPE,
          json(Map.of("batchId", batchId.toString())), "mes-import-" + batchId);
    }
    return batch(authentication, batchId);
  }

  @Transactional
  public BatchView processBatch(UUID batchId) {
    return processBatch(batchId, () -> {});
  }

  @Transactional
  public BatchView processBatch(UUID batchId, Runnable heartbeat) {
    heartbeat.run();
    var source = claimUploadedBatch(batchId);
    if (source.isEmpty()) {
      var existing = sourceBatch(batchId);
      if (existing.isEmpty())
        throw new ApiProblem(HttpStatus.NOT_FOUND, "MES_IMPORT_NOT_FOUND", "Không tìm thấy lô import");
      return batchById(batchId, existing.getFirst().tenantId());
    }
    var item = source.getFirst();
    Path path = storage.resolve(item.storageKey());
    var specs = templateSpecsForTenant(item.tenantId());
    try {
      var identified = parser.identify(path, item.originalName(), specs, heartbeat);
      if (identified.isEmpty()) {
        replaceIssues(batchId, item.tenantId(), List.of(new MesWorkbookParser.Issue(
            "ERROR", "UNKNOWN_TEMPLATE", null, null, null, null, item.originalName(),
            "Không nhận diện được biểu mẫu. Kiểm tra tên file, sheet và tiêu đề bắt buộc.")));
        jdbc.update("""
            update mes.import_batch set status='UNKNOWN_TEMPLATE',total_rows=0,valid_rows=0,error_rows=1,warning_rows=0,updated_at=now()
            where id=?
            """, batchId);
        audits.record(item.tenantKey(), null, "system", "MES_REPORT_IDENTIFICATION_FAILED", "MES_IMPORT_BATCH",
            batchId.toString(), "FAILED", json(Map.of("fileName", item.originalName())));
        return batchById(batchId, item.tenantId());
      }

      var template = identified.get();
      jdbc.update("update mes.import_batch set template_id=?,status='VALIDATING',updated_at=now() where id=?", template.id(), batchId);
      var protectedCalendarStatus = bindCalendarForProcessing(item.tenantId(), template.id(), batchId);
      if (protectedCalendarStatus.isPresent()) {
        String currentStatus = protectedCalendarStatus.get();
        String message = "Kỳ báo cáo " + reportDate(batchId, item.tenantId()) + " đang ở trạng thái "
            + currentStatus + "; không thể thay thế dữ liệu đã khóa hoặc đã ghi nhận tích hợp";
        replaceIssues(batchId, item.tenantId(), List.of(new MesWorkbookParser.Issue(
            "ERROR", "REPORTING_PERIOD_PROTECTED", template.worksheetName(), null, null,
            "reportingDate", reportDate(batchId, item.tenantId()).toString(), message)));
        jdbc.update("""
            update mes.import_batch set status='REJECTED',rejected_reason=?,total_rows=0,valid_rows=0,
              error_rows=1,warning_rows=0,updated_at=now(),version=version+1 where id=?
            """, message, batchId);
        audits.record(item.tenantKey(), null, "system", "MES_REPORT_PROTECTED_PERIOD_REJECTED",
            "MES_IMPORT_BATCH", batchId.toString(), "FAILED",
            json(Map.of("calendarStatus", currentStatus, "templateCode", template.code())));
        return batchById(batchId, item.tenantId());
      }
      heartbeat.run();
      var parsed = parser.parse(path, template, heartbeat);
      heartbeat.run();
      replaceParsedData(batchId, item.tenantId(), parsed);
      String finalStatus = parsed.errorRows() > 0 ? "INVALID" : "PENDING_CONFIRMATION";
      jdbc.update("""
          update mes.import_batch set status=?,total_rows=?,valid_rows=?,error_rows=?,warning_rows=?,updated_at=now(),version=version+1
          where id=?
          """, finalStatus, parsed.totalRows(), parsed.validRows(), parsed.errorRows(), parsed.warningRows(), batchId);
      syncCalendar(item.tenantId(), template.id(), batchId, finalStatus);
      audits.record(item.tenantKey(), null, "system", "MES_REPORT_VALIDATED", "MES_IMPORT_BATCH",
          batchId.toString(), parsed.errorRows() > 0 ? "FAILED" : "SUCCESS",
          json(Map.of("totalRows", parsed.totalRows(), "validRows", parsed.validRows(),
              "errorRows", parsed.errorRows(), "warningRows", parsed.warningRows(), "templateCode", template.code())));
      return batchById(batchId, item.tenantId());
    } catch (Exception error) {
      replaceIssues(batchId, item.tenantId(), List.of(new MesWorkbookParser.Issue(
          "ERROR", "WORKBOOK_READ_FAILED", null, null, null, null, null,
          "Không đọc được workbook: " + safeMessage(error))));
      jdbc.update("""
          update mes.import_batch set status='INVALID',total_rows=0,valid_rows=0,error_rows=1,warning_rows=0,updated_at=now()
          where id=?
          """, batchId);
      syncCalendarFromBatch(batchId, "INVALID");
      audits.record(item.tenantKey(), null, "system", "MES_REPORT_PARSE_FAILED", "MES_IMPORT_BATCH",
          batchId.toString(), "FAILED", json(Map.of("error", safeMessage(error))));
      return batchById(batchId, item.tenantId());
    }
  }

  public BatchView batch(Authentication authentication, UUID id) {
    return batchById(id, permissions.tenant(authentication));
  }

  public PageResult<BatchView> recentBatches(Authentication authentication, int page, int size) {
    UUID tenant = permissions.tenant(authentication);
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(100, size));
    long total = jdbc.queryForObject("select count(*) from mes.import_batch where tenant_id=?", Long.class, tenant);
    var items = jdbc.query(batchSql() + " where b.tenant_id=? order by b.created_at desc limit ? offset ?",
        (row, index) -> batchView(row), tenant, safeSize, safePage * safeSize);
    return new PageResult<>(items, safePage, safeSize, total);
  }

  public PageResult<ImportRecordView> records(Authentication authentication, UUID batchId, int page, int size) {
    UUID tenant = permissions.tenant(authentication);
    requireBatch(batchId, tenant);
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(100, size));
    long total = jdbc.queryForObject("select count(*) from mes.import_record where tenant_id=? and batch_id=?", Long.class, tenant, batchId);
    var items = jdbc.query("""
        select id,source_sheet,source_row,record_status,payload::text from mes.import_record
        where tenant_id=? and batch_id=? order by source_row limit ? offset ?
        """, (row, index) -> new ImportRecordView(row.getObject(1, UUID.class), row.getString(2), row.getInt(3),
        row.getString(4), readTree(row.getString(5))), tenant, batchId, safeSize, safePage * safeSize);
    return new PageResult<>(items, safePage, safeSize, total);
  }

  public List<IssueView> issues(Authentication authentication, UUID batchId) {
    UUID tenant = permissions.tenant(authentication);
    requireBatch(batchId, tenant);
    return jdbc.query("""
        select id,severity,issue_code,source_sheet,source_row,source_cell,field_name,raw_value,message
        from mes.import_issue where tenant_id=? and batch_id=?
        order by case severity when 'ERROR' then 0 else 1 end,source_row nulls first,id
        """, (row, index) -> new IssueView(row.getObject(1, UUID.class), row.getString(2), row.getString(3),
        row.getString(4), (Integer) row.getObject(5), row.getString(6), row.getString(7), row.getString(8), row.getString(9)),
        tenant, batchId);
  }

  @Transactional
  public BatchView confirm(Authentication authentication, UUID batchId) {
    return transition(authentication, batchId, Set.of("PENDING_CONFIRMATION"), "PENDING_APPROVAL",
        "confirmed_by", "confirmed_at", "MES_REPORT_CONFIRMED");
  }

  @Transactional
  public BatchView approve(Authentication authentication, UUID batchId) {
    var result = transition(authentication, batchId, Set.of("PENDING_APPROVAL"), "APPROVED",
        "approved_by", "approved_at", "MES_REPORT_APPROVED");
    var payload = json.createObjectNode().put("batchId", batchId.toString()).put("reportingDate", result.reportingDate().toString());
    outbox.publish(permissions.tenantKey(authentication), "mes.report.approved.v1", "mes-import-batch", batchId.toString(), payload);
    return result;
  }

  @Transactional
  public BatchView lock(Authentication authentication, UUID batchId) {
    var result = transition(authentication, batchId, Set.of("APPROVED"), "LOCKED",
        null, "locked_at", "MES_REPORT_LOCKED");
    canonicalData.publishLockedBatch(authentication, batchId);
    return result;
  }

  @Transactional
  public BatchView reject(Authentication authentication, UUID batchId, String reason) {
    if (reason == null || reason.trim().length() < 3)
      throw new ApiProblem(HttpStatus.BAD_REQUEST, "MES_REJECTION_REASON", "Lý do từ chối phải có ít nhất 3 ký tự");
    UUID tenant = permissions.tenant(authentication);
    UUID actor = permissions.account(authentication);
    int changed = jdbc.update("""
        update mes.import_batch set status='REJECTED',rejected_reason=?,updated_at=now(),version=version+1
        where id=? and tenant_id=? and status='PENDING_APPROVAL'
        """, reason.trim(), batchId, tenant);
    if (changed == 0) transitionConflict(batchId, tenant, Set.of("PENDING_APPROVAL"));
    syncCalendarFromBatch(batchId, "INVALID");
    audits.record(permissions.tenantKey(authentication), actor, authentication.getName(), "MES_REPORT_REJECTED",
        "MES_IMPORT_BATCH", batchId.toString(), "SUCCESS", json(Map.of("reason", reason.trim())));
    return batchById(batchId, tenant);
  }

  @Transactional
  public PortalSubmissionView submitPortal(
      Authentication authentication, UUID batchId, String receiptCode, String note) {
    if (!portal.manualTrackingEnabled()) {
      throw new ApiProblem(HttpStatus.CONFLICT, "MES_PORTAL_INTEGRATION_DISABLED",
          "Tích hợp Portal đang tạm dừng; không phát sinh lần gửi hoặc trạng thái Portal");
    }
    UUID tenant = permissions.tenant(authentication);
    UUID actor = permissions.account(authentication);
    var rows = jdbc.query("""
        select c.id from mes.reporting_calendar_item c join mes.import_batch b on b.id=c.current_batch_id
        where c.tenant_id=? and b.id=? and b.status='LOCKED'
        """, (row, index) -> row.getObject(1, UUID.class), tenant, batchId);
    if (rows.isEmpty()) throw new ApiProblem(HttpStatus.CONFLICT, "MES_REPORT_NOT_LOCKED", "Chỉ báo cáo đã khóa mới được ghi nhận nộp Portal");
    UUID calendarId = rows.getFirst();
    advisoryTransactionLock("mes:portal-attempt:" + tenant + ":" + calendarId);
    int attempt = Optional.ofNullable(jdbc.queryForObject("""
        select max(attempt_no) from mes.portal_submission where tenant_id=? and calendar_item_id=?
        """, Integer.class, tenant, calendarId)).orElse(0) + 1;
    UUID id = UUID.randomUUID();
    jdbc.update("""
        insert into mes.portal_submission(id,tenant_id,calendar_item_id,batch_id,attempt_no,status,receipt_code,note,submitted_by)
        values(?,?,?,?,?,'SUBMITTED',?,?,?)
        """, id, tenant, calendarId, batchId, attempt, blankToNull(receiptCode), blankToNull(note), actor);
    jdbc.update("update mes.reporting_calendar_item set status='PORTAL_SUBMITTED',updated_at=now() where id=? and tenant_id=?", calendarId, tenant);
    audits.record(permissions.tenantKey(authentication), actor, authentication.getName(), "MES_REPORT_PORTAL_SUBMITTED",
        "MES_IMPORT_BATCH", batchId.toString(), "SUCCESS", json(Map.of("attempt", attempt)));
    return portalSubmission(id, tenant);
  }

  private BatchView transition(
      Authentication authentication, UUID batchId, Set<String> allowed, String next,
      String actorColumn, String timeColumn, String auditAction) {
    UUID tenant = permissions.tenant(authentication);
    UUID actor = permissions.account(authentication);
    var assignments = new ArrayList<String>();
    var parameters = new ArrayList<Object>();
    assignments.add("status=?"); parameters.add(next);
    if (actorColumn != null) { assignments.add(actorColumn + "=?"); parameters.add(actor); }
    if (timeColumn != null) assignments.add(timeColumn + "=now()");
    assignments.add("updated_at=now()");
    assignments.add("version=version+1");
    parameters.add(batchId); parameters.add(tenant); parameters.addAll(allowed);
    String placeholders = String.join(",", allowed.stream().map(value -> "?").toList());
    int changed = jdbc.update("update mes.import_batch set " + String.join(",", assignments)
        + " where id=? and tenant_id=? and status in (" + placeholders + ")", parameters.toArray());
    if (changed == 0) transitionConflict(batchId, tenant, allowed);
    syncCalendarFromBatch(batchId, next);
    audits.record(permissions.tenantKey(authentication), actor, authentication.getName(), auditAction,
        "MES_IMPORT_BATCH", batchId.toString(), "SUCCESS", json(Map.of("status", next)));
    return batchById(batchId, tenant);
  }

  private void transitionConflict(UUID batchId, UUID tenant, Set<String> allowed) {
    var current = jdbc.query("select status from mes.import_batch where id=? and tenant_id=?",
        (row, index) -> row.getString(1), batchId, tenant);
    if (current.isEmpty()) throw new ApiProblem(HttpStatus.NOT_FOUND, "MES_IMPORT_NOT_FOUND", "Không tìm thấy lô import");
    throw new ApiProblem(HttpStatus.CONFLICT, "MES_INVALID_STATE",
        "Trạng thái hiện tại " + current.getFirst() + "; yêu cầu một trong " + allowed);
  }

  private void ensurePilotTemplates(UUID tenant) {
    for (var template : PILOT_TEMPLATES) {
      String fieldContract = json(template.fields());
      jdbc.update("""
          insert into mes.template_definition(tenant_id,template_code,name,department,frequency,filename_pattern,
            worksheet_name,parser_type,reporting_entity_code,header_tokens,field_contract,contract_hash,
            version,due_time,active,effective_from,lifecycle_status)
          values(?,?,?,?,'DAILY',?,?,?,?,?::jsonb,?::jsonb,
            encode(digest((?::jsonb)::text,'sha256'),'hex'),1,?,true,date '2026-09-08','ACTIVE')
          on conflict(tenant_id,template_code,version) do update set
            filename_pattern=excluded.filename_pattern,worksheet_name=excluded.worksheet_name,
            parser_type=excluded.parser_type,reporting_entity_code=excluded.reporting_entity_code,
            header_tokens=excluded.header_tokens,field_contract=excluded.field_contract,
            contract_hash=excluded.contract_hash,effective_from=excluded.effective_from,
            lifecycle_status=excluded.lifecycle_status,updated_at=now()
          where mes.template_definition.field_contract='[]'::jsonb
          """, tenant, template.code(), template.name(), template.department(), template.filenamePattern(),
          template.sheet(), template.parserType(), "4100", json(template.tokens()), fieldContract, fieldContract,
          template.dueTime());
    }
  }

  private void ensureCalendar(UUID tenant, LocalDate date) {
    var templates = jdbc.query("""
        select id,due_time from mes.template_definition where tenant_id=? and active=true and frequency='DAILY'
        """, (row, index) -> Map.entry(row.getObject(1, UUID.class), row.getTime(2).toLocalTime()), tenant);
    for (var template : templates) {
      Instant dueAt = date.atTime(template.getValue()).atZone(BUSINESS_ZONE).toInstant();
      jdbc.update("""
          insert into mes.reporting_calendar_item(tenant_id,template_id,reporting_date,period_key,due_at,status)
          values(?,?,?,?,?,'NOT_SUBMITTED') on conflict(tenant_id,template_id,reporting_date) do nothing
          """, tenant, template.getKey(), date, date.toString(), Timestamp.from(dueAt));
    }
    jdbc.update("""
        update mes.reporting_calendar_item set status='OVERDUE',updated_at=now()
        where tenant_id=? and reporting_date=? and status='NOT_SUBMITTED' and due_at<now()
        """, tenant, date);
  }

  /** Package-visible để bộ UAT xác nhận chính workbook vận hành với registry runtime. */
  List<MesWorkbookParser.TemplateSpec> templateSpecsForTenant(UUID tenant) {
    return jdbc.query("""
        select id,template_code,name,filename_pattern,worksheet_name,reporting_entity_code,header_tokens::text,
               parser_type,field_contract::text
        from mes.template_definition where tenant_id=? and active=true
        """, (row, index) -> new MesWorkbookParser.TemplateSpec(row.getObject(1, UUID.class), row.getString(2),
        row.getString(3), row.getString(4), row.getString(5), row.getString(6),
        readStringList(row.getString(7)), row.getString(8), readFields(row.getString(9))), tenant);
  }

  private void replaceParsedData(UUID batchId, UUID tenant, MesWorkbookParser.ParseResult parsed) {
    jdbc.update("delete from mes.import_issue where tenant_id=? and batch_id=?", tenant, batchId);
    jdbc.update("delete from mes.import_record where tenant_id=? and batch_id=?", tenant, batchId);
    var recordArgs = parsed.records().stream().map(record -> new Object[]{
        tenant, batchId, record.sourceSheet(), record.sourceRow(), record.status(), json(record.values())}).toList();
    if (!recordArgs.isEmpty()) jdbc.batchUpdate("""
        insert into mes.import_record(tenant_id,batch_id,source_sheet,source_row,record_status,payload)
        values(?,?,?,?,?,?::jsonb)
        """, recordArgs);
    replaceIssues(batchId, tenant, parsed.issues());
  }

  private void replaceIssues(UUID batchId, UUID tenant, List<MesWorkbookParser.Issue> issues) {
    jdbc.update("delete from mes.import_issue where tenant_id=? and batch_id=?", tenant, batchId);
    var args = issues.stream().map(issue -> new Object[]{tenant, batchId, issue.severity(), issue.code(), issue.sheet(),
        issue.row(), issue.cell(), issue.field(), issue.rawValue(), issue.message()}).toList();
    if (!args.isEmpty()) jdbc.batchUpdate("""
        insert into mes.import_issue(tenant_id,batch_id,severity,issue_code,source_sheet,source_row,source_cell,field_name,raw_value,message)
        values(?,?,?,?,?,?,?,?,?,?)
        """, args);
  }

  private void syncCalendar(UUID tenant, UUID templateId, UUID batchId, String status) {
    LocalDate reportDate = reportDate(batchId, tenant);
    jdbc.update("""
        update mes.reporting_calendar_item set status=?,updated_at=now()
        where tenant_id=? and template_id=? and reporting_date=? and current_batch_id=?
        """, calendarStatus(status), tenant, templateId, reportDate, batchId);
  }

  private Optional<String> bindCalendarForProcessing(UUID tenant, UUID templateId, UUID batchId) {
    LocalDate reportDate = reportDate(batchId, tenant);
    ensureCalendar(tenant, reportDate);
    int changed = jdbc.update("""
        update mes.reporting_calendar_item set current_batch_id=?,status='PROCESSING',updated_at=now()
        where tenant_id=? and template_id=? and reporting_date=?
          and status not in ('LOCKED','PORTAL_SUBMITTED','PORTAL_ACCEPTED','PORTAL_REJECTED')
        """, batchId, tenant, templateId, reportDate);
    if (changed > 0) return Optional.empty();
    var states = jdbc.query("""
        select status from mes.reporting_calendar_item
        where tenant_id=? and template_id=? and reporting_date=?
        """, (row, index) -> row.getString(1), tenant, templateId, reportDate);
    if (!states.isEmpty() && PROTECTED_CALENDAR_STATUSES.contains(states.getFirst()))
      return Optional.of(states.getFirst());
    throw new IllegalStateException("Không thể gắn lô import vào lịch báo cáo");
  }

  private void syncCalendarFromBatch(UUID batchId, String status) {
    jdbc.update("""
        update mes.reporting_calendar_item set status=?,updated_at=now()
        where current_batch_id=?
        """, calendarStatus(status), batchId);
  }

  private LocalDate reportDate(UUID batchId, UUID tenant) {
    return jdbc.queryForObject(
        "select reporting_date from mes.import_batch where id=? and tenant_id=?",
        LocalDate.class, batchId, tenant);
  }

  private List<SourceBatch> claimUploadedBatch(UUID batchId) {
    return jdbc.query("""
        with claimed as (
          update mes.import_batch set status='IDENTIFYING',updated_at=now(),version=version+1
          where id=? and status='UPLOADED'
          returning tenant_id,original_name,file_id,status
        )
        select c.tenant_id,c.original_name,c.file_id,c.status,f.storage_key,t.tenant_key
        from claimed c
        join files.file_object f on f.id=c.file_id and f.tenant_id=c.tenant_id
        join platform.tenant t on t.id=c.tenant_id
        """, (row, index) -> sourceBatch(row), batchId);
  }

  private List<SourceBatch> sourceBatch(UUID batchId) {
    return jdbc.query("""
        select b.tenant_id,b.original_name,b.file_id,b.status,f.storage_key,t.tenant_key
        from mes.import_batch b
        join files.file_object f on f.id=b.file_id and f.tenant_id=b.tenant_id
        join platform.tenant t on t.id=b.tenant_id where b.id=?
        """, (row, index) -> sourceBatch(row), batchId);
  }

  private SourceBatch sourceBatch(ResultSet row) throws SQLException {
    return new SourceBatch(row.getObject(1, UUID.class), row.getString(2), row.getObject(3, UUID.class),
        row.getString(4), row.getString(5), row.getString(6));
  }

  private void advisoryTransactionLock(String key) {
    jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?, 0))", key);
  }

  private String calendarStatus(String batchStatus) {
    return switch (batchStatus) {
      case "UPLOADED", "IDENTIFYING", "IDENTIFIED", "VALIDATING" -> "PROCESSING";
      case "UNKNOWN_TEMPLATE", "INVALID", "REJECTED" -> "INVALID";
      case "PENDING_CONFIRMATION", "PENDING_APPROVAL", "APPROVED", "LOCKED" -> batchStatus;
      case "PORTAL_SUBMITTED" -> "PORTAL_SUBMITTED";
      default -> "PROCESSING";
    };
  }

  private void requireBatch(UUID batchId, UUID tenant) {
    Integer found = jdbc.queryForObject("select count(*) from mes.import_batch where id=? and tenant_id=?", Integer.class, batchId, tenant);
    if (found == null || found == 0) throw new ApiProblem(HttpStatus.NOT_FOUND, "MES_IMPORT_NOT_FOUND", "Không tìm thấy lô import");
  }

  private BatchView batchById(UUID id, UUID tenant) {
    var rows = jdbc.query(batchSql() + " where b.id=? and b.tenant_id=?",
        (row, index) -> batchView(row), id, tenant);
    if (rows.isEmpty()) throw new ApiProblem(HttpStatus.NOT_FOUND, "MES_IMPORT_NOT_FOUND", "Không tìm thấy lô import");
    return rows.getFirst();
  }

  private String batchSql() {
    return """
        select b.id,t.template_code,t.name template_name,b.reporting_date,b.period_key,b.original_name,b.checksum_sha256,
               b.status,b.total_rows,b.valid_rows,b.error_rows,b.warning_rows,a.email submitted_by,b.duplicate_of,
               b.created_at,b.updated_at
        from mes.import_batch b
        left join mes.template_definition t on t.id=b.template_id and t.tenant_id=b.tenant_id
        join identity.account a on a.id=b.submitted_by and a.tenant_id=b.tenant_id
        """;
  }

  private BatchView batchView(ResultSet row) throws SQLException {
    return new BatchView(row.getObject("id", UUID.class), row.getString("template_code"), row.getString("template_name"),
        row.getObject("reporting_date", LocalDate.class), row.getString("period_key"), row.getString("original_name"),
        row.getString("checksum_sha256"), row.getString("status"), row.getInt("total_rows"), row.getInt("valid_rows"),
        row.getInt("error_rows"), row.getInt("warning_rows"), row.getString("submitted_by"),
        row.getObject("duplicate_of", UUID.class), row.getTimestamp("created_at").toInstant(), row.getTimestamp("updated_at").toInstant());
  }

  private TemplateView templateView(ResultSet row) throws SQLException {
    return new TemplateView(row.getObject(1, UUID.class), row.getString(2), row.getString(3), row.getString(4),
        row.getString(5), row.getString(6), row.getString(7), row.getString(8), row.getInt(9),
        row.getTime(10).toLocalTime(), row.getBoolean(11), readTree(row.getString(12)), row.getString(13),
        row.getObject(14, LocalDate.class), row.getString(15));
  }

  private CalendarItemView calendarItem(ResultSet row) throws SQLException {
    Timestamp updated = row.getTimestamp("updated_at");
    return new CalendarItemView(row.getObject("id", UUID.class), row.getString("template_code"), row.getString("name"),
        row.getString("department"), row.getObject("reporting_date", LocalDate.class), row.getTimestamp("due_at").toInstant(),
        row.getString("status"), row.getObject("batch_id", UUID.class), row.getString("original_name"),
        row.getInt("total_rows"), row.getInt("valid_rows"), row.getInt("error_rows"), row.getInt("warning_rows"),
        updated == null ? null : updated.toInstant());
  }

  private PortalSubmissionView portalSubmission(UUID id, UUID tenant) {
    return jdbc.queryForObject("""
        select p.id,p.calendar_item_id,p.batch_id,p.attempt_no,p.status,p.receipt_code,p.note,a.email,p.submitted_at
        from mes.portal_submission p join identity.account a on a.id=p.submitted_by and a.tenant_id=p.tenant_id
        where p.id=? and p.tenant_id=?
        """, (row, index) -> new PortalSubmissionView(row.getObject(1, UUID.class), row.getObject(2, UUID.class),
        row.getObject(3, UUID.class), row.getInt(4), row.getString(5), row.getString(6), row.getString(7),
        row.getString(8), row.getTimestamp(9).toInstant()), id, tenant);
  }

  private List<String> readStringList(String value) {
    try {
      var result = new ArrayList<String>();
      json.readTree(value).forEach(node -> result.add(node.asText()));
      return result;
    } catch (Exception error) {
      throw new IllegalStateException("header_tokens không hợp lệ", error);
    }
  }

  private List<MesWorkbookParser.FieldSpec> readFields(String value) {
    try {
      return json.readerForListOf(MesWorkbookParser.FieldSpec.class).readValue(value);
    } catch (Exception error) {
      throw new IllegalStateException("field_contract không hợp lệ", error);
    }
  }

  private JsonNode readTree(String value) {
    try { return json.readTree(value); }
    catch (Exception error) { throw new IllegalStateException("JSON staging không hợp lệ", error); }
  }

  private String json(Object value) {
    try { return json.writeValueAsString(value); }
    catch (Exception error) { throw new IllegalStateException("Không serialize được dữ liệu MES", error); }
  }

  private String sanitize(String value) {
    String name = Optional.ofNullable(value).orElse("report.xlsx").replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\u0000]", "_");
    return name.length() <= 255 ? name : name.substring(name.length() - 255);
  }

  private String safeMessage(Exception error) {
    String value = Optional.ofNullable(error.getMessage()).orElse(error.getClass().getSimpleName());
    return value.length() <= 300 ? value : value.substring(0, 300);
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private record SourceBatch(UUID tenantId, String originalName, UUID fileId, String status, String storageKey, String tenantKey) {}
}
