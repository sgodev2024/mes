package vn.coreplatform.mes.reporting;

import static vn.coreplatform.shared.ApiExceptionHandler.ApiProblem;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.coreplatform.audit.AuditService;
import vn.coreplatform.permission.PermissionService;

/** Quy trình Data Steward xác nhận các chiều canonical sinh ra từ báo cáo MES. */
@Service
public class MesMasterDataService {
  private final JdbcTemplate jdbc;
  private final PermissionService permissions;
  private final AuditService audits;
  private final ObjectMapper json;

  public MesMasterDataService(
      JdbcTemplate jdbc, PermissionService permissions, AuditService audits, ObjectMapper json) {
    this.jdbc = jdbc;
    this.permissions = permissions;
    this.audits = audits;
    this.json = json;
  }

  public record PageResult<T>(List<T> items, int page, int size, long total) {}

  public record ProductView(
      UUID id, String code, String name, String unitCode, String resolutionStatus,
      int version, String resolvedBy, Instant resolvedAt, Instant updatedAt) {}

  public record PartnerView(
      UUID id, String type, String key, String code, String name, String resolutionStatus,
      int version, String resolvedBy, Instant resolvedAt, Instant updatedAt) {}

  public record ProductUpdate(
      String code, String name, String unitCode, String resolutionStatus, int expectedVersion) {}

  public record PartnerUpdate(
      String code, String name, String resolutionStatus, int expectedVersion) {}

  @Transactional(readOnly = true)
  public PageResult<ProductView> products(
      Authentication authentication, String status, String search, int page, int size) {
    UUID tenant = permissions.tenant(authentication);
    String safeStatus = status(status);
    String safeSearch = text(search).toLowerCase(Locale.ROOT);
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(100, size));
    long total = jdbc.queryForObject("""
        select count(*) from mes.canonical_product
        where tenant_id=? and (?='' or resolution_status=?)
          and (?='' or position(? in lower(product_code || ' ' || product_name || ' ' || unit_code)) > 0)
        """, Long.class, tenant, safeStatus, safeStatus, safeSearch, safeSearch);
    List<ProductView> items = jdbc.query("""
        select p.id,p.product_code,p.product_name,p.unit_code,p.resolution_status,p.version,
          a.display_name,p.resolved_at,p.updated_at
        from mes.canonical_product p
        left join identity.account a on a.id=p.resolved_by and a.tenant_id=p.tenant_id
        where p.tenant_id=? and (?='' or p.resolution_status=?)
          and (?='' or position(? in lower(p.product_code || ' ' || p.product_name || ' ' || p.unit_code)) > 0)
        order by case when p.resolution_status='UNVERIFIED' then 0 else 1 end,p.product_code,p.id
        limit ? offset ?
        """, (row, index) -> product(row), tenant, safeStatus, safeStatus, safeSearch, safeSearch,
        safeSize, safePage * safeSize);
    return new PageResult<>(items, safePage, safeSize, total);
  }

  @Transactional(readOnly = true)
  public PageResult<PartnerView> partners(
      Authentication authentication, String type, String status, String search, int page, int size) {
    UUID tenant = permissions.tenant(authentication);
    String safeType = partnerType(type);
    String safeStatus = status(status);
    String safeSearch = text(search).toLowerCase(Locale.ROOT);
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(100, size));
    Object[] args = {tenant, safeType, safeType, safeStatus, safeStatus, safeSearch, safeSearch};
    long total = jdbc.queryForObject("""
        select count(*) from mes.canonical_partner
        where tenant_id=? and (?='' or partner_type=?) and (?='' or resolution_status=?)
          and (?='' or position(? in lower(coalesce(partner_code,'') || ' ' || coalesce(partner_name,'') || ' ' || partner_key)) > 0)
        """, Long.class, args);
    List<PartnerView> items = jdbc.query("""
        select p.id,p.partner_type,p.partner_key,p.partner_code,p.partner_name,p.resolution_status,
          p.version,a.display_name,p.resolved_at,p.updated_at
        from mes.canonical_partner p
        left join identity.account a on a.id=p.resolved_by and a.tenant_id=p.tenant_id
        where p.tenant_id=? and (?='' or p.partner_type=?) and (?='' or p.resolution_status=?)
          and (?='' or position(? in lower(coalesce(p.partner_code,'') || ' ' || coalesce(p.partner_name,'') || ' ' || p.partner_key)) > 0)
        order by case when p.resolution_status='UNVERIFIED' then 0 else 1 end,p.partner_type,p.partner_key,p.id
        limit ? offset ?
        """, (row, index) -> partner(row), tenant, safeType, safeType, safeStatus, safeStatus,
        safeSearch, safeSearch, safeSize, safePage * safeSize);
    return new PageResult<>(items, safePage, safeSize, total);
  }

  @Transactional
  public ProductView updateProduct(Authentication authentication, UUID id, ProductUpdate update) {
    UUID tenant = permissions.tenant(authentication);
    UUID actor = permissions.account(authentication);
    String status = statusRequired(update.resolutionStatus());
    try {
      int changed = jdbc.update("""
          update mes.canonical_product set product_code=?,product_name=?,unit_code=?,resolution_status=?,
            resolved_by=case when ?='RESOLVED' then ? else null end,
            resolved_at=case when ?='RESOLVED' then now() else null end,
            version=version+1,updated_at=now()
          where tenant_id=? and id=? and version=?
          """, update.code().trim(), update.name().trim(), update.unitCode().trim().toUpperCase(Locale.ROOT),
          status, status, actor, status, tenant, id, update.expectedVersion());
      changed(changed, tenant, id, update.expectedVersion(), "Sản phẩm");
    } catch (DataIntegrityViolationException error) {
      throw new ApiProblem(HttpStatus.CONFLICT, "MES_MASTER_DATA_DUPLICATE",
          "Mã sản phẩm đã tồn tại hoặc dữ liệu xác nhận không hợp lệ");
    }
    audit(authentication, "MES_PRODUCT_MASTER_UPDATED", "MES_CANONICAL_PRODUCT", id, status);
    return findProduct(tenant, id);
  }

  @Transactional
  public PartnerView updatePartner(Authentication authentication, UUID id, PartnerUpdate update) {
    UUID tenant = permissions.tenant(authentication);
    UUID actor = permissions.account(authentication);
    String status = statusRequired(update.resolutionStatus());
    int changed = jdbc.update("""
        update mes.canonical_partner set partner_code=?,partner_name=?,resolution_status=?,
          resolved_by=case when ?='RESOLVED' then ? else null end,
          resolved_at=case when ?='RESOLVED' then now() else null end,
          version=version+1,updated_at=now()
        where tenant_id=? and id=? and version=?
        """, blankToNull(update.code()), update.name().trim(), status, status, actor, status,
        tenant, id, update.expectedVersion());
    changed(changed, tenant, id, update.expectedVersion(), "Đối tác");
    audit(authentication, "MES_PARTNER_MASTER_UPDATED", "MES_CANONICAL_PARTNER", id, status);
    return findPartner(tenant, id);
  }

  private ProductView findProduct(UUID tenant, UUID id) {
    var rows = jdbc.query("""
        select p.id,p.product_code,p.product_name,p.unit_code,p.resolution_status,p.version,
          a.display_name,p.resolved_at,p.updated_at
        from mes.canonical_product p left join identity.account a
          on a.id=p.resolved_by and a.tenant_id=p.tenant_id
        where p.tenant_id=? and p.id=?
        """, (row, index) -> product(row), tenant, id);
    if (rows.isEmpty()) throw new ApiProblem(HttpStatus.NOT_FOUND, "MES_MASTER_DATA_NOT_FOUND", "Không tìm thấy sản phẩm");
    return rows.getFirst();
  }

  private PartnerView findPartner(UUID tenant, UUID id) {
    var rows = jdbc.query("""
        select p.id,p.partner_type,p.partner_key,p.partner_code,p.partner_name,p.resolution_status,
          p.version,a.display_name,p.resolved_at,p.updated_at
        from mes.canonical_partner p left join identity.account a
          on a.id=p.resolved_by and a.tenant_id=p.tenant_id
        where p.tenant_id=? and p.id=?
        """, (row, index) -> partner(row), tenant, id);
    if (rows.isEmpty()) throw new ApiProblem(HttpStatus.NOT_FOUND, "MES_MASTER_DATA_NOT_FOUND", "Không tìm thấy đối tác");
    return rows.getFirst();
  }

  private void changed(int changed, UUID tenant, UUID id, int expectedVersion, String label) {
    if (changed > 0) return;
    Integer exists = jdbc.queryForObject("select count(*) from mes.canonical_product where tenant_id=? and id=?",
        Integer.class, tenant, id);
    if ("Đối tác".equals(label)) exists = jdbc.queryForObject(
        "select count(*) from mes.canonical_partner where tenant_id=? and id=?", Integer.class, tenant, id);
    if (exists == null || exists == 0)
      throw new ApiProblem(HttpStatus.NOT_FOUND, "MES_MASTER_DATA_NOT_FOUND", "Không tìm thấy " + label.toLowerCase(Locale.ROOT));
    throw new ApiProblem(HttpStatus.CONFLICT, "MES_MASTER_DATA_VERSION_CONFLICT",
        label + " đã được người khác cập nhật; phiên bản " + expectedVersion + " không còn hiệu lực");
  }

  private void audit(Authentication auth, String action, String resource, UUID id, String status) {
    try {
      String details = json.writeValueAsString(java.util.Map.of("resolutionStatus", status));
      audits.record(permissions.tenantKey(auth), permissions.account(auth), auth.getName(), action,
          resource, id.toString(), "SUCCESS", details);
    } catch (ApiProblem problem) {
      throw problem;
    } catch (Exception error) {
      throw new IllegalStateException("Không ghi được audit master data", error);
    }
  }

  private ProductView product(ResultSet row) throws SQLException {
    return new ProductView(row.getObject(1, UUID.class), row.getString(2), row.getString(3),
        row.getString(4), row.getString(5), row.getInt(6), row.getString(7),
        instant(row, 8), row.getTimestamp(9).toInstant());
  }

  private PartnerView partner(ResultSet row) throws SQLException {
    return new PartnerView(row.getObject(1, UUID.class), row.getString(2), row.getString(3),
        row.getString(4), row.getString(5), row.getString(6), row.getInt(7), row.getString(8),
        instant(row, 9), row.getTimestamp(10).toInstant());
  }

  private Instant instant(ResultSet row, int column) throws SQLException {
    var value = row.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }

  private String status(String value) {
    String status = text(value).toUpperCase(Locale.ROOT);
    if (!status.isEmpty() && !status.equals("UNVERIFIED") && !status.equals("RESOLVED"))
      throw new ApiProblem(HttpStatus.BAD_REQUEST, "MES_MASTER_DATA_STATUS_INVALID", "Trạng thái master data không hợp lệ");
    return status;
  }

  private String statusRequired(String value) {
    String status = status(value);
    if (status.isEmpty()) throw new ApiProblem(HttpStatus.BAD_REQUEST, "MES_MASTER_DATA_STATUS_REQUIRED", "Phải chọn trạng thái master data");
    return status;
  }

  private String partnerType(String value) {
    String type = text(value).toUpperCase(Locale.ROOT);
    if (!type.isEmpty() && !type.equals("SUPPLIER") && !type.equals("CUSTOMER"))
      throw new ApiProblem(HttpStatus.BAD_REQUEST, "MES_PARTNER_TYPE_INVALID", "Loại đối tác không hợp lệ");
    return type;
  }

  private String text(String value) { return value == null ? "" : value.trim(); }
  private String blankToNull(String value) { String cleaned = text(value); return cleaned.isEmpty() ? null : cleaned; }
}
