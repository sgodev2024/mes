package vn.coreplatform.mes.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import vn.coreplatform.AbstractApiTest;
import vn.coreplatform.kernel.TenantContext;

/**
 * UAT tùy chọn trên workbook do nghiệp vụ bàn giao. File không được copy vào source hay sửa đổi.
 * Chạy bằng MES_UAT_SOURCE_DIR=/duong/dan/da/giai/nen ./mvnw -Dtest=... test.
 */
@EnabledIfEnvironmentVariable(named = "MES_UAT_SOURCE_DIR", matches = ".+")
class MesRealWorkbookContractUatTest extends AbstractApiTest {
  private static final Map<String, String> EXPECTED = new LinkedHashMap<>();
  static {
    EXPECTED.put("4100_TT_Ngay_PTCB_01.xlsx", "MES-DAY-PRODUCTION-PTCB");
    EXPECTED.put("4100_TT_NhapKho_Ngay_thanNK.xlsx", "MES-DAY-RAW-COAL-RECEIPT");
    EXPECTED.put("4100_TT_NhapKho_Ngay_than_sach.xlsx", "MES-DAY-CLEAN-COAL-RECEIPT");
    EXPECTED.put("4100_TT_Tonkho_Ngay.xlsx", "MES-DAY-STOCK");
    EXPECTED.put("4100_TT_XuatKho_Ngay_than_sach.xlsx", "MES-DAY-CLEAN-COAL-ISSUE");
    EXPECTED.put("4100_TT_XuatKho_Ngay_thanNK.xlsx", "MES-DAY-RAW-COAL-ISSUE");
  }

  @Autowired MesDailyReportingService reporting;
  @Autowired MesWorkbookParser parser;

  @Test
  void actualPilotWorkbooksMatchRuntimeContractsAndContainNoBusinessRowsYet() throws Exception {
    Path source = Path.of(System.getenv("MES_UAT_SOURCE_DIR"));
    UUID tenant = jdbc.queryForObject(
        "select id from platform.tenant where tenant_key='default'", UUID.class);
    try {
      TenantContext.set(tenant);
      reporting.ensurePilotTemplatesForTenant(tenant);
      var specs = reporting.templateSpecsForTenant(tenant);
      assertThat(specs).hasSize(6);
      for (var expected : EXPECTED.entrySet()) {
        Path workbook = source.resolve(expected.getKey());
        assertThat(workbook).as("workbook UAT %s", expected.getKey()).isRegularFile();
        byte[] before = Files.readAllBytes(workbook);
        var identified = parser.identify(workbook, expected.getKey(), specs);
        assertThat(identified).isPresent();
        assertThat(identified.orElseThrow().code()).isEqualTo(expected.getValue());
        var result = parser.parse(workbook, identified.orElseThrow());
        assertThat(result.records()).as("Biểu mẫu nguồn hiện chưa có số liệu nghiệp vụ").isEmpty();
        assertThat(result.issues()).extracting(MesWorkbookParser.Issue::code)
            .containsExactly("NO_DATA");
        assertThat(Files.readAllBytes(workbook)).isEqualTo(before);
      }
    } finally {
      TenantContext.clear();
    }
  }
}
