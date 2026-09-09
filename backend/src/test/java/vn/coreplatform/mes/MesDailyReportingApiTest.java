package vn.coreplatform.mes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import vn.coreplatform.AbstractApiTest;
import vn.coreplatform.jobs.JobHandler;
import vn.coreplatform.kernel.TenantContext;
import vn.coreplatform.mes.reporting.MesImportJobHandler;

class MesDailyReportingApiTest extends AbstractApiTest {
  @Autowired MesImportJobHandler importJob;

  @Test
  void importsValidDailyWorkbookThenPublishesInternalDataWhilePortalIsDisabled() throws Exception {
    UUID tenantId = jdbc.queryForObject(
        "select id from platform.tenant where tenant_key='default'", UUID.class);
    try {
      TenantContext.set(tenantId);
      assertThat(jdbc.queryForObject("""
          select count(*) from mes.template_definition
          where tenant_id=? and field_contract <> '[]'::jsonb
            and contract_hash <> encode(digest('[]','sha256'),'hex')
          """, Integer.class, tenantId)).isEqualTo(6);
    } finally {
      TenantContext.clear();
    }
    String admin = adminToken();
    LocalDate reportingDate = LocalDate.of(2031, 1, 15);

    mvc.perform(get("/api/v1/mes/templates").with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(6))
        .andExpect(jsonPath("$[?(@.code == 'MES-DAY-STOCK')]").exists());

    mvc.perform(get("/api/v1/mes/reporting-calendar")
            .param("date", reportingDate.toString()).with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(6));

    byte[] workbook = stockWorkbook();
    String response = mvc.perform(multipart("/api/v1/mes/import-batches")
            .file(new MockMultipartFile("file", "4100_TT_Tonkho_Ngay.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbook))
            .param("reportingDate", reportingDate.toString()).with(bearer(admin)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("UPLOADED"))
        .andReturn().getResponse().getContentAsString();
    UUID batchId = UUID.fromString(json.readTree(response).get("id").asText());

    var payload = JsonNodeFactory.instance.objectNode().put("batchId", batchId.toString());
    importJob.handle(new JobHandler.JobContext(UUID.randomUUID(), "test-worker", "default", payload, () -> {}));

    mvc.perform(get("/api/v1/mes/import-batches/{id}", batchId).with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.templateCode").value("MES-DAY-STOCK"))
        .andExpect(jsonPath("$.status").value("PENDING_CONFIRMATION"))
        .andExpect(jsonPath("$.totalRows").value(1))
        .andExpect(jsonPath("$.validRows").value(1))
        .andExpect(jsonPath("$.errorRows").value(0));

    mvc.perform(get("/api/v1/mes/import-batches/{id}/records", batchId).with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.items[0].row").value(7))
        .andExpect(jsonPath("$.items[0].status").value("VALID"));

    mvc.perform(post("/api/v1/mes/import-batches/{id}/confirm", batchId).with(bearer(admin)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    mvc.perform(post("/api/v1/mes/import-batches/{id}/approve", batchId).with(bearer(admin)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
    mvc.perform(post("/api/v1/mes/import-batches/{id}/lock", batchId).with(bearer(admin)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("LOCKED"));

    mvc.perform(get("/api/v1/mes/internal-data/summary")
            .param("from", reportingDate.toString()).param("to", reportingDate.toString())
            .with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.releases").value(1))
        .andExpect(jsonPath("$.sourceRows").value(1))
        .andExpect(jsonPath("$.metricPoints").value(2))
        .andExpect(jsonPath("$.metricTotals.closing_stock").exists());
    mvc.perform(get("/api/v1/mes/internal-data/metrics")
            .param("from", reportingDate.toString()).param("to", reportingDate.toString())
            .with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.items[?(@.templateCode == 'MES-DAY-STOCK')]").exists())
        .andExpect(jsonPath("$.items[?(@.metricCode == 'closing_stock')]").exists());

    String replacementResponse = mvc.perform(multipart("/api/v1/mes/import-batches")
            .file(new MockMultipartFile("file", "4100_TT_Tonkho_Ngay.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", stockWorkbook(106_401)))
            .param("reportingDate", reportingDate.toString()).with(bearer(admin)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("UPLOADED"))
        .andReturn().getResponse().getContentAsString();
    UUID replacementBatchId = UUID.fromString(json.readTree(replacementResponse).get("id").asText());
    importJob.handle(new JobHandler.JobContext(UUID.randomUUID(), "test-worker", "default",
        JsonNodeFactory.instance.objectNode().put("batchId", replacementBatchId.toString()), () -> {}));
    mvc.perform(get("/api/v1/mes/import-batches/{id}", replacementBatchId).with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"));
    mvc.perform(get("/api/v1/mes/import-batches/{id}/issues", replacementBatchId).with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("REPORTING_PERIOD_PROTECTED"));
    mvc.perform(get("/api/v1/mes/reporting-calendar")
            .param("date", reportingDate.toString()).with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[?(@.templateCode == 'MES-DAY-STOCK')].status").value("LOCKED"))
        .andExpect(jsonPath("$.items[?(@.templateCode == 'MES-DAY-STOCK')].batchId").value(batchId.toString()));

    mvc.perform(get("/api/v1/mes/integrations/portal/status").with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mode").value("DISABLED"))
        .andExpect(jsonPath("$.automaticDispatchEnabled").value(false));
    mvc.perform(post("/api/v1/mes/import-batches/{id}/portal-submissions", batchId)
            .contentType(APPLICATION_JSON).content("{\"receiptCode\":\"PORTAL-2031-001\",\"note\":\"Đã nộp thủ công\"}")
            .with(bearer(admin)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("MES_PORTAL_INTEGRATION_DISABLED"));

    mvc.perform(get("/api/v1/mes/reporting-calendar")
            .param("date", reportingDate.toString()).with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[?(@.templateCode == 'MES-DAY-STOCK')].status").value("LOCKED"));

    try {
      TenantContext.set(tenantId);
      assertThat(jdbc.queryForObject(
          "select count(*) from mes.portal_submission where batch_id=?", Integer.class, batchId)).isZero();
    } finally {
      TenantContext.clear();
    }

    Integer auditCount = jdbc.queryForObject("""
        select count(*) from audit.event where resource_type='MES_IMPORT_BATCH' and resource_id=?
        """, Integer.class, batchId.toString());
    assertThat(auditCount).isGreaterThanOrEqualTo(5);
  }

  @Test
  void keepsDuplicateAsAuditableSupersededBatchWithoutEnqueuingAnotherImport() throws Exception {
    String admin = adminToken();
    LocalDate reportingDate = LocalDate.of(2031, 4, 18);
    byte[] workbook = stockWorkbook();

    String firstResponse = mvc.perform(multipart("/api/v1/mes/import-batches")
            .file(new MockMultipartFile("file", "4100_TT_Tonkho_Ngay.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbook))
            .param("reportingDate", reportingDate.toString()).with(bearer(admin)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("UPLOADED"))
        .andReturn().getResponse().getContentAsString();
    UUID firstBatchId = UUID.fromString(json.readTree(firstResponse).get("id").asText());

    String duplicateResponse = mvc.perform(multipart("/api/v1/mes/import-batches")
            .file(new MockMultipartFile("file", "4100_TT_Tonkho_Ngay.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbook))
            .param("reportingDate", reportingDate.toString()).with(bearer(admin)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("SUPERSEDED"))
        .andExpect(jsonPath("$.duplicateOf").value(firstBatchId.toString()))
        .andReturn().getResponse().getContentAsString();
    UUID duplicateBatchId = UUID.fromString(json.readTree(duplicateResponse).get("id").asText());

    Integer importJobs = jdbc.queryForObject("""
        select count(*) from async.job
        where idempotency_key in (?, ?)
        """, Integer.class, "mes-import-" + firstBatchId, "mes-import-" + duplicateBatchId);
    assertThat(importJobs).isEqualTo(1);
    assertThat(jdbc.queryForObject(
        "select count(*) from mes.import_record where batch_id=?", Integer.class, duplicateBatchId)).isZero();

    mvc.perform(multipart("/api/v1/mes/import-batches")
            .file(new MockMultipartFile("file", "4100_TT_Tonkho_Ngay.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbook))
            .param("reportingDate", reportingDate.plusDays(1).toString()).with(bearer(admin)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("UPLOADED"))
        .andExpect(jsonPath("$.duplicateOf").doesNotExist());
  }

  @Test
  void rejectsUnknownWorkbookWithCellLevelIssue() throws Exception {
    String admin = adminToken();
    LocalDate date = LocalDate.of(2031, 2, 16);
    byte[] workbook;
    try (var book = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
      book.createSheet("Khac").createRow(0).createCell(0).setCellValue("Không phải biểu mẫu MES");
      book.write(output);
      workbook = output.toByteArray();
    }
    String response = mvc.perform(multipart("/api/v1/mes/import-batches")
            .file(new MockMultipartFile("file", "unknown.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbook))
            .param("reportingDate", date.toString()).with(bearer(admin)))
        .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
    UUID batchId = UUID.fromString(json.readTree(response).get("id").asText());
    var payload = JsonNodeFactory.instance.objectNode().put("batchId", batchId.toString());
    importJob.handle(new JobHandler.JobContext(UUID.randomUUID(), "test-worker", "default", payload, () -> {}));

    mvc.perform(get("/api/v1/mes/import-batches/{id}", batchId).with(bearer(admin)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UNKNOWN_TEMPLATE"));
    mvc.perform(get("/api/v1/mes/import-batches/{id}/issues", batchId).with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("UNKNOWN_TEMPLATE"))
        .andExpect(jsonPath("$[0].message").value(org.hamcrest.Matchers.containsString("Không nhận diện")));
  }

  @Test
  void mesReportingApiIsFailClosedForApplicationUser() throws Exception {
    String marker = suffix();
    String email = "mes-user-" + marker + "@core.local";
    String password = "MesUserPass@2026";
    seedDefaultTenantAccount(email, password);
    String token = login(email, password);
    mvc.perform(get("/api/v1/mes/reporting-calendar/summary")
            .param("date", LocalDate.of(2031, 3, 17).toString()).with(bearer(token)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
  }

  private byte[] stockWorkbook() throws Exception {
    return stockWorkbook(106_400);
  }

  private byte[] stockWorkbook(double closingStock) throws Exception {
    try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Template_Import");
      sheet.createRow(0).createCell(0).setCellValue("4100");
      var header = sheet.createRow(5);
      header.createCell(0).setCellValue("Mã sản phẩm");
      header.createCell(1).setCellValue("Tên sản phẩm");
      header.createCell(2).setCellValue("ĐVT");
      header.createCell(3).setCellValue("Số lượng tồn kho cuối ngày");
      header.createCell(4).setCellValue("Số lượng đi đường");
      var data = sheet.createRow(6);
      data.createCell(0).setCellValue("SP-NK-01");
      data.createCell(1).setCellValue("Than nguyên khai");
      data.createCell(2).setCellValue("tấn");
      data.createCell(3).setCellValue(closingStock);
      data.createCell(4).setCellValue(2_100);
      workbook.write(output);
      return output.toByteArray();
    }
  }
}
