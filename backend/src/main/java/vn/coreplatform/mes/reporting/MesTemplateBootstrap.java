package vn.coreplatform.mes.reporting;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.coreplatform.kernel.TenantContext;

/** Seed/backfill Data Contract sau Flyway và trước khi runtime nhận traffic. */
@Component
@Order(20)
class MesTemplateBootstrap implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  private final MesDailyReportingService reporting;

  MesTemplateBootstrap(JdbcTemplate jdbc, MesDailyReportingService reporting) {
    this.jdbc = jdbc;
    this.reporting = reporting;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    var tenants = jdbc.query("select id from platform.tenant order by created_at",
        (row, index) -> row.getObject(1, java.util.UUID.class));
    for (var tenant : tenants) {
      try {
        TenantContext.set(tenant);
        reporting.ensurePilotTemplatesForTenant(tenant);
      } finally {
        TenantContext.clear();
      }
    }
  }
}
