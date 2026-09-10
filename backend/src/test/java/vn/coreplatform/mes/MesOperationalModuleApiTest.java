package vn.coreplatform.mes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import vn.coreplatform.AbstractApiTest;

class MesOperationalModuleApiTest extends AbstractApiTest {

  @Test
  void readsSeedDataAndRunsAuditedWorkflowWithOptimisticLocking() throws Exception {
    var admin = adminToken();

    mvc.perform(get("/api/v1/mes/modules/occupational-safety/overview")
            .param("tab", "Tổng quan").with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.module").value("occupational-safety"))
        .andExpect(jsonPath("$.tab").value("Tổng quan"))
        .andExpect(jsonPath("$.kpis.length()").value(4))
        .andExpect(jsonPath("$.trend.length()").value(7))
        .andExpect(jsonPath("$.items.length()").value(3))
        .andExpect(jsonPath("$.items[0].correlationKey").isNotEmpty());

    var createdBody = mvc.perform(post("/api/v1/mes/modules/occupational-safety/records")
            .contentType(APPLICATION_JSON).with(bearer(admin)).content("""
                {"tab":"Tổng quan","title":"Kiểm tra an toàn đầu ca","organization":"PX-KT1",
                 "period":"09/2026","value":82,"unit":"PERCENT","owner":"Phòng An toàn",
                 "severity":"HIGH","source":"MANUAL","note":"Kiểm thử workflow basic runtime"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.statusCode").value("DRAFT"))
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.correlationKey").isNotEmpty())
        .andReturn().getResponse().getContentAsString();
    var created = json.readTree(createdBody);
    var id = created.path("id").asText();

    mvc.perform(patch("/api/v1/mes/modules/occupational-safety/records/{id}/workflow", id)
            .contentType(APPLICATION_JSON).with(bearer(admin))
            .content("{\"action\":\"START\",\"expectedVersion\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.statusCode").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.version").value(2));

    mvc.perform(patch("/api/v1/mes/modules/occupational-safety/records/{id}/workflow", id)
            .contentType(APPLICATION_JSON).with(bearer(admin))
            .content("{\"action\":\"SUBMIT\",\"expectedVersion\":1}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("MES_OPERATION_VERSION_CONFLICT"));

    assertThat(jdbc.queryForObject(
        "select count(*) from audit.event where resource_type='MES_OPERATION' and resource_id=?",
        Integer.class, id)).isGreaterThanOrEqualTo(2);
    assertThat(jdbc.queryForObject(
        "select count(*) from async.outbox_event where aggregate_type='mes-operational-record' and aggregate_id=?",
        Integer.class, id)).isGreaterThanOrEqualTo(2);
  }

  @Test
  void exposesAllWorkforceLaborSlicesWithTabSpecificData() throws Exception {
    var admin = adminToken();
    mvc.perform(get("/api/v1/mes/modules/workforce-labor/overview")
            .param("tab", "Danh sách nhân sự").with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(3))
        .andExpect(jsonPath("$.items[0].data.employeeCode").isNotEmpty())
        .andExpect(jsonPath("$.items[0].data.fullName").isNotEmpty());
    mvc.perform(get("/api/v1/mes/modules/workforce-labor/overview")
            .param("tab", "Đào tạo và chứng chỉ").with(bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(3))
        .andExpect(jsonPath("$.items[0].data.course").isNotEmpty());
  }

  @Test
  void createsWorkforceRecordsWithTabSpecificContractAndValidation() throws Exception {
    var admin = adminToken();
    String employeeCode = jdbc.queryForObject("""
        select record_code from mes.operational_record
        where module_code='workforce-labor' and tab_name='Danh sách nhân sự'
        order by record_code limit 1
        """, String.class);

    mvc.perform(post("/api/v1/mes/modules/workforce-labor/records")
            .contentType(APPLICATION_JSON).with(bearer(admin)).content("""
                {"tab":"Hồ sơ nhân sự","title":"Nguyễn Văn Hùng","organization":"PX-KT1",
                 "period":"09/2026","value":1,"target":1,"unit":"PROFILE","owner":"KỸ SƯ KHAI THÁC",
                 "severity":"LOW","source":"MANUAL","occurredOn":"2026-09-10",
                 "details":{"employeeCode":"%s","fullName":"Nguyễn Văn Hùng",
                   "profileType":"Hồ sơ lao động","contractType":"Không xác định thời hạn","contact":"0900000000"}}
                """.formatted(employeeCode)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.startsWith("HS-")))
        .andExpect(jsonPath("$.data.employeeCode").value(employeeCode))
        .andExpect(jsonPath("$.data.profileType").value("Hồ sơ lao động"))
        .andExpect(jsonPath("$.data.contact").value("0900000000"));

    mvc.perform(post("/api/v1/mes/modules/workforce-labor/records")
            .contentType(APPLICATION_JSON).with(bearer(admin)).content("""
                {"tab":"Phân ca và ngày công","title":"Ca 1 · PX-KT1","organization":"PX-KT1",
                 "period":"09/2026","value":12,"target":10,"unit":"PERSON","owner":"Quản đốc",
                 "severity":"LOW","source":"MANUAL","occurredOn":"2026-09-10","details":{"shift":"Ca 1"}}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MES_WORKFORCE_PRESENT_INVALID"));
  }
}
