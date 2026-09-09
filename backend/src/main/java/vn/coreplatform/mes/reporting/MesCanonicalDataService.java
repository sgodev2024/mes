package vn.coreplatform.mes.reporting;

import static vn.coreplatform.shared.ApiExceptionHandler.ApiProblem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.coreplatform.audit.AuditService;
import vn.coreplatform.eventing.OutboxService;
import vn.coreplatform.permission.PermissionService;

/**
 * Projector và read model cho nguồn sự thật nội bộ MES.
 *
 * <p>{@code import_record.payload} luôn là staging có nhãn Excel. Service này ánh xạ
 * nhãn theo field contract sang mã field ổn định, chuyển measure sang DECIMAL và tạo
 * một release bất biến. Dashboard/Portal tương lai chỉ được đọc release này.
 */
@Service
public class MesCanonicalDataService {
  private static final int MAX_RANGE_DAYS = 366;

  private final JdbcTemplate jdbc;
  private final PermissionService permissions;
  private final AuditService audits;
  private final OutboxService outbox;
  private final ObjectMapper json;

  public MesCanonicalDataService(
      JdbcTemplate jdbc,
      PermissionService permissions,
      AuditService audits,
      OutboxService outbox,
      ObjectMapper json) {
    this.jdbc = jdbc;
    this.permissions = permissions;
    this.audits = audits;
    this.outbox = outbox;
    this.json = json;
  }

  public record ReleaseView(
      UUID id, UUID batchId, String templateCode, LocalDate reportingDate, String domain,
      int sourceRows, int metricPoints, int contractVersion, String contractHash,
      String status, Instant publishedAt) {}

  public record InternalDataSummary(
      long releases,
      long sourceRows,
      long metricPoints,
      long distinctProducts,
      long distinctPartners,
      long unresolvedReferences,
      LocalDate latestReportingDate,
      Map<String, BigDecimal> metricTotals) {}

  public record InternalMetricView(
      UUID id,
      LocalDate reportingDate,
      String domain,
      String templateCode,
      String metricCode,
      String metricLabel,
      BigDecimal value,
      String unitCode,
      String productCode,
      String productName,
      String partnerType,
      String partnerCode,
      String partnerName,
      String qualityStatus,
      UUID batchId,
      String sourceSheet,
      int sourceRow,
      int contractVersion,
      String contractHash,
      Instant publishedAt) {}

  public record PageResult<T>(List<T> items, int page, int size, long total) {}

  private record BatchSource(
      UUID tenantId,
      UUID templateId,
      String tenantKey,
      String status,
      LocalDate reportingDate,
      String templateCode,
      int contractVersion,
      String contractHash,
      JsonNode fieldContract) {}

  private record SourceRecord(
      UUID id, String sheet, int row, String recordStatus, JsonNode payload) {}

  private record Field(String code, String label, String dataType) {}

  private record MetricCandidate(
      SourceRecord source,
      String code,
      String label,
      BigDecimal value,
      String unitCode,
      String qualityStatus,
      UUID productId,
      UUID partnerId) {}

  /**
   * Phải được gọi bên trong transaction khóa lô. Projection lỗi sẽ rollback cả
   * trạng thái LOCKED, audit và outbox; không tồn tại lô đã khóa nhưng thiếu facts.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public ReleaseView publishLockedBatch(Authentication authentication, UUID batchId) {
    UUID tenant = permissions.tenant(authentication);
    UUID actor = permissions.account(authentication);
    jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?, 0))",
        "mes:internal-publish:" + tenant + ":" + batchId);

    var existing = releaseByBatch(tenant, batchId);
    if (!existing.isEmpty()) return existing.getFirst();

    BatchSource batch = batchSource(tenant, batchId);
    if (!"LOCKED".equals(batch.status())) {
      throw new ApiProblem(HttpStatus.CONFLICT, "MES_REPORT_NOT_LOCKED",
          "Chỉ lô đã khóa mới được phát hành làm dữ liệu nội bộ");
    }

    List<Field> fields = fields(batch.fieldContract());
    Map<String, Field> numericFields = new LinkedHashMap<>();
    fields.stream().filter(field -> "NUMBER".equalsIgnoreCase(field.dataType()))
        .forEach(field -> numericFields.put(field.code(), field));
    if (numericFields.isEmpty()) {
      throw new ApiProblem(HttpStatus.CONFLICT, "MES_CANONICAL_MAPPING_EMPTY",
          "Data Contract chưa có measure số để phát hành nội bộ");
    }

    List<SourceRecord> sourceRecords = jdbc.query("""
        select id,source_sheet,source_row,record_status,payload::text
        from mes.import_record
        where tenant_id=? and batch_id=? and record_status in ('VALID','WARNING')
        order by source_sheet,source_row,id
        """, (row, index) -> new SourceRecord(
        row.getObject(1, UUID.class), row.getString(2), row.getInt(3), row.getString(4),
        readTree(row.getString(5))), tenant, batchId);
    if (sourceRecords.isEmpty()) {
      throw new ApiProblem(HttpStatus.CONFLICT, "MES_CANONICAL_SOURCE_EMPTY",
          "Lô không có dòng hợp lệ để phát hành nội bộ");
    }

    String domain = domain(batch.templateCode());
    List<MetricCandidate> candidates = new ArrayList<>();
    for (SourceRecord source : sourceRecords) {
      Map<String, String> stableValues = stableValues(source.payload(), fields);
      UUID productId = product(tenant, stableValues);
      UUID partnerId = partner(tenant, batch.templateCode(), stableValues);
      boolean unresolved = unresolvedDimension(tenant, productId, partnerId);
      for (Field field : numericFields.values()) {
        String rawValue = stableValues.get(field.code());
        if (rawValue == null || rawValue.isBlank()) continue;
        BigDecimal value = decimal(rawValue, field.code());
        String quality = unresolved || "WARNING".equals(source.recordStatus()) ? "UNVERIFIED" : "VALID";
        candidates.add(new MetricCandidate(source, field.code(), field.label(), value,
            unit(field.code(), field.label(), stableValues.get("unit")), quality, productId, partnerId));
      }
    }
    if (candidates.isEmpty()) {
      throw new ApiProblem(HttpStatus.CONFLICT, "MES_CANONICAL_METRICS_EMPTY",
          "Lô không có giá trị số hợp lệ để phát hành nội bộ");
    }

    UUID releaseId = UUID.randomUUID();
    Instant publishedAt = Instant.now();
    jdbc.update("""
        insert into mes.internal_dataset_release(
          id,tenant_id,batch_id,template_id,reporting_date,domain,template_code,
          contract_version,contract_hash,status,source_rows,metric_points,published_by,published_at)
        values(?,?,?,?,?,?,?,?,?,'PUBLISHED',?,?,?,?)
        """, releaseId, tenant, batchId, batch.templateId(), batch.reportingDate(), domain,
        batch.templateCode(), batch.contractVersion(), batch.contractHash(), sourceRecords.size(),
        candidates.size(), actor, java.sql.Timestamp.from(publishedAt));

    List<Object[]> metricArgs = candidates.stream().map(metric -> new Object[]{
        UUID.randomUUID(), tenant, releaseId, batchId, metric.source().id(), batch.reportingDate(),
        domain, batch.templateCode(), metric.code(), metric.label(), metric.value(), metric.unitCode(),
        metric.productId(), metric.partnerId(), metric.qualityStatus(), metric.source().sheet(),
        metric.source().row(), batch.contractVersion(), batch.contractHash(),
        java.sql.Timestamp.from(publishedAt)}).toList();
    jdbc.batchUpdate("""
        insert into mes.canonical_daily_metric(
          id,tenant_id,release_id,batch_id,source_record_id,reporting_date,domain,template_code,
          metric_code,metric_label,metric_value,unit_code,product_id,partner_id,quality_status,
          source_sheet,source_row,contract_version,contract_hash,published_at)
        values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """, metricArgs);

    var event = json.createObjectNode()
        .put("releaseId", releaseId.toString())
        .put("batchId", batchId.toString())
        .put("templateCode", batch.templateCode())
        .put("reportingDate", batch.reportingDate().toString())
        .put("contractVersion", batch.contractVersion())
        .put("contractHash", batch.contractHash())
        .put("metricPoints", candidates.size());
    outbox.publish(batch.tenantKey(), "mes.report.internal-published.v1", "mes-internal-dataset",
        releaseId.toString(), event);
    audits.record(batch.tenantKey(), actor, authentication.getName(), "MES_INTERNAL_DATA_PUBLISHED",
        "MES_INTERNAL_DATASET", releaseId.toString(), "SUCCESS", write(event));
    return releaseByBatch(tenant, batchId).getFirst();
  }

  @Transactional(readOnly = true)
  public InternalDataSummary summary(Authentication authentication, LocalDate from, LocalDate to) {
    UUID tenant = permissions.tenant(authentication);
    DateRange range = range(from, to);
    Map<String, Object> summary = jdbc.queryForMap("""
        select count(*) releases,coalesce(sum(source_rows),0) source_rows,
          coalesce(sum(metric_points),0) metric_points,max(reporting_date) latest_reporting_date
        from mes.internal_dataset_release
        where tenant_id=? and status='PUBLISHED' and reporting_date between ? and ?
        """, tenant, range.from(), range.to());
    Map<String, Object> dimensions = jdbc.queryForMap("""
        select count(distinct m.product_id) filter(where m.product_id is not null) distinct_products,
          count(distinct m.partner_id) filter(where m.partner_id is not null) distinct_partners,
          count(*) filter(where (p.id is not null and p.resolution_status<>'RESOLVED')
            or (k.id is not null and k.resolution_status<>'RESOLVED')) unresolved_references
        from mes.canonical_daily_metric m
        left join mes.canonical_product p on p.id=m.product_id and p.tenant_id=m.tenant_id
        left join mes.canonical_partner k on k.id=m.partner_id and k.tenant_id=m.tenant_id
        where m.tenant_id=? and m.reporting_date between ? and ?
        """, tenant, range.from(), range.to());
    var totals = new LinkedHashMap<String, BigDecimal>();
    var totalRows = jdbc.query("""
        select metric_code,sum(metric_value) total from mes.canonical_daily_metric
        where tenant_id=? and reporting_date between ? and ?
        group by metric_code order by metric_code
        """, (row, index) -> Map.entry(row.getString(1), row.getBigDecimal(2)),
        tenant, range.from(), range.to());
    totalRows.forEach(row -> totals.put(row.getKey(), row.getValue()));
    return new InternalDataSummary(number(summary, "releases"), number(summary, "source_rows"),
        number(summary, "metric_points"), number(dimensions, "distinct_products"),
        number(dimensions, "distinct_partners"), number(dimensions, "unresolved_references"),
        localDate(summary.get("latest_reporting_date")), Map.copyOf(totals));
  }

  @Transactional(readOnly = true)
  public PageResult<InternalMetricView> metrics(
      Authentication authentication, LocalDate from, LocalDate to, String domain, int page, int size) {
    UUID tenant = permissions.tenant(authentication);
    DateRange range = range(from, to);
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(100, size));
    String safeDomain = Optional.ofNullable(domain).orElse("").trim().toUpperCase(Locale.ROOT);
    long total = jdbc.queryForObject("""
        select count(*) from mes.canonical_daily_metric
        where tenant_id=? and reporting_date between ? and ? and (?='' or domain=?)
        """, Long.class, tenant, range.from(), range.to(), safeDomain, safeDomain);
    var items = jdbc.query("""
        select m.id,m.reporting_date,m.domain,m.template_code,m.metric_code,m.metric_label,
          m.metric_value,m.unit_code,p.product_code,p.product_name,k.partner_type,k.partner_code,
          k.partner_name,m.quality_status,m.batch_id,m.source_sheet,m.source_row,
          m.contract_version,m.contract_hash,m.published_at
        from mes.canonical_daily_metric m
        left join mes.canonical_product p on p.id=m.product_id and p.tenant_id=m.tenant_id
        left join mes.canonical_partner k on k.id=m.partner_id and k.tenant_id=m.tenant_id
        where m.tenant_id=? and m.reporting_date between ? and ? and (?='' or m.domain=?)
        order by m.reporting_date desc,m.published_at desc,m.source_row,m.metric_code,m.id
        limit ? offset ?
        """, (row, index) -> metricView(row), tenant, range.from(), range.to(), safeDomain,
        safeDomain, safeSize, safePage * safeSize);
    return new PageResult<>(items, safePage, safeSize, total);
  }

  private BatchSource batchSource(UUID tenant, UUID batchId) {
    var rows = jdbc.query("""
        select b.tenant_id,b.template_id,tenant.tenant_key,b.status,b.reporting_date,
          t.template_code,t.version,t.contract_hash,t.field_contract::text
        from mes.import_batch b
        join mes.template_definition t on t.id=b.template_id and t.tenant_id=b.tenant_id
        join platform.tenant tenant on tenant.id=b.tenant_id
        where b.tenant_id=? and b.id=?
        """, (row, index) -> new BatchSource(row.getObject(1, UUID.class),
        row.getObject(2, UUID.class), row.getString(3), row.getString(4),
        row.getObject(5, LocalDate.class), row.getString(6), row.getInt(7), row.getString(8),
        readTree(row.getString(9))), tenant, batchId);
    if (rows.isEmpty()) throw new ApiProblem(HttpStatus.NOT_FOUND, "MES_IMPORT_NOT_FOUND", "Không tìm thấy lô import");
    return rows.getFirst();
  }

  private List<ReleaseView> releaseByBatch(UUID tenant, UUID batchId) {
    return jdbc.query("""
        select id,batch_id,template_code,reporting_date,domain,source_rows,metric_points,
          contract_version,contract_hash,status,published_at
        from mes.internal_dataset_release where tenant_id=? and batch_id=?
        """, (row, index) -> new ReleaseView(row.getObject(1, UUID.class), row.getObject(2, UUID.class),
        row.getString(3), row.getObject(4, LocalDate.class), row.getString(5), row.getInt(6),
        row.getInt(7), row.getInt(8), row.getString(9), row.getString(10),
        row.getTimestamp(11).toInstant()), tenant, batchId);
  }

  private List<Field> fields(JsonNode contract) {
    var result = new ArrayList<Field>();
    if (contract != null && contract.isArray()) contract.forEach(field -> result.add(new Field(
        field.path("code").asText(), field.path("label").asText(), field.path("dataType").asText())));
    return result;
  }

  private Map<String, String> stableValues(JsonNode payload, List<Field> fields) {
    var result = new LinkedHashMap<String, String>();
    for (Field field : fields) {
      String label = normalize(field.label());
      payload.fields().forEachRemaining(entry -> {
        if (!result.containsKey(field.code()) && normalize(entry.getKey()).contains(label))
          result.put(field.code(), entry.getValue().asText());
      });
    }
    return result;
  }

  private UUID product(UUID tenant, Map<String, String> values) {
    String code = clean(values.get("product_code"));
    String name = clean(values.get("product_name"));
    if (code == null && name == null) return null;
    String key = code == null ? "NAME:" + name.toUpperCase(Locale.ROOT) : code;
    jdbc.update("""
        insert into mes.canonical_product(tenant_id,product_code,product_name,unit_code,resolution_status)
        values(?,?,?,?, 'UNVERIFIED') on conflict(tenant_id,product_code) do nothing
        """, tenant, key, name == null ? key : name, normalizeUnit(values.get("unit")));
    return jdbc.queryForObject(
        "select id from mes.canonical_product where tenant_id=? and product_code=?", UUID.class, tenant, key);
  }

  private UUID partner(UUID tenant, String templateCode, Map<String, String> values) {
    String type = templateCode.contains("RECEIPT") ? "SUPPLIER" : templateCode.contains("ISSUE") ? "CUSTOMER" : null;
    if (type == null) return null;
    String prefix = "SUPPLIER".equals(type) ? "supplier" : "customer";
    String code = clean(values.get(prefix + "_code"));
    String name = clean(values.get(prefix + "_name"));
    if (code == null && name == null) return null;
    String key = code == null ? "NAME:" + name.toUpperCase(Locale.ROOT) : code;
    jdbc.update("""
        insert into mes.canonical_partner(tenant_id,partner_type,partner_key,partner_code,partner_name,resolution_status)
        values(?,?,?,?,?,'UNVERIFIED') on conflict(tenant_id,partner_type,partner_key) do nothing
        """, tenant, type, key, code, name);
    return jdbc.queryForObject("""
        select id from mes.canonical_partner where tenant_id=? and partner_type=? and partner_key=?
        """, UUID.class, tenant, type, key);
  }

  private boolean unresolvedDimension(UUID tenant, UUID productId, UUID partnerId) {
    if (productId != null) {
      String status = jdbc.queryForObject(
          "select resolution_status from mes.canonical_product where tenant_id=? and id=?",
          String.class, tenant, productId);
      if (!"RESOLVED".equals(status)) return true;
    }
    if (partnerId != null) {
      String status = jdbc.queryForObject(
          "select resolution_status from mes.canonical_partner where tenant_id=? and id=?",
          String.class, tenant, partnerId);
      if (!"RESOLVED".equals(status)) return true;
    }
    return false;
  }

  private String domain(String templateCode) {
    if (templateCode.contains("PRODUCTION")) return "PRODUCTION";
    if (templateCode.contains("RECEIPT")) return "RECEIPT";
    if (templateCode.contains("ISSUE")) return "ISSUE";
    if (templateCode.contains("STOCK")) return "INVENTORY";
    throw new ApiProblem(HttpStatus.CONFLICT, "MES_CANONICAL_DOMAIN_UNKNOWN",
        "Chưa có domain canonical cho template " + templateCode);
  }

  private String unit(String code, String label, String sourceUnit) {
    String normalized = normalize(label + " " + code);
    if (normalized.contains("percent") || label.contains("%") || normalized.contains("chat boc")) return "PERCENT";
    if ("processing_point".equals(code)) return "POINT";
    if ("calorific_value".equals(code)) return "KCAL_KG";
    return normalizeUnit(sourceUnit);
  }

  private String normalizeUnit(String value) {
    String normalized = normalize(Optional.ofNullable(value).orElse(""));
    if (normalized.isBlank() || normalized.equals("tan") || normalized.equals("t")) return "TONNE";
    if (normalized.equals("kg")) return "KILOGRAM";
    if (normalized.equals("m3")) return "CUBIC_METRE";
    return normalized.toUpperCase(Locale.ROOT).replace(' ', '_');
  }

  private BigDecimal decimal(String raw, String fieldCode) {
    String value = raw.trim().replace("\u00a0", "").replace(" ", "");
    if (value.endsWith("%")) value = value.substring(0, value.length() - 1);
    int comma = value.lastIndexOf(',');
    int dot = value.lastIndexOf('.');
    if (comma >= 0 && dot >= 0) {
      if (comma > dot) value = value.replace(".", "").replace(',', '.');
      else value = value.replace(",", "");
    } else if (comma >= 0) {
      value = value.replace(',', '.');
    }
    try {
      BigDecimal parsed = new BigDecimal(value);
      // DataFormatter trả phần trăm theo giá trị hiển thị (vd. 16.5%); không đổi scale lần nữa.
      return parsed.stripTrailingZeros();
    } catch (NumberFormatException error) {
      throw new ApiProblem(HttpStatus.CONFLICT, "MES_CANONICAL_NUMBER_INVALID",
          "Không chuẩn hóa được giá trị " + raw + " của trường " + fieldCode);
    }
  }

  private DateRange range(LocalDate from, LocalDate to) {
    LocalDate safeTo = Optional.ofNullable(to).orElse(LocalDate.now());
    LocalDate safeFrom = Optional.ofNullable(from).orElse(safeTo);
    if (safeFrom.isAfter(safeTo) || safeFrom.plusDays(MAX_RANGE_DAYS).isBefore(safeTo))
      throw new ApiProblem(HttpStatus.BAD_REQUEST, "MES_DATE_RANGE_INVALID",
          "Khoảng thời gian phải hợp lệ và không vượt quá " + MAX_RANGE_DAYS + " ngày");
    return new DateRange(safeFrom, safeTo);
  }

  private InternalMetricView metricView(ResultSet row) throws SQLException {
    return new InternalMetricView(row.getObject(1, UUID.class), row.getObject(2, LocalDate.class),
        row.getString(3), row.getString(4), row.getString(5), row.getString(6), row.getBigDecimal(7),
        row.getString(8), row.getString(9), row.getString(10), row.getString(11), row.getString(12),
        row.getString(13), row.getString(14), row.getObject(15, UUID.class), row.getString(16),
        row.getInt(17), row.getInt(18), row.getString(19), row.getTimestamp(20).toInstant());
  }

  private long number(Map<String, Object> values, String key) {
    Object value = values.get(key);
    return value instanceof Number number ? number.longValue() : 0;
  }

  private LocalDate localDate(Object value) {
    if (value == null) return null;
    if (value instanceof LocalDate date) return date;
    if (value instanceof java.sql.Date date) return date.toLocalDate();
    throw new IllegalStateException("Kiểu ngày canonical không được hỗ trợ: " + value.getClass().getName());
  }

  private JsonNode readTree(String value) {
    try { return json.readTree(value); }
    catch (Exception error) { throw new IllegalStateException("JSON staging không hợp lệ", error); }
  }

  private String write(JsonNode value) {
    try { return json.writeValueAsString(value); }
    catch (Exception error) { throw new IllegalStateException("Không serialize được dữ liệu canonical", error); }
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String normalize(String value) {
    if (value == null) return "";
    return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D')
        .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9%]+", " ").trim();
  }

  private record DateRange(LocalDate from, LocalDate to) {}
}
