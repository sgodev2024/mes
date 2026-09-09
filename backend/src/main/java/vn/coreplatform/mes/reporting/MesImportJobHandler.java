package vn.coreplatform.mes.reporting;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.coreplatform.jobs.JobHandler;
import vn.coreplatform.kernel.TenantContext;

/** Job tenant-aware để parse workbook ngoài request HTTP. */
@Component
public class MesImportJobHandler implements JobHandler {
  public static final String JOB_TYPE = "MES_XLSX_IMPORT";

  private final JdbcTemplate jdbc;
  private final MesDailyReportingService service;

  public MesImportJobHandler(JdbcTemplate jdbc, MesDailyReportingService service) {
    this.jdbc = jdbc;
    this.service = service;
  }

  @Override
  public String jobType() {
    return JOB_TYPE;
  }

  @Override
  public void handle(JobContext context) {
    String batchId = context.payload().path("batchId").asText("");
    if (batchId.isBlank()) throw new NonRetryableJobException("batchId bắt buộc");
    UUID parsedBatchId;
    try {
      parsedBatchId = UUID.fromString(batchId);
    } catch (IllegalArgumentException error) {
      throw new NonRetryableJobException("batchId không hợp lệ", error);
    }
    var tenants = jdbc.query(
        "select id from platform.tenant where tenant_key=? and status='ACTIVE'",
        (row, index) -> row.getObject(1, UUID.class), context.tenantKey());
    if (tenants.isEmpty())
      throw new NonRetryableJobException("Tenant không hoạt động: " + context.tenantKey());
    UUID tenantId = tenants.getFirst();
    TenantContext.set(tenantId);
    try {
      context.heartbeat().run();
      service.processBatch(parsedBatchId, context.heartbeat());
    } finally {
      TenantContext.clear();
    }
  }
}
