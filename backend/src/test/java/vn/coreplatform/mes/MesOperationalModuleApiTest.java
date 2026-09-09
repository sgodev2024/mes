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
}
