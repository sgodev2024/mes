package vn.coreplatform.mes.operations;

import static vn.coreplatform.shared.ApiExceptionHandler.ApiProblem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.coreplatform.audit.AuditService;
import vn.coreplatform.eventing.OutboxService;
import vn.coreplatform.permission.PermissionService;

/** Runtime cơ bản dùng chung cho các phân hệ MES trước khi tách domain chuyên sâu. */
@Service
public class MesOperationalModuleService {
  private static final Set<String> MODULES = Set.of(
      "planning-performance", "production-operations", "quality-acceptance", "sales-logistics",
      "materials-equipment", "occupational-safety", "finance-accounting", "workforce-labor",
      "investment-projects", "science-digital", "alerts-directives", "esg", "portal-tkv",
      "integration-quality");
  private static final Set<String> STATUSES = Set.of(
      "DRAFT", "IN_PROGRESS", "PENDING_APPROVAL", "APPROVED", "CLOSED", "REJECTED");
  private static final Set<String> WORKFORCE_TABS = Set.of(
      "Danh sách nhân sự", "Hồ sơ nhân sự", "Chức danh và đơn vị", "Phân ca và ngày công",
      "Năng suất lao động", "Đào tạo và chứng chỉ");

  private final JdbcTemplate jdbc;
  private final PermissionService permissions;
  private final AuditService audits;
  private final OutboxService outbox;
  private final ObjectMapper json;

  public MesOperationalModuleService(
      JdbcTemplate jdbc, PermissionService permissions, AuditService audits,
      OutboxService outbox, ObjectMapper json) {
    this.jdbc = jdbc;
    this.permissions = permissions;
    this.audits = audits;
    this.outbox = outbox;
    this.json = json;
  }

  public record KpiView(String label, String value, String note, String tone) {}
  public record TrendPoint(LocalDate date, BigDecimal value) {}
  public record AttentionView(UUID id, String title, String detail, String severity) {}
  public record RecordView(
      UUID id, String code, String title, String tab, String statusCode, String statusLabel,
      String organization, String period, String correlationKey, String source, String severity, int version,
      Map<String, Object> data, List<String> actions, Instant updatedAt) {}
  public record Overview(
      String module, String tab, List<KpiView> kpis, List<TrendPoint> trend,
      List<AttentionView> attention, List<RecordView> items, long total) {}
  public record CreateInput(
      String tab, String title, String organization, String period, BigDecimal value, BigDecimal target,
      String unit, String owner, String severity, String source, String note, LocalDate occurredOn,
      LocalDate dueOn, Map<String,Object> details) {}

  @Transactional(readOnly = true)
  public Overview overview(
      Authentication authentication, String module, String tab, String search, String status) {
    UUID tenant = permissions.tenant(authentication);
    String safeModule = module(module);
    String safeTab = required(tab, "MES_TAB_REQUIRED", "Phải chọn tab nghiệp vụ", 120);
    String safeSearch = text(search).toLowerCase(Locale.ROOT);
    String safeStatus = statusFilter(status);
    long total = jdbc.queryForObject("""
        select count(*) from mes.operational_record
        where tenant_id=? and module_code=? and tab_name=?
          and (?='' or status=?)
          and (?='' or position(? in lower(record_code||' '||title||' '||organization_code||' '||coalesce(owner_name,'')))>0)
        """, Long.class, tenant, safeModule, safeTab, safeStatus, safeStatus,
        safeSearch, safeSearch);
    List<RecordView> items = jdbc.query("""
        select id,record_code,title,tab_name,status,organization_code,period_key,correlation_key,metric_value,
          target_value,unit_code,owner_name,severity,source_type,details::text,version,
          occurred_on,due_on,updated_at
        from mes.operational_record
        where tenant_id=? and module_code=? and tab_name=?
          and (?='' or status=?)
          and (?='' or position(? in lower(record_code||' '||title||' '||organization_code||' '||coalesce(owner_name,'')))>0)
        order by occurred_on desc,created_at desc,id limit 100
        """, (row, index) -> view(safeModule, row), tenant, safeModule, safeTab,
        safeStatus, safeStatus, safeSearch, safeSearch);
    return new Overview(safeModule, safeTab, kpis(tenant, safeModule, safeTab),
        trend(tenant, safeModule, safeTab), attention(tenant, safeModule), items, total);
  }

  @Transactional
  public RecordView create(Authentication authentication, String module, CreateInput input) {
    String safeModule = module(module);
    UUID tenant = permissions.tenant(authentication);
    UUID actor = permissions.account(authentication);
    String title = required(input.title(), "MES_TITLE_REQUIRED", "Nội dung nghiệp vụ phải có ít nhất 3 ký tự", 300);
    if (title.length() < 3) throw new ApiProblem(HttpStatus.BAD_REQUEST, "MES_TITLE_INVALID", "Nội dung nghiệp vụ phải có ít nhất 3 ký tự");
    String tab = required(input.tab(), "MES_TAB_REQUIRED", "Phải chọn tab nghiệp vụ", 120);
    String organization = required(input.organization(), "MES_ORGANIZATION_REQUIRED", "Phải chọn đơn vị", 80);
    String period = required(input.period(), "MES_PERIOD_REQUIRED", "Phải chọn kỳ dữ liệu", 30);
    String severity = enumValue(input.severity(), Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL"), "MEDIUM");
    String source = enumValue(input.source(), Set.of("MANUAL", "EXCEL", "API", "DATALAKE"), "MANUAL");
    if (input.occurredOn()!=null && input.dueOn()!=null && input.dueOn().isBefore(input.occurredOn()))
      throw new ApiProblem(HttpStatus.BAD_REQUEST,"MES_DATE_RANGE_INVALID","Ngày kết thúc không được trước ngày bắt đầu");
    var details = json.createObjectNode().put("note", text(input.note())).put("createdFrom", "MES_UI");
    if (safeModule.equals("workforce-labor")) validateAndCopyWorkforceDetails(tenant,tab,input,details);
    String prefix = safeModule.equals("workforce-labor") ? workforcePrefix(tab)
        : safeModule.substring(0, Math.min(3, safeModule.length())).toUpperCase(Locale.ROOT);
    String code = prefix
        + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    UUID id = UUID.randomUUID();
    BigDecimal target = input.target()==null?BigDecimal.valueOf(100):input.target();
    LocalDate occurredOn = input.occurredOn()==null?LocalDate.now():input.occurredOn();
    LocalDate dueOn = input.dueOn()==null&&!safeModule.equals("workforce-labor")?occurredOn.plusDays(7):input.dueOn();
    jdbc.update("""
        insert into mes.operational_record(
          id,tenant_id,module_code,tab_name,record_code,title,organization_code,period_key,
          metric_value,target_value,unit_code,owner_name,severity,source_type,status,
          correlation_key,details,occurred_on,due_on,created_by,updated_by)
        values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,'DRAFT',?,?::jsonb,?,?,?,?)
        """, id, tenant, safeModule, tab, code, title, organization, period,
        input.value(), target, clean(input.unit()), clean(input.owner()), severity, source,
        "MK-" + occurredOn + "-" + organization, details.toString(), occurredOn, dueOn, actor, actor);
    event(authentication, "mes.operation.created.v1", "MES_OPERATION_CREATED", safeModule, id,
        json.createObjectNode().put("module", safeModule).put("tab", tab).put("code", code));
    return find(tenant, safeModule, id);
  }

  private void validateAndCopyWorkforceDetails(
      UUID tenant,String tab,CreateInput input,com.fasterxml.jackson.databind.node.ObjectNode target) {
    if (!WORKFORCE_TABS.contains(tab))
      throw new ApiProblem(HttpStatus.BAD_REQUEST,"MES_WORKFORCE_TAB_INVALID","Tab Nhân sự & lao động không hợp lệ");
    Map<String,Object> source=input.details()==null?Map.of():input.details();
    switch(tab){
      case "Danh sách nhân sự" -> {
        copyRequired(source,target,"fullName","Phải nhập họ và tên nhân sự",160);
        copyRequired(source,target,"position","Phải nhập chức danh",160);
      }
      case "Hồ sơ nhân sự" -> {
        String employeeCode=copyRequired(source,target,"employeeCode","Phải chọn nhân sự",40);
        requireEmployee(tenant,employeeCode);
        copyRequired(source,target,"fullName","Nhân sự không hợp lệ",160);
        copyRequired(source,target,"profileType","Phải chọn loại hồ sơ",80);
        copyRequired(source,target,"contractType","Phải chọn loại hợp đồng",80);
        copyOptional(source,target,"contact",160);
      }
      case "Chức danh và đơn vị" -> {
        copyRequired(source,target,"position","Phải nhập tên chức danh",160);
        if(input.value()==null||input.target()==null)
          throw new ApiProblem(HttpStatus.BAD_REQUEST,"MES_WORKFORCE_HEADCOUNT_REQUIRED","Phải nhập hiện có và định biên");
      }
      case "Phân ca và ngày công" -> {
        copyRequired(source,target,"shift","Phải chọn ca làm việc",40);
        if(input.occurredOn()==null||input.value()==null||input.target()==null)
          throw new ApiProblem(HttpStatus.BAD_REQUEST,"MES_WORKFORCE_SHIFT_REQUIRED","Phải nhập ngày, kế hoạch và số lao động có mặt");
        if(input.value().compareTo(input.target())>0)
          throw new ApiProblem(HttpStatus.BAD_REQUEST,"MES_WORKFORCE_PRESENT_INVALID","Số lao động có mặt không được lớn hơn kế hoạch");
      }
      case "Năng suất lao động" -> {
        if(input.value()==null||input.target()==null||input.target().signum()<=0)
          throw new ApiProblem(HttpStatus.BAD_REQUEST,"MES_WORKFORCE_PRODUCTIVITY_REQUIRED","Sản lượng và ngày công phải lớn hơn 0");
      }
      case "Đào tạo và chứng chỉ" -> {
        String employeeCode=copyRequired(source,target,"employeeCode","Phải chọn nhân sự",40);
        requireEmployee(tenant,employeeCode);
        copyRequired(source,target,"fullName","Nhân sự không hợp lệ",160);
        copyRequired(source,target,"course","Phải nhập khóa học hoặc chứng chỉ",200);
        if(input.occurredOn()==null)
          throw new ApiProblem(HttpStatus.BAD_REQUEST,"MES_WORKFORCE_TRAINING_DATE_REQUIRED","Phải nhập ngày cấp");
      }
      default -> throw new ApiProblem(HttpStatus.BAD_REQUEST,"MES_WORKFORCE_TAB_INVALID","Tab Nhân sự & lao động không hợp lệ");
    }
  }

  private String workforcePrefix(String tab){return switch(tab){
    case "Danh sách nhân sự"->"NV";case "Hồ sơ nhân sự"->"HS";case "Chức danh và đơn vị"->"CD";
    case "Phân ca và ngày công"->"CA";case "Năng suất lao động"->"NS";case "Đào tạo và chứng chỉ"->"DT";
    default->"NSL";};}
  private void requireEmployee(UUID tenant,String employeeCode){
    Integer count=jdbc.queryForObject("""
        select count(*) from mes.operational_record
        where tenant_id=? and module_code='workforce-labor' and tab_name='Danh sách nhân sự' and record_code=?
        """,Integer.class,tenant,employeeCode);
    if(count==null||count==0)throw new ApiProblem(HttpStatus.BAD_REQUEST,"MES_WORKFORCE_EMPLOYEE_NOT_FOUND","Nhân sự được chọn không còn tồn tại");
  }
  private String copyRequired(Map<String,Object> source,com.fasterxml.jackson.databind.node.ObjectNode target,String key,String message,int max){
    String value=required(source.get(key)==null?null:String.valueOf(source.get(key)),"MES_WORKFORCE_FIELD_REQUIRED",message,max);
    target.put(key,value);return value;
  }
  private void copyOptional(Map<String,Object> source,com.fasterxml.jackson.databind.node.ObjectNode target,String key,int max){
    String value=text(source.get(key)==null?null:String.valueOf(source.get(key)));
    if(value.length()>max)throw new ApiProblem(HttpStatus.BAD_REQUEST,"MES_WORKFORCE_FIELD_INVALID","Dữ liệu "+key+" vượt quá độ dài cho phép");
    target.put(key,value);
  }

  @Transactional
  public RecordView transition(
      Authentication authentication, String module, UUID id, String action, int expectedVersion) {
    String safeModule = module(module);
    UUID tenant = permissions.tenant(authentication);
    UUID actor = permissions.account(authentication);
    String safeAction = text(action).toUpperCase(Locale.ROOT);
    var current = jdbc.queryForList("""
        select status from mes.operational_record where tenant_id=? and module_code=? and id=?
        """, tenant, safeModule, id);
    if (current.isEmpty()) throw new ApiProblem(HttpStatus.NOT_FOUND, "MES_OPERATION_NOT_FOUND", "Không tìm thấy bản ghi nghiệp vụ");
    String from = String.valueOf(current.getFirst().get("status"));
    String to = next(from, safeAction);
    if (safeAction.equals("APPROVE") || safeAction.equals("REJECT"))
      permissions.require(authentication, "MES_OPERATION", "APPROVE", null);
    int changed = jdbc.update("""
        update mes.operational_record set status=?,version=version+1,updated_by=?,updated_at=now()
        where tenant_id=? and module_code=? and id=? and version=? and status=?
        """, to, actor, tenant, safeModule, id, expectedVersion, from);
    if (changed == 0) throw new ApiProblem(HttpStatus.CONFLICT, "MES_OPERATION_VERSION_CONFLICT", "Bản ghi đã thay đổi; hãy tải lại dữ liệu trước khi thao tác");
    event(authentication, "mes.operation.status-changed.v1", "MES_OPERATION_STATUS_CHANGED",
        safeModule, id, json.createObjectNode().put("module", safeModule).put("from", from).put("to", to).put("action", safeAction));
    return find(tenant, safeModule, id);
  }

  private List<KpiView> kpis(UUID tenant, String module, String tab) {
    Map<String, Object> values = jdbc.queryForMap("""
        select count(*) total,
          count(*) filter(where status in ('DRAFT','IN_PROGRESS','PENDING_APPROVAL','REJECTED')) active,
          count(*) filter(where status in ('APPROVED','CLOSED')) completed,
          count(*) filter(where severity in ('HIGH','CRITICAL') and status<>'CLOSED') exceptions,
          coalesce(round(avg(case when target_value>0 then metric_value/target_value*100 end),1),0) completion
        from mes.operational_record where tenant_id=? and module_code=? and tab_name=?
        """, tenant, module, tab);
    return List.of(
        new KpiView("Tổng bản ghi", number(values,"total"), "Dữ liệu của tab đang chọn", "blue"),
        new KpiView("Đang xử lý", number(values,"active"), "Bao gồm chờ phê duyệt", "amber"),
        new KpiView("Đã hoàn thành", number(values,"completed"), "Đã duyệt hoặc đóng", "green"),
        new KpiView("Mức hoàn thành", number(values,"completion")+"%", number(values,"exceptions")+" ngoại lệ cần chú ý", Long.parseLong(number(values,"exceptions"))>0?"red":"green"));
  }

  private List<TrendPoint> trend(UUID tenant, String module, String tab) {
    Map<LocalDate, BigDecimal> values = new LinkedHashMap<>();
    for (int day=6; day>=0; day--) values.put(LocalDate.now().minusDays(day), BigDecimal.ZERO);
    var daily = jdbc.query("""
        select occurred_on,coalesce(sum(metric_value),0) from mes.operational_record
        where tenant_id=? and module_code=? and tab_name=? and occurred_on between current_date-6 and current_date
        group by occurred_on order by occurred_on
        """, (row,index) -> Map.entry(row.getObject(1,LocalDate.class),row.getBigDecimal(2)), tenant,module,tab);
    daily.forEach(item -> values.put(item.getKey(),item.getValue()));
    return values.entrySet().stream().map(item -> new TrendPoint(item.getKey(),item.getValue())).toList();
  }

  private List<AttentionView> attention(UUID tenant, String module) {
    return jdbc.query("""
        select id,title,coalesce(owner_name,organization_code),severity from mes.operational_record
        where tenant_id=? and module_code=? and severity in ('HIGH','CRITICAL') and status not in ('CLOSED','APPROVED')
        order by case severity when 'CRITICAL' then 0 else 1 end,due_on,created_at limit 5
        """, (row,index)->new AttentionView(row.getObject(1,UUID.class),row.getString(2),row.getString(3),severityLabel(row.getString(4))), tenant,module);
  }

  private RecordView find(UUID tenant, String module, UUID id) {
    var rows = jdbc.query("""
        select id,record_code,title,tab_name,status,organization_code,period_key,correlation_key,metric_value,
          target_value,unit_code,owner_name,severity,source_type,details::text,version,
          occurred_on,due_on,updated_at from mes.operational_record
        where tenant_id=? and module_code=? and id=?
        """, (row,index)->view(module,row),tenant,module,id);
    if (rows.isEmpty()) throw new ApiProblem(HttpStatus.NOT_FOUND,"MES_OPERATION_NOT_FOUND","Không tìm thấy bản ghi nghiệp vụ");
    return rows.getFirst();
  }

  private RecordView view(String module, ResultSet row) throws SQLException {
    UUID id=row.getObject(1,UUID.class);String code=row.getString(2);String title=row.getString(3);
    String tab=row.getString(4);String status=row.getString(5);String organization=row.getString(6);
    String period=row.getString(7);String correlationKey=row.getString(8);BigDecimal actual=row.getBigDecimal(9);BigDecimal target=row.getBigDecimal(10);
    String unit=row.getString(11);String owner=row.getString(12);String severity=row.getString(13);
    String source=row.getString(14);JsonNode details=read(row.getString(15));int version=row.getInt(16);
    LocalDate occurred=row.getObject(17,LocalDate.class);LocalDate due=row.getObject(18,LocalDate.class);
    Instant updated=row.getTimestamp(19).toInstant();
    Map<String,Object> data=display(module,tab,code,title,organization,period,actual,target,unit,owner,severity,source,status,occurred,due,details);
    return new RecordView(id,code,title,tab,status,statusLabel(status),organization,period,correlationKey,sourceLabel(source),severityLabel(severity),version,data,actions(status),updated);
  }

  private Map<String,Object> display(
      String module,String tab,String code,String title,String organization,String period,BigDecimal actual,
      BigDecimal target,String unit,String owner,String severity,String source,String status,
      LocalDate occurred,LocalDate due,JsonNode details) {
    var data=new LinkedHashMap<String,Object>();String actualText=measure(actual,unit);String targetText=measure(target,unit);
    String variance=actual==null||target==null?"—":measure(actual.subtract(target),unit);
    String completion=actual==null||target==null||target.signum()==0?"—":actual.multiply(BigDecimal.valueOf(100)).divide(target,1,RoundingMode.HALF_UP)+"%";
    String statusText=statusLabel(status);String date=occurred==null?"—":occurred.toString();String dueText=due==null?"—":due.toString();
    switch(module){
      case "planning-performance" -> put(data,"indicator",title,"organization",organization,"period",period,"plan",targetText,"actual",actualText,"completion",completion,"variance",variance,"status",statusText);
      case "production-operations" -> put(data,"date",date,"shift",code,"organization",organization,"location",organization,"indicator",title,"actual",actualText,"source",sourceLabel(source),"status",statusText);
      case "quality-acceptance" -> put(data,"lot",code,"product",title,"source",organization,"ash",actualText,"moisture",completion,"recovery",targetText,"result",severityLabel(severity),"status",statusText);
      case "sales-logistics" -> put(data,"order",code,"customer",organization,"product",title,"vehicle","Đang điều phối","planned",targetText,"actual",actualText,"eta",dueText,"status",statusText);
      case "materials-equipment" -> put(data,"code",code,"name",title,"organization",organization,"quantity",actualText,"threshold",targetText,"owner",owner,"updated",date,"status",statusText);
      case "occupational-safety" -> put(data,"code",code,"type",details.path("note").asText("Ghi nhận"),"location",organization,"description",title,"severity",severityLabel(severity),"owner",owner,"due",dueText,"status",statusText);
      case "finance-accounting" -> put(data,"indicator",title,"organization",organization,"period",period,"plan",targetText,"actual",actualText,"variance",variance,"updated",date,"status",statusText);
      case "workforce-labor" -> workforceDisplay(data,tab,code,title,organization,period,actual,target,actualText,targetText,variance,owner,source,statusText,date,dueText,details);
      case "investment-projects" -> put(data,"code",code,"name",title,"budget",targetText,"progress",completion,"disbursement",actualText,"milestone",dueText,"owner",owner,"status",statusText);
      case "science-digital" -> put(data,"code",code,"name",title,"type",details.path("note").asText("Nhiệm vụ"),"progress",completion,"benefit",actualText,"owner",owner,"milestone",dueText,"status",statusText);
      case "alerts-directives" -> put(data,"code",code,"type","Cảnh báo/Chỉ đạo","title",title,"source",sourceLabel(source),"severity",severityLabel(severity),"owner",owner,"due",dueText,"status",statusText);
      case "esg" -> put(data,"code",code,"pillar",organization,"indicator",title,"target",targetText,"actual",actualText,"unit",unit,"evidence",sourceLabel(source),"status",statusText);
      case "portal-tkv" -> put(data,"report",title,"period",period,"contract","Nội bộ v1","records",actualText,"validation",severityLabel(severity),"approvedBy",owner,"lastAttempt",date,"status",statusText);
      case "integration-quality" -> put(data,"source",title,"type",sourceLabel(source),"domain",organization,"lastSync",date,"records",actualText,"quality",completion,"owner",owner,"status",statusText);
      default -> put(data,"code",code,"title",title,"organization",organization,"actual",actualText,"status",statusText);
    }
    return data;
  }

  private void event(Authentication authentication,String eventType,String action,String module,UUID id,JsonNode payload){
    outbox.publish(permissions.tenantKey(authentication),eventType,"mes-operational-record",id.toString(),payload);
    audits.record(permissions.tenantKey(authentication),permissions.account(authentication),authentication.getName(),action,"MES_OPERATION",id.toString(),"SUCCESS",payload.toString());
  }
  private void workforceDisplay(Map<String,Object> data,String tab,String code,String title,String organization,String period,
      BigDecimal actualValue,BigDecimal targetValue,String actual,String target,String variance,String owner,String source,
      String status,String date,String due,JsonNode details){
    String fullName=details.path("fullName").asText(title);
    String productivity=actualValue==null||targetValue==null||targetValue.signum()==0?"—":
        actualValue.divide(targetValue,2,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    switch(tab){
      case "Danh sách nhân sự" -> put(data,"employeeCode",code,"fullName",fullName,"organization",organization,"position",details.path("position").asText(owner),"startDate",date,"endDate",due,"status",status);
      case "Hồ sơ nhân sự" -> put(data,"employeeCode",details.path("employeeCode").asText(code),"fullName",fullName,"profileType",details.path("profileType").asText("Hồ sơ lao động"),"contact",details.path("contact").asText("—"),"contractType",details.path("contractType").asText(source),"updated",date,"status",status);
      case "Chức danh và đơn vị" -> put(data,"positionCode",code,"position",details.path("position").asText(title),"organization",organization,"headcount",actual,"quota",target,"manager",owner,"status",status);
      case "Phân ca và ngày công" -> put(data,"date",date,"shift",details.path("shift").asText(code),"organization",organization,"planned",target,"present",actual,"absent",variance,"manager",owner,"status",status);
      case "Năng suất lao động" -> put(data,"organization",organization,"period",period,"output",actual,"workdays",target,"productivity",productivity,"manager",owner,"status",status);
      case "Đào tạo và chứng chỉ" -> put(data,"employeeCode",details.path("employeeCode").asText(code),"fullName",fullName,"course",details.path("course").asText(title),"issued",date,"expires",due,"owner",organization,"status",status);
      default -> put(data,"organization",organization,"headcount",target,"present",actual,"absent",variance,"output",actual,"productivity",productivity,"manager",owner,"status",status);
    }
  }

  private String next(String from,String action){
    return switch(from+":"+action){
      case "DRAFT:START","REJECTED:START" -> "IN_PROGRESS";
      case "IN_PROGRESS:SUBMIT" -> "PENDING_APPROVAL";
      case "PENDING_APPROVAL:APPROVE" -> "APPROVED";
      case "PENDING_APPROVAL:REJECT" -> "REJECTED";
      case "APPROVED:CLOSE" -> "CLOSED";
      case "CLOSED:REOPEN" -> "IN_PROGRESS";
      default -> throw new ApiProblem(HttpStatus.CONFLICT,"MES_WORKFLOW_INVALID","Thao tác "+action+" không hợp lệ khi bản ghi ở trạng thái "+statusLabel(from));
    };
  }
  private List<String> actions(String status){return switch(status){case "DRAFT","REJECTED"->List.of("START");case "IN_PROGRESS"->List.of("SUBMIT");case "PENDING_APPROVAL"->List.of("APPROVE","REJECT");case "APPROVED"->List.of("CLOSE");case "CLOSED"->List.of("REOPEN");default->List.of();};}
  private String module(String value){String result=text(value).toLowerCase(Locale.ROOT);if(!MODULES.contains(result))throw new ApiProblem(HttpStatus.NOT_FOUND,"MES_MODULE_NOT_FOUND","Phân hệ MES không tồn tại");return result;}
  private String statusFilter(String value){String result=text(value).toUpperCase(Locale.ROOT);if(!result.isEmpty()&&!STATUSES.contains(result))throw new ApiProblem(HttpStatus.BAD_REQUEST,"MES_STATUS_INVALID","Trạng thái lọc không hợp lệ");return result;}
  private String statusLabel(String value){return Map.of("DRAFT","Nháp","IN_PROGRESS","Đang xử lý","PENDING_APPROVAL","Chờ phê duyệt","APPROVED","Đã duyệt","CLOSED","Đã đóng","REJECTED","Bị trả lại").getOrDefault(value,value);}
  private String severityLabel(String value){return Map.of("LOW","Thấp","MEDIUM","Trung bình","HIGH","Cao","CRITICAL","Nghiêm trọng").getOrDefault(value,value);}
  private String sourceLabel(String value){return Map.of("MANUAL","Nhập trực tiếp","EXCEL","Excel","API","API nội bộ","DATALAKE","Data Lake").getOrDefault(value,value);}
  private String enumValue(String value,Set<String> accepted,String fallback){String result=text(value).toUpperCase(Locale.ROOT);if(result.isEmpty())return fallback;if(!accepted.contains(result))throw new ApiProblem(HttpStatus.BAD_REQUEST,"MES_ENUM_INVALID","Giá trị lựa chọn không hợp lệ");return result;}
  private String required(String value,String code,String message,int max){String result=text(value);if(result.isEmpty()||result.length()>max)throw new ApiProblem(HttpStatus.BAD_REQUEST,code,message);return result;}
  private String text(String value){return value==null?"":value.trim();}private String clean(String value){String result=text(value);return result.isEmpty()?null:result;}
  private String number(Map<String,Object> values,String key){Object value=values.get(key);return value==null?"0":new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();}
  private String measure(BigDecimal value,String unit){if(value==null)return "—";return value.stripTrailingZeros().toPlainString()+(unit==null||unit.isBlank()?"":" "+unit);}
  private JsonNode read(String value){try{return json.readTree(value);}catch(Exception error){return json.createObjectNode();}}
  private void put(Map<String,Object> target,Object... values){for(int i=0;i<values.length;i+=2)target.put(String.valueOf(values[i]),values[i+1]==null?"—":values[i+1]);}
}
