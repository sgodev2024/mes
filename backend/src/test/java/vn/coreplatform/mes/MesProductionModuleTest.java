package vn.coreplatform.mes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import vn.coreplatform.kernel.KernelModule;

class MesProductionModuleTest {
  private final MesProductionModule module = new MesProductionModule();

  @Test
  void contributesApprovedMesMenuWithoutReplacingCoreWorkspaces() {
    assertThat(module.descriptor().key()).isEqualTo("mes-production");
    assertThat(module.navigationWorkspaces()).isEmpty();
    assertThat(module.navigationItems()).hasSize(18);
    assertThat(module.navigationItems()).allMatch(item -> !item.group() && item.parentKey().isBlank());
    assertThat(module.navigationItems()).allMatch(item -> item.requiredAuthority().isBlank()
        && item.permissionResource().equals("MES_REPORT") && item.permissionAction().equals("READ"));
    assertThat(module.navigationItems().stream().map(item -> item.viewKey()))
        .containsExactly("mes-dashboard", "mes-planning-performance", "mes-production-operations", "mes-coal-flow",
            "mes-quality-acceptance", "mes-sales-logistics", "mes-materials-equipment", "mes-occupational-safety",
            "mes-finance-accounting", "mes-workforce-labor", "mes-investment-projects", "mes-science-digital",
            "mes-alerts-directives", "mes-esg", "mes-reporting-center", "mes-portal-tkv", "mes-data-mapping",
            "mes-integration-quality");
    assertThat(module.navigationItems().stream().map(item -> item.label()))
        .containsExactly("Dashboard điều hành", "Kế hoạch & hiệu quả", "Sản xuất & điều hành", "Kho & dòng than",
            "Chất lượng & nghiệm thu", "Tiêu thụ & giao vận", "Vật tư & thiết bị", "An toàn lao động",
            "Tài chính & Kế toán", "Nhân sự & lao động", "Đầu tư & dự án", "KHCN & Chuyển đổi số",
            "Cảnh báo & chỉ đạo", "ESG", "Trung tâm báo cáo", "Portal/TKV", "Danh mục & ánh xạ dữ liệu",
            "Tích hợp & chất lượng dữ liệu");
    assertThat(module.navigationItems().stream().map(item -> item.route())).doesNotHaveDuplicates();
    assertThat(module.navigationItems().stream().map(item -> item.sortOrder()))
        .containsExactlyElementsOf(IntStream.rangeClosed(101, 118).boxed().toList());
    assertThat(module.navigationItems()).allMatch(item -> item.workspaceKey().equals("business"));
  }

  @Test
  void preservesApprovedTopLevelWorkspaceOrder() {
    assertThat(new KernelModule().navigationWorkspaces().stream().map(workspace -> workspace.label()))
        .containsExactly("Trang chủ", "MES", "Quản trị hệ thống");
  }
}
