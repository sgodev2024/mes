package vn.coreplatform.mes;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.coreplatform.mes.reporting.MesWorkbookParser;

class MesWorkbookParserTest {
  @TempDir Path temp;
  private final MesWorkbookParser parser = new MesWorkbookParser();

  @Test
  void identificationRequiresFilenameWorksheetAndEveryHeaderToken() throws Exception {
    var spec = stockSpec();
    Path valid = workbook("valid.xlsx", "Template_Import", true);

    assertThat(parser.identify(valid, "4100_TT_Tonkho_Ngay.xlsx", List.of(spec))).contains(spec);
    assertThat(parser.identify(valid, "renamed.xlsx", List.of(spec))).isEmpty();

    Path missingHeader = workbook("missing-header.xlsx", "Template_Import", false);
    assertThat(parser.identify(missingHeader, "4100_TT_Tonkho_Ngay.xlsx", List.of(spec))).isEmpty();

    Path wrongSheet = workbook("wrong-sheet.xlsx", "Khac", true);
    assertThat(parser.identify(wrongSheet, "4100_TT_Tonkho_Ngay.xlsx", List.of(spec))).isEmpty();
  }

  @Test
  void parserPersistsActualSheetAndDoesNotPolluteHeadersWithPreambleRow() throws Exception {
    var spec = stockSpec();
    Path file = workbook("layout.xlsx", "Template_Import", true);

    var result = parser.parse(file, spec);

    assertThat(result.errorRows()).isZero();
    assertThat(result.records()).hasSize(1);
    assertThat(result.records().getFirst().sourceSheet()).isEqualTo("Template_Import");
    assertThat(result.records().getFirst().values().keySet())
        .allMatch(key -> !key.contains("Loại dữ liệu"))
        .anyMatch(key -> key.contains("Số lượng tồn kho cuối ngày"));
  }

  @Test
  void fieldContractRejectsMissingRequiredValueAndNegativeQuantity() throws Exception {
    Path file = invalidStockWorkbook();

    var result = parser.parse(file, stockSpec());

    assertThat(result.errorRows()).isEqualTo(1);
    assertThat(result.issues()).extracting(MesWorkbookParser.Issue::code)
        .contains("REQUIRED_VALUE", "NUMBER_BELOW_MINIMUM");
    assertThat(result.issues()).allMatch(issue -> issue.sheet().equals("Template_Import"));
  }

  private MesWorkbookParser.TemplateSpec stockSpec() {
    return new MesWorkbookParser.TemplateSpec(UUID.randomUUID(), "MES-DAY-STOCK", "Tồn kho than ngày",
        "^4100_TT_Tonkho_Ngay\\.xlsx$", "Template_Import", "4100",
        List.of("Mã sản phẩm", "Số lượng tồn kho cuối ngày", "Số lượng đi đường"), "TABULAR", List.of(
            new MesWorkbookParser.FieldSpec("product_code", "Mã sản phẩm", "TEXT", true, null, null, null),
            new MesWorkbookParser.FieldSpec("product_name", "Tên sản phẩm", "TEXT", true, null, null, null),
            new MesWorkbookParser.FieldSpec("unit", "ĐVT", "TEXT", true, null, null, null),
            new MesWorkbookParser.FieldSpec("closing_stock", "Số lượng tồn kho cuối ngày", "NUMBER", true, 0d, null, null),
            new MesWorkbookParser.FieldSpec("in_transit", "Số lượng đi đường", "NUMBER", false, 0d, null, null)));
  }

  private Path workbook(String name, String sheetName, boolean includeLastHeader) throws Exception {
    Path file = temp.resolve(name);
    try (var workbook = new XSSFWorkbook(); OutputStream output = Files.newOutputStream(file)) {
      var sheet = workbook.createSheet(sheetName);
      sheet.createRow(0).createCell(0).setCellValue("4100");
      sheet.createRow(4).createCell(0).setCellValue("Loại dữ liệu");
      var header = sheet.createRow(5);
      header.createCell(0).setCellValue("Mã sản phẩm");
      header.createCell(1).setCellValue("Tên sản phẩm");
      header.createCell(2).setCellValue("ĐVT");
      header.createCell(3).setCellValue("Số lượng tồn kho cuối ngày");
      if (includeLastHeader) header.createCell(4).setCellValue("Số lượng đi đường");
      var data = sheet.createRow(6);
      data.createCell(0).setCellValue("SP-NK-01");
      data.createCell(1).setCellValue("Than nguyên khai");
      data.createCell(2).setCellValue("tấn");
      data.createCell(3).setCellValue(106_400);
      data.createCell(4).setCellValue(2_100);
      workbook.write(output);
    }
    return file;
  }

  private Path invalidStockWorkbook() throws Exception {
    Path file = temp.resolve("invalid-stock.xlsx");
    try (var workbook = new XSSFWorkbook(); OutputStream output = Files.newOutputStream(file)) {
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
      data.createCell(2).setCellValue("tấn");
      data.createCell(3).setCellValue(-1);
      workbook.write(output);
    }
    return file;
  }
}
