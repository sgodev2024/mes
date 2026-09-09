package vn.coreplatform.mes;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import vn.coreplatform.mes.reporting.MesImportJobHandler;

class MesMasterDataReconciliationApiTest extends AbstractApiTest {
  @Autowired MesImportJobHandler importJob;

  @Test
  void confirmsCanonicalMasterWithOptimisticLockAndNeverReportsBalanceOnMissingCoverage() throws Exception {
    String admin = adminToken();
    String productCode = "UAT-" + suffix();
    LocalDate date = LocalDate.of(2035, 6, 18);
    publishStock(admin, date.minusDays(1), productCode, 100);
    publishStock(admin, date, productCode, 90);

    String masterJson = mvc.perform(get("/api/v1/mes/master-data/products")
            .param("search", productCode).with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.items[0].resolutionStatus").value("UNVERIFIED"))
        .andReturn().getResponse().getContentAsString();
    var product = json.readTree(masterJson).path("items").get(0);
    UUID productId = UUID.fromString(product.path("id").asText());
    int version = product.path("version").asInt();

    mvc.perform(get("/api/v1/mes/reconciliation/inventory").param("date", date.toString())
            .with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.coveredContracts").value(1))
        .andExpect(jsonPath("$.coverageComplete").value(false))
        .andExpect(jsonPath("$.balanced").value(0))
        .andExpect(jsonPath("$.items[0].status").value("MASTER_UNVERIFIED"));

    String update = """
        {"code":"%s","name":"Than UAT chuẩn","unitCode":"TONNE",
         "resolutionStatus":"RESOLVED","expectedVersion":%d}
        """.formatted(productCode, version);
    mvc.perform(patch("/api/v1/mes/master-data/products/{id}", productId)
            .contentType(APPLICATION_JSON).content(update).with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resolutionStatus").value("RESOLVED"))
        .andExpect(jsonPath("$.version").value(version + 1));

    mvc.perform(patch("/api/v1/mes/master-data/products/{id}", productId)
            .contentType(APPLICATION_JSON).content(update).with(bearer(admin)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("MES_MASTER_DATA_VERSION_CONFLICT"));

    mvc.perform(get("/api/v1/mes/reconciliation/inventory").param("date", date.toString())
            .with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balanced").value(0))
        .andExpect(jsonPath("$.incomplete").value(1))
        .andExpect(jsonPath("$.items[0].status").value("INCOMPLETE"))
        .andExpect(jsonPath("$.items[0].openingStock").value(100))
        .andExpect(jsonPath("$.items[0].reportedClosingStock").value(90));
  }

  private void publishStock(String admin, LocalDate date, String productCode, double stock) throws Exception {
    String response = mvc.perform(multipart("/api/v1/mes/import-batches")
            .file(new MockMultipartFile("file", "4100_TT_Tonkho_Ngay.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                stockWorkbook(productCode, stock)))
            .param("reportingDate", date.toString()).with(bearer(admin)))
        .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
    UUID batchId = UUID.fromString(json.readTree(response).path("id").asText());
    importJob.handle(new JobHandler.JobContext(UUID.randomUUID(), "test-worker", "default",
        JsonNodeFactory.instance.objectNode().put("batchId", batchId.toString()), () -> {}));
    mvc.perform(post("/api/v1/mes/import-batches/{id}/confirm", batchId).with(bearer(admin)))
        .andExpect(status().isOk());
    mvc.perform(post("/api/v1/mes/import-batches/{id}/approve", batchId).with(bearer(admin)))
        .andExpect(status().isOk());
    mvc.perform(post("/api/v1/mes/import-batches/{id}/lock", batchId).with(bearer(admin)))
        .andExpect(status().isOk());
  }

  private byte[] stockWorkbook(String productCode, double closingStock) throws Exception {
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
      data.createCell(0).setCellValue(productCode);
      data.createCell(1).setCellValue("Than UAT");
      data.createCell(2).setCellValue("tấn");
      data.createCell(3).setCellValue(closingStock);
      data.createCell(4).setCellValue(0);
      workbook.write(output);
      return output.toByteArray();
    }
  }
}
