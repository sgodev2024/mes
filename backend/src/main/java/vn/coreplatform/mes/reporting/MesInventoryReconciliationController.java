package vn.coreplatform.mes.reporting;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.coreplatform.permission.RequirePermission;

@RestController
@RequestMapping("/api/v1/mes/reconciliation")
public class MesInventoryReconciliationController {
  private final MesInventoryReconciliationService service;

  public MesInventoryReconciliationController(MesInventoryReconciliationService service) {
    this.service = service;
  }

  @GetMapping("/inventory")
  @RequirePermission(resource = "MES_REPORT", action = "READ")
  MesInventoryReconciliationService.ReconciliationView inventory(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      Authentication authentication) {
    return service.reconcile(authentication, date, page, size);
  }
}
