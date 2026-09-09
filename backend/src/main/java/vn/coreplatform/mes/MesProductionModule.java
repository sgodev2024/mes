package vn.coreplatform.mes;

import java.util.List;
import org.springframework.stereotype.Component;
import vn.coreplatform.kernel.ModuleContributor;
import vn.coreplatform.kernel.ModuleDescriptor;
import vn.coreplatform.kernel.NavigationItemDescriptor;

/**
 * Canonical navigation contribution for the MES project. The eighteen entries and
 * their order are the approved long-lived information architecture contract.
 * MES pages are discovered through MES_REPORT:READ instead of an administrator-only
 * authority so preparer/approver/viewer roles can reach the same authorized workspace.
 */
@Component
public class MesProductionModule implements ModuleContributor {
  private static final String WORKSPACE = "business";

  @Override
  public ModuleDescriptor descriptor() {
    return new ModuleDescriptor(
        "mes-production",
        "MES Than Mạo Khê",
        "1.4.0",
        List.of("kernel", "permission"),
        List.of("mes-navigation", "mes-frontend", "mes-daily-reporting", "mes-xlsx-import",
            "mes-canonical-internal-data", "mes-master-data-stewardship",
            "mes-inventory-reconciliation", "mes-canonical-executive-dashboard", "mes-portal-boundary",
            "mes-navigation-baseline-v1"),
        "MES V1 theo baseline 18 phân hệ; dữ liệu nội bộ canonical và ranh giới Portal/TKV an toàn",
        ">=1.1.0 <2.0.0");
  }

  @Override
  public List<NavigationItemDescriptor> navigationItems() {
    return List.of(
        page("dashboard", "Dashboard điều hành", "layout-dashboard", "mes-dashboard", "/mes/dashboard", 101),
        page("planning-performance", "Kế hoạch & hiệu quả", "calendar", "mes-planning-performance", "/mes/planning-performance", 102),
        page("production-operations", "Sản xuất & điều hành", "factory", "mes-production-operations", "/mes/production-operations", 103),
        page("coal-flow", "Kho & dòng than", "warehouse", "mes-coal-flow", "/mes/coal-flow", 104),
        page("quality-acceptance", "Chất lượng & nghiệm thu", "flask", "mes-quality-acceptance", "/mes/quality-acceptance", 105),
        page("sales-logistics", "Tiêu thụ & giao vận", "truck", "mes-sales-logistics", "/mes/sales-logistics", 106),
        page("materials-equipment", "Vật tư & thiết bị", "settings", "mes-materials-equipment", "/mes/materials-equipment", 107),
        page("occupational-safety", "An toàn lao động", "shield", "mes-occupational-safety", "/mes/occupational-safety", 108),
        page("finance-accounting", "Tài chính & Kế toán", "chart-combined", "mes-finance-accounting", "/mes/finance-accounting", 109),
        page("workforce-labor", "Nhân sự & lao động", "users", "mes-workforce-labor", "/mes/workforce-labor", 110),
        page("investment-projects", "Đầu tư & dự án", "folder", "mes-investment-projects", "/mes/investment-projects", 111),
        page("science-digital", "KHCN & Chuyển đổi số", "cpu", "mes-science-digital", "/mes/science-digital", 112),
        page("alerts-directives", "Cảnh báo & chỉ đạo", "bell", "mes-alerts-directives", "/mes/alerts-directives", 113),
        page("esg", "ESG", "leaf", "mes-esg", "/mes/esg", 114),
        page("reporting-center", "Trung tâm báo cáo", "file-input", "mes-reporting-center", "/mes/reporting-center", 115),
        page("portal-tkv", "Portal/TKV", "webhook", "mes-portal-tkv", "/mes/portal-tkv", 116),
        page("data-mapping", "Danh mục & ánh xạ dữ liệu", "database", "mes-data-mapping", "/mes/data-mapping", 117),
        page("integration-quality", "Tích hợp & chất lượng dữ liệu", "server", "mes-integration-quality", "/mes/integration-quality", 118));
  }

  private static NavigationItemDescriptor page(
      String key, String label, String icon, String view, String route, int order) {
    return new NavigationItemDescriptor(
        "module.mes-production." + key, WORKSPACE, "", label,
        "navigation.module.mes-production." + key, icon, view, route, order,
        "", "MES_REPORT", "READ", List.of("mes", "sản xuất", label.toLowerCase()));
  }
}
