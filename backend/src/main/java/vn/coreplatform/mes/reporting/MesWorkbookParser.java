package vn.coreplatform.mes.reporting;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Parser read-only cho Slice 1. File gốc không bị thay đổi; parser chỉ sinh staging records
 * và vị trí lỗi để người dùng sửa đúng sheet/dòng/ô.
 */
@Component
public class MesWorkbookParser {
  private static final int MAX_ROWS = 5_000;
  private static final int MAX_COLUMNS = 128;
  private static final int HEARTBEAT_ROW_INTERVAL = 100;
  private static final Runnable NOOP_HEARTBEAT = () -> {};
  private static final Pattern FORMULA_ERROR = Pattern.compile("#(REF!|DIV/0!|VALUE!|NAME\\?|N/A|NUM!|NULL!|SPILL!|CALC!)", Pattern.CASE_INSENSITIVE);

  static {
    ZipSecureFile.setMinInflateRatio(0.01d);
    ZipSecureFile.setMaxEntrySize(50L * 1024 * 1024);
    ZipSecureFile.setMaxTextSize(10L * 1024 * 1024);
  }

  public record TemplateSpec(
      UUID id,
      String code,
      String name,
      String filenamePattern,
      String worksheetName,
      String reportingEntityCode,
      List<String> headerTokens,
      String parserType,
      List<FieldSpec> fields) {}

  /** Contract cấp trường được lưu cùng phiên bản template trong Template Registry. */
  public record FieldSpec(
      String code,
      String label,
      String dataType,
      boolean required,
      Double minimum,
      Double maximum,
      String atLeastOneGroup) {}

  public record ParsedRecord(String sourceSheet, int sourceRow, String status, Map<String, String> values) {}

  public record Issue(
      String severity,
      String code,
      String sheet,
      Integer row,
      String cell,
      String field,
      String rawValue,
      String message) {}

  public record ParseResult(
      int totalRows,
      int validRows,
      int errorRows,
      int warningRows,
      List<ParsedRecord> records,
      List<Issue> issues) {}

  public Optional<TemplateSpec> identify(Path file, String originalName, List<TemplateSpec> templates) throws Exception {
    return identify(file, originalName, templates, NOOP_HEARTBEAT);
  }

  public Optional<TemplateSpec> identify(
      Path file, String originalName, List<TemplateSpec> templates, Runnable heartbeat) throws Exception {
    try (InputStream input = Files.newInputStream(file); Workbook workbook = WorkbookFactory.create(input)) {
      var scored = new ArrayList<ScoredTemplate>();
      for (var template : templates) {
        heartbeat.run();
        Sheet sheet = workbook.getSheet(template.worksheetName());
        boolean filenameMatches = Pattern.compile(template.filenamePattern(), Pattern.CASE_INSENSITIVE)
            .matcher(originalName).matches();
        if (!filenameMatches || sheet == null || template.headerTokens().isEmpty()) continue;
        int tokenMatches = countTokenMatches(sheet, template.headerTokens());
        if (tokenMatches != template.headerTokens().size()) continue;
        scored.add(new ScoredTemplate(template, 125 + tokenMatches * 5));
      }
      scored.sort((left, right) -> Integer.compare(right.score(), left.score()));
      if (scored.isEmpty()) return Optional.empty();
      if (scored.size() > 1 && scored.get(0).score() == scored.get(1).score()) return Optional.empty();
      return Optional.of(scored.getFirst().template());
    }
  }

  public ParseResult parse(Path file, TemplateSpec template) throws Exception {
    return parse(file, template, NOOP_HEARTBEAT);
  }

  public ParseResult parse(Path file, TemplateSpec template, Runnable heartbeat) throws Exception {
    var records = new ArrayList<ParsedRecord>();
    var issues = new ArrayList<Issue>();
    try (InputStream input = Files.newInputStream(file); Workbook workbook = WorkbookFactory.create(input)) {
      heartbeat.run();
      if (workbook instanceof XSSFWorkbook xssf && !xssf.getExternalLinksTable().isEmpty()) {
        issues.add(new Issue("ERROR", "EXTERNAL_LINK_NOT_ALLOWED", null, null, null, null, null,
            "Biểu mẫu có liên kết tới workbook bên ngoài; hãy đưa dữ liệu cần dùng vào chính file trước khi nhập"));
        return result(records, issues);
      }
      Sheet sheet = workbook.getSheet(template.worksheetName());
      if (sheet == null) {
        issues.add(new Issue("ERROR", "SHEET_MISSING", template.worksheetName(), null, null, null, null,
            "Không tìm thấy sheet bắt buộc " + template.worksheetName()));
        return result(records, issues);
      }

      var formatter = new DataFormatter(Locale.ROOT, true);
      var evaluator = workbook.getCreationHelper().createFormulaEvaluator();
      var headerRows = findHeaderRows(sheet, template.headerTokens(), formatter, evaluator);
      if (headerRows.size() != template.headerTokens().size()) {
        issues.add(new Issue("ERROR", "HEADER_INCOMPLETE", sheet.getSheetName(), null, null, null, null,
            "Không tìm thấy đủ tiêu đề bắt buộc của biểu mẫu " + template.code()));
        return result(records, issues);
      }
      int headerStart = headerRows.stream().min(Integer::compareTo).orElse(0);
      int headerEnd = headerRows.stream().max(Integer::compareTo).orElse(headerStart);
      var headers = headers(sheet, headerStart, headerEnd, formatter, evaluator);
      var fieldColumns = resolveFieldColumns(headers, template.fields());
      var missingColumns = template.fields().stream()
          .filter(field -> !fieldColumns.containsKey(field.code()))
          .toList();
      if (!missingColumns.isEmpty()) {
        for (var field : missingColumns) {
          issues.add(new Issue("ERROR", "FIELD_COLUMN_MISSING", sheet.getSheetName(), headerEnd + 1,
              null, field.label(), null, "Thiếu cột theo Data Contract: " + field.label()));
        }
        return result(records, issues);
      }

      boolean entityFound = containsValue(sheet, template.reportingEntityCode(), 12, formatter, evaluator);
      if (!entityFound) {
        issues.add(new Issue("WARNING", "REPORTING_ENTITY_NOT_FOUND", sheet.getSheetName(), null, null, null,
            template.reportingEntityCode(), "Không tìm thấy mã đơn vị báo cáo " + template.reportingEntityCode() + " trong vùng đầu biểu mẫu"));
      }

      int lastRow = Math.min(sheet.getLastRowNum(), headerEnd + MAX_ROWS);
      for (int rowIndex = headerEnd + 1; rowIndex <= lastRow; rowIndex++) {
        if ((rowIndex - headerEnd) % HEARTBEAT_ROW_INTERVAL == 0) heartbeat.run();
        Row row = sheet.getRow(rowIndex);
        if (row == null) continue;
        var values = new LinkedHashMap<String, String>();
        var valuesByColumn = new LinkedHashMap<Integer, String>();
        var rowIssues = new ArrayList<Issue>();
        int lastCell = Math.min(MAX_COLUMNS, Math.max(headers.size(), Math.max(0, row.getLastCellNum())));
        boolean hasValue = false;
        for (int column = 0; column < lastCell; column++) {
          Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
          String value = display(cell, formatter, evaluator);
          if (!value.isBlank()) hasValue = true;
          String key = headers.getOrDefault(column, "Cột " + CellReference.convertNumToColString(column));
          if (!value.isBlank()) {
            values.put(key, trim(value, 2_000));
            valuesByColumn.put(column, value);
          }
          if (FORMULA_ERROR.matcher(value).find()) {
            String cellRef = new CellReference(rowIndex, column).formatAsString();
            rowIssues.add(new Issue("ERROR", "FORMULA_ERROR", sheet.getSheetName(), rowIndex + 1, cellRef,
                key, trim(value, 1_000), "Ô công thức đang có lỗi " + value));
          }
        }
        if (!hasValue) continue;
        validateContractRow(template, sheet, row, rowIndex, headers, valuesByColumn, fieldColumns, formatter, evaluator, rowIssues);
        validateConditionalBusinessRules(template, sheet.getSheetName(), rowIndex, headers, valuesByColumn, rowIssues);
        boolean hasError = rowIssues.stream().anyMatch(issue -> "ERROR".equals(issue.severity()));
        boolean hasWarning = rowIssues.stream().anyMatch(issue -> "WARNING".equals(issue.severity()));
        String rowStatus = hasError ? "ERROR" : hasWarning ? "WARNING" : "VALID";
        records.add(new ParsedRecord(sheet.getSheetName(), rowIndex + 1, rowStatus, values));
        issues.addAll(rowIssues);
      }
      if (records.isEmpty()) {
        issues.add(new Issue("ERROR", "NO_DATA", sheet.getSheetName(), headerEnd + 2, null, null, null,
            "Biểu mẫu không có dòng dữ liệu sau vùng tiêu đề"));
      }
      if (sheet.getLastRowNum() > headerEnd + MAX_ROWS) {
        issues.add(new Issue("ERROR", "ROW_LIMIT_EXCEEDED", sheet.getSheetName(), headerEnd + MAX_ROWS + 2, null,
            null, null, "Biểu mẫu vượt giới hạn " + MAX_ROWS + " dòng dữ liệu của Slice 1"));
      }
    }
    return result(records, issues);
  }

  private ParseResult result(List<ParsedRecord> records, List<Issue> issues) {
    int valid = (int) records.stream().filter(row -> row.status().equals("VALID")).count();
    int errorRows = (int) records.stream().filter(row -> row.status().equals("ERROR")).count();
    if (records.isEmpty() && issues.stream().anyMatch(issue -> issue.severity().equals("ERROR"))) errorRows = 1;
    int warningRows = (int) issues.stream().filter(issue -> issue.severity().equals("WARNING"))
        .map(issue -> issue.row() == null ? -1 : issue.row()).distinct().count();
    return new ParseResult(records.size(), valid, errorRows, warningRows, List.copyOf(records), List.copyOf(issues));
  }

  private int countTokenMatches(Sheet sheet, List<String> tokens) {
    var formatter = new DataFormatter(Locale.ROOT, true);
    var evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
    return (int) tokens.stream().filter(token -> containsValue(sheet, token, 15, formatter, evaluator)).count();
  }

  private List<Integer> findHeaderRows(Sheet sheet, List<String> tokens, DataFormatter formatter, FormulaEvaluator evaluator) {
    var rows = new ArrayList<Integer>();
    for (String token : tokens) {
      String expected = normalize(token);
      for (int rowIndex = 0; rowIndex <= Math.min(sheet.getLastRowNum(), 14); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) continue;
        for (int column = 0; column < Math.min(MAX_COLUMNS, Math.max(0, row.getLastCellNum())); column++) {
          if (normalize(display(row.getCell(column), formatter, evaluator)).contains(expected)) {
            rows.add(rowIndex);
            rowIndex = 15;
            break;
          }
        }
      }
    }
    return rows;
  }

  private Map<Integer, String> headers(Sheet sheet, int start, int end, DataFormatter formatter, FormulaEvaluator evaluator) {
    var result = new LinkedHashMap<Integer, String>();
    int max = 0;
    for (int rowIndex = start; rowIndex <= end; rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (row != null) max = Math.max(max, Math.max(0, row.getLastCellNum()));
    }
    for (int column = 0; column < Math.min(MAX_COLUMNS, max); column++) {
      var labels = new ArrayList<String>();
      for (int rowIndex = start; rowIndex <= end; rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        String label = row == null ? "" : display(row.getCell(column), formatter, evaluator);
        if (!label.isBlank() && labels.stream().noneMatch(label::equalsIgnoreCase)) labels.add(trim(label, 160));
      }
      String columnName = "Cột " + CellReference.convertNumToColString(column);
      if (!labels.isEmpty()) columnName += " — " + String.join(" / ", labels);
      result.put(column, columnName);
    }
    return result;
  }

  private boolean containsValue(Sheet sheet, String expected, int maxRows, DataFormatter formatter, FormulaEvaluator evaluator) {
    String normalized = normalize(expected);
    for (int rowIndex = 0; rowIndex <= Math.min(sheet.getLastRowNum(), maxRows - 1); rowIndex++) {
      Row row = sheet.getRow(rowIndex);
      if (row == null) continue;
      for (int column = 0; column < Math.min(MAX_COLUMNS, Math.max(0, row.getLastCellNum())); column++) {
        if (normalize(display(row.getCell(column), formatter, evaluator)).contains(normalized)) return true;
      }
    }
    return false;
  }

  private Map<String, Integer> resolveFieldColumns(Map<Integer, String> headers, List<FieldSpec> fields) {
    var result = new LinkedHashMap<String, Integer>();
    for (var field : fields) {
      String expected = normalize(field.label());
      headers.entrySet().stream()
          .filter(entry -> normalize(entry.getValue()).contains(expected))
          .map(Map.Entry::getKey)
          .findFirst()
          .ifPresent(column -> result.put(field.code(), column));
    }
    return result;
  }

  private void validateContractRow(
      TemplateSpec template,
      Sheet sheet,
      Row row,
      int zeroBasedRow,
      Map<Integer, String> headers,
      Map<Integer, String> values,
      Map<String, Integer> fieldColumns,
      DataFormatter formatter,
      FormulaEvaluator evaluator,
      List<Issue> issues) {
    Set<String> groups = new java.util.LinkedHashSet<>();
    Set<String> populatedGroups = new java.util.LinkedHashSet<>();
    for (var field : template.fields()) {
      Integer column = fieldColumns.get(field.code());
      if (column == null) continue;
      Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
      String value = display(cell, formatter, evaluator);
      if (field.atLeastOneGroup() != null && !field.atLeastOneGroup().isBlank()) {
        groups.add(field.atLeastOneGroup());
        if (!value.isBlank()) populatedGroups.add(field.atLeastOneGroup());
      }
      if (field.required() && value.isBlank()) {
        addCellIssue(sheet.getSheetName(), zeroBasedRow, column, headers, null,
            "REQUIRED_VALUE", "Trường bắt buộc chưa có dữ liệu", issues);
        continue;
      }
      if (value.isBlank() || !"NUMBER".equalsIgnoreCase(field.dataType())) continue;
      var number = decimal(cell, value, evaluator);
      if (number.isEmpty()) {
        addCellIssue(sheet.getSheetName(), zeroBasedRow, column, headers, value,
            "NUMBER_INVALID", "Giá trị phải là số", issues);
        continue;
      }
      if (field.minimum() != null && number.get().compareTo(BigDecimal.valueOf(field.minimum())) < 0) {
        addCellIssue(sheet.getSheetName(), zeroBasedRow, column, headers, value,
            "NUMBER_BELOW_MINIMUM", "Giá trị không được nhỏ hơn " + plainNumber(field.minimum()), issues);
      }
      if (field.maximum() != null && number.get().compareTo(BigDecimal.valueOf(field.maximum())) > 0) {
        addCellIssue(sheet.getSheetName(), zeroBasedRow, column, headers, value,
            "NUMBER_ABOVE_MAXIMUM", "Giá trị không được lớn hơn " + plainNumber(field.maximum()), issues);
      }
    }
    for (String group : groups) {
      if (populatedGroups.contains(group)) continue;
      var labels = template.fields().stream().filter(field -> group.equals(field.atLeastOneGroup()))
          .map(FieldSpec::label).toList();
      issues.add(new Issue("ERROR", "VALUE_GROUP_REQUIRED", sheet.getSheetName(), zeroBasedRow + 1,
          null, group, null, "Phải nhập ít nhất một trường: " + String.join(", ", labels)));
    }
  }

  private void validateConditionalBusinessRules(
      TemplateSpec template,
      String sheet,
      int zeroBasedRow,
      Map<Integer, String> headers,
      Map<Integer, String> values,
      List<Issue> issues) {
    Predicate<String> purchasedReceipt = header -> header.contains("nhap mua ngoai tkv")
        || header.contains("nhap mua trong tkv");
    if (template.code().contains("RECEIPT") && hasNonZeroValue(headers, values, purchasedReceipt)) {
      requireAnyValue(sheet, zeroBasedRow, headers, values,
          header -> header.contains("ma nha cung cap") || header.contains("ten ncc") || header.contains("ten nha cung cap"),
          "SUPPLIER_REQUIRED", "Nhà cung cấp", "Dòng nhập mua phải có mã hoặc tên nhà cung cấp", issues);
    }
    Predicate<String> soldIssue = header -> header.contains("so luong xuat ban");
    if (template.code().contains("ISSUE") && hasNonZeroValue(headers, values, soldIssue)) {
      requireAnyValue(sheet, zeroBasedRow, headers, values,
          header -> header.contains("ma khach hang") || header.contains("ten kh") || header.contains("ten khach hang"),
          "CUSTOMER_REQUIRED", "Khách hàng", "Dòng xuất bán phải có mã hoặc tên khách hàng", issues);
    }
  }

  private boolean hasNonZeroValue(
      Map<Integer, String> headers, Map<Integer, String> values, Predicate<String> matcher) {
    return headers.entrySet().stream().anyMatch(entry -> matcher.test(normalize(entry.getValue()))
        && decimal(values.get(entry.getKey())).map(value -> value.signum() != 0).orElse(false));
  }

  private void requireAnyValue(
      String sheet, int row, Map<Integer, String> headers, Map<Integer, String> values,
      Predicate<String> matcher, String code, String field, String message, List<Issue> issues) {
    if (!hasAnyValue(headers, values, matcher))
      addMissingIssue(sheet, row, headers, matcher, code, field, message, issues);
  }

  private boolean hasAnyValue(
      Map<Integer, String> headers, Map<Integer, String> values, Predicate<String> matcher) {
    return headers.entrySet().stream().anyMatch(entry -> matcher.test(normalize(entry.getValue()))
        && !values.getOrDefault(entry.getKey(), "").isBlank());
  }

  private void addMissingIssue(
      String sheet, int row, Map<Integer, String> headers, Predicate<String> matcher,
      String code, String field, String message, List<Issue> issues) {
    int column = headers.entrySet().stream().filter(entry -> matcher.test(normalize(entry.getValue())))
        .map(Map.Entry::getKey).findFirst().orElse(0);
    issues.add(new Issue("ERROR", code, sheet, row + 1,
        new CellReference(row, column).formatAsString(), field, null, message));
  }

  private void addCellIssue(
      String sheet, int row, int column, Map<Integer, String> headers, String rawValue,
      String code, String message, List<Issue> issues) {
    issues.add(new Issue("ERROR", code, sheet, row + 1,
        new CellReference(row, column).formatAsString(), headers.get(column), trim(rawValue, 1_000), message));
  }

  private Optional<BigDecimal> decimal(Cell cell, String displayed, FormulaEvaluator evaluator) {
    if (cell != null) {
      try {
        if (cell.getCellType() == CellType.NUMERIC)
          return Optional.of(BigDecimal.valueOf(cell.getNumericCellValue()));
        if (cell.getCellType() == CellType.FORMULA) {
          var value = evaluator.evaluate(cell);
          if (value != null && value.getCellType() == CellType.NUMERIC)
            return Optional.of(BigDecimal.valueOf(value.getNumberValue()));
        }
      } catch (RuntimeException ignored) {
        // Tiếp tục parse chuỗi để trả lỗi đúng ô thay vì dừng toàn bộ lô.
      }
    }
    return decimal(displayed);
  }

  private String plainNumber(Double value) {
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
  }

  private Optional<BigDecimal> decimal(String raw) {
    if (raw == null || raw.isBlank()) return Optional.empty();
    String value = raw.trim().replace("\u00a0", "").replace(" ", "").replace("%", "");
    boolean parenthesesNegative = value.startsWith("(") && value.endsWith(")");
    if (parenthesesNegative) value = "-" + value.substring(1, value.length() - 1);
    int comma = value.lastIndexOf(',');
    int dot = value.lastIndexOf('.');
    if (comma >= 0 && dot >= 0) {
      if (comma > dot) value = value.replace(".", "").replace(',', '.');
      else value = value.replace(",", "");
    } else if (comma >= 0) {
      value = value.replace(',', '.');
    }
    try {
      return Optional.of(new BigDecimal(value));
    } catch (NumberFormatException ignored) {
      return Optional.empty();
    }
  }

  private String display(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
    if (cell == null || cell.getCellType() == CellType.BLANK) return "";
    try {
      return formatter.formatCellValue(cell, evaluator).trim();
    } catch (RuntimeException evaluationFailure) {
      try {
        return formatter.formatCellValue(cell).trim();
      } catch (RuntimeException ignored) {
        return "";
      }
    }
  }

  private String normalize(String value) {
    if (value == null) return "";
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replace('đ', 'd')
        .replaceAll("\\s+", " ")
        .trim();
  }

  private String trim(String value, int max) {
    if (value == null) return null;
    return value.length() <= max ? value : value.substring(0, max);
  }

  private record ScoredTemplate(TemplateSpec template, int score) {}
}
