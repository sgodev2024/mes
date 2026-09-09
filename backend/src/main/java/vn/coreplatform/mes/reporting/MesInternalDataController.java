package vn.coreplatform.mes.reporting;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.coreplatform.permission.RequirePermission;

/** API read model cho dữ liệu MES đã phát hành nội bộ. */
@RestController
@RequestMapping("/api/v1/mes/internal-data")
public class MesInternalDataController {
  private final MesCanonicalDataService service;

  public MesInternalDataController(MesCanonicalDataService service) {
    this.service = service;
  }

  @GetMapping("/summary")
  @RequirePermission(resource = "MES_REPORT", action = "READ")
  MesCanonicalDataService.InternalDataSummary summary(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      Authentication authentication) {
    return service.summary(authentication, from, to);
  }

  @GetMapping("/metrics")
  @RequirePermission(resource = "MES_REPORT", action = "READ")
  MesCanonicalDataService.PageResult<MesCanonicalDataService.InternalMetricView> metrics(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "") String domain,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      Authentication authentication) {
    return service.metrics(authentication, from, to, domain, page, size);
  }
}
