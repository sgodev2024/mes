package vn.coreplatform.mes.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.coreplatform.permission.PermissionService;

/** Read model đối soát tồn kho chỉ dùng facts canonical đã phát hành nội bộ. */
@Service
public class MesInventoryReconciliationService {
  private static final int REQUIRED_DAILY_CONTRACTS = 5;
  private final JdbcTemplate jdbc;
  private final PermissionService permissions;

  public MesInventoryReconciliationService(JdbcTemplate jdbc, PermissionService permissions) {
    this.jdbc = jdbc;
    this.permissions = permissions;
  }

  public record InventoryItem(
      UUID productId, String productCode, String productName, String unitCode,
      String masterDataStatus, BigDecimal openingStock, BigDecimal receipts,
      BigDecimal issues, BigDecimal expectedClosingStock, BigDecimal reportedClosingStock,
      BigDecimal variance, String status, String reason) {}

  public record ReconciliationView(
      LocalDate reportingDate, LocalDate openingDate, int coveredContracts,
      int requiredContracts, boolean coverageComplete, long totalProducts,
      long balanced, long variances, long incomplete, long unverifiedMasterData,
      BigDecimal totalReceipts, BigDecimal totalIssues, BigDecimal totalVariance,
      List<InventoryItem> items, int page, int size) {}

  private record RawItem(
      UUID productId, String code, String name, String unit, String masterStatus,
      BigDecimal opening, BigDecimal receipts, BigDecimal issues, BigDecimal closing) {}

  @Transactional(readOnly = true)
  public ReconciliationView reconcile(
      Authentication authentication, LocalDate requestedDate, int page, int size) {
    UUID tenant = permissions.tenant(authentication);
    LocalDate reportingDate = requestedDate;
    if (reportingDate == null) reportingDate = jdbc.queryForObject(
        "select max(reporting_date) from mes.internal_dataset_release where tenant_id=?",
        LocalDate.class, tenant);
    if (reportingDate == null) reportingDate = LocalDate.now();
    LocalDate openingDate = reportingDate.minusDays(1);
    int coverage = jdbc.queryForObject("""
        select count(distinct template_code) from mes.internal_dataset_release
        where tenant_id=? and reporting_date=? and template_code in (
          'MES-DAY-RAW-COAL-RECEIPT','MES-DAY-CLEAN-COAL-RECEIPT','MES-DAY-STOCK',
          'MES-DAY-CLEAN-COAL-ISSUE','MES-DAY-RAW-COAL-ISSUE')
        """, Integer.class, tenant, reportingDate);
    boolean coverageComplete = coverage == REQUIRED_DAILY_CONTRACTS;

    List<RawItem> raw = jdbc.query("""
        with product_scope as (
          select distinct product_id from mes.canonical_daily_metric
          where tenant_id=? and product_id is not null and reporting_date in (?,?)
        )
        select p.id,p.product_code,p.product_name,p.unit_code,p.resolution_status,
          max(m.metric_value) filter(where m.reporting_date=? and m.template_code='MES-DAY-STOCK'
            and m.metric_code='closing_stock') opening_stock,
          coalesce(sum(m.metric_value) filter(where m.reporting_date=? and m.domain='RECEIPT'
            and m.metric_code in ('external_purchase','internal_purchase','mined_receipt','processing_receipt')),0) receipts,
          coalesce(sum(m.metric_value) filter(where m.reporting_date=? and m.domain='ISSUE'
            and m.metric_code in ('sale_issue','screening_issue','processing_issue')),0) issues,
          max(m.metric_value) filter(where m.reporting_date=? and m.template_code='MES-DAY-STOCK'
            and m.metric_code='closing_stock') closing_stock
        from product_scope scope
        join mes.canonical_product p on p.id=scope.product_id and p.tenant_id=?
        left join mes.canonical_daily_metric m on m.product_id=p.id and m.tenant_id=p.tenant_id
          and m.reporting_date in (?,?)
        group by p.id,p.product_code,p.product_name,p.unit_code,p.resolution_status
        order by p.product_code,p.id
        """, (row, index) -> new RawItem(row.getObject(1, UUID.class), row.getString(2),
        row.getString(3), row.getString(4), row.getString(5), row.getBigDecimal(6),
        row.getBigDecimal(7), row.getBigDecimal(8), row.getBigDecimal(9)),
        tenant, openingDate, reportingDate, openingDate, reportingDate, reportingDate,
        reportingDate, tenant, openingDate, reportingDate);

    List<InventoryItem> all = new ArrayList<>();
    for (RawItem item : raw) all.add(resolve(item, coverageComplete, coverage));
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(100, size));
    int from = Math.min(all.size(), safePage * safeSize);
    int to = Math.min(all.size(), from + safeSize);
    return new ReconciliationView(reportingDate, openingDate, coverage,
        REQUIRED_DAILY_CONTRACTS, coverageComplete, all.size(),
        count(all, "BALANCED"), count(all, "VARIANCE"), count(all, "INCOMPLETE"),
        count(all, "MASTER_UNVERIFIED"), total(all, Value.RECEIPTS),
        total(all, Value.ISSUES), total(all, Value.VARIANCE),
        List.copyOf(all.subList(from, to)), safePage, safeSize);
  }

  private InventoryItem resolve(RawItem item, boolean coverageComplete, int coverage) {
    BigDecimal expected = item.opening() == null ? null
        : item.opening().add(item.receipts()).subtract(item.issues());
    BigDecimal variance = expected == null || item.closing() == null ? null
        : item.closing().subtract(expected);
    String status;
    String reason;
    if (!"RESOLVED".equals(item.masterStatus())) {
      status = "MASTER_UNVERIFIED";
      reason = "Sản phẩm chưa được Data Steward xác nhận";
    } else if (!coverageComplete) {
      status = "INCOMPLETE";
      reason = "Mới có " + coverage + "/" + REQUIRED_DAILY_CONTRACTS + " hợp đồng dữ liệu bắt buộc trong ngày";
    } else if (item.opening() == null) {
      status = "INCOMPLETE";
      reason = "Thiếu tồn cuối ngày " + "trước";
    } else if (item.closing() == null) {
      status = "INCOMPLETE";
      reason = "Thiếu tồn cuối ngày đối soát";
    } else if (variance.compareTo(BigDecimal.ZERO) == 0) {
      status = "BALANCED";
      reason = "Tồn báo cáo khớp hoàn toàn với tồn tính toán";
    } else {
      status = "VARIANCE";
      reason = "Chênh lệch cần được kiểm tra và xử lý nghiệp vụ";
    }
    return new InventoryItem(item.productId(), item.code(), item.name(), item.unit(),
        item.masterStatus(), item.opening(), item.receipts(), item.issues(), expected,
        item.closing(), variance, status, reason);
  }

  private long count(List<InventoryItem> items, String status) {
    return items.stream().filter(item -> status.equals(item.status())).count();
  }

  private enum Value { RECEIPTS, ISSUES, VARIANCE }
  private BigDecimal total(List<InventoryItem> items, Value value) {
    return items.stream().map(item -> switch (value) {
      case RECEIPTS -> item.receipts();
      case ISSUES -> item.issues();
      case VARIANCE -> item.variance();
    }).filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
