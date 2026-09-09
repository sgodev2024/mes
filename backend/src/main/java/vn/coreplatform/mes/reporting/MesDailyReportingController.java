package vn.coreplatform.mes.reporting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.coreplatform.permission.RequirePermission;

/** API Slice 1 — Trung tâm báo cáo ngày. */
@RestController
@RequestMapping("/api/v1/mes")
public class MesDailyReportingController {
  private final MesDailyReportingService service;

  public MesDailyReportingController(MesDailyReportingService service) {
    this.service = service;
  }

  public record ReasonRequest(@Size(min = 3, max = 500) String reason) {}
  public record PortalSubmissionRequest(@Size(max = 160) String receiptCode, @Size(max = 1000) String note) {}

  @GetMapping("/templates")
  @RequirePermission(resource = "MES_REPORT", action = "READ")
  List<MesDailyReportingService.TemplateView> templates(Authentication authentication) {
    return service.templates(authentication);
  }

  @GetMapping("/integrations/portal/status")
  @RequirePermission(resource = "MES_REPORT", action = "READ")
  MesPortalConfiguration.Capability portalStatus() {
    return service.portalCapability();
  }

  @GetMapping("/reporting-calendar")
  @RequirePermission(resource = "MES_REPORT", action = "READ")
  MesDailyReportingService.PageResult<MesDailyReportingService.CalendarItemView> calendar(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      Authentication authentication) {
    return service.calendar(authentication, date, status, page, size);
  }

  @GetMapping("/reporting-calendar/summary")
  @RequirePermission(resource = "MES_REPORT", action = "READ")
  MesDailyReportingService.CalendarSummary summary(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      Authentication authentication) {
    return service.summary(authentication, date);
  }

  @PostMapping(value = "/import-batches", consumes = "multipart/form-data")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @RequirePermission(resource = "MES_REPORT", action = "IMPORT")
  MesDailyReportingService.BatchView upload(
      @RequestPart("file") MultipartFile file,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportingDate,
      Authentication authentication) throws Exception {
    return service.upload(authentication, file, reportingDate);
  }

  @GetMapping("/import-batches")
  @RequirePermission(resource = "MES_REPORT", action = "READ")
  MesDailyReportingService.PageResult<MesDailyReportingService.BatchView> recentBatches(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      Authentication authentication) {
    return service.recentBatches(authentication, page, size);
  }

  @GetMapping("/import-batches/{id}")
  @RequirePermission(resource = "MES_REPORT", action = "READ")
  MesDailyReportingService.BatchView batch(@PathVariable UUID id, Authentication authentication) {
    return service.batch(authentication, id);
  }

  @GetMapping("/import-batches/{id}/records")
  @RequirePermission(resource = "MES_REPORT", action = "READ")
  MesDailyReportingService.PageResult<MesDailyReportingService.ImportRecordView> records(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      Authentication authentication) {
    return service.records(authentication, id, page, size);
  }

  @GetMapping("/import-batches/{id}/issues")
  @RequirePermission(resource = "MES_REPORT", action = "READ")
  List<MesDailyReportingService.IssueView> issues(@PathVariable UUID id, Authentication authentication) {
    return service.issues(authentication, id);
  }

  @PostMapping("/import-batches/{id}/confirm")
  @RequirePermission(resource = "MES_REPORT", action = "CONFIRM")
  MesDailyReportingService.BatchView confirm(@PathVariable UUID id, Authentication authentication) {
    return service.confirm(authentication, id);
  }

  @PostMapping("/import-batches/{id}/approve")
  @RequirePermission(resource = "MES_REPORT", action = "APPROVE")
  MesDailyReportingService.BatchView approve(@PathVariable UUID id, Authentication authentication) {
    return service.approve(authentication, id);
  }

  @PostMapping("/import-batches/{id}/lock")
  @RequirePermission(resource = "MES_REPORT", action = "APPROVE")
  MesDailyReportingService.BatchView lock(@PathVariable UUID id, Authentication authentication) {
    return service.lock(authentication, id);
  }

  @PostMapping("/import-batches/{id}/reject")
  @RequirePermission(resource = "MES_REPORT", action = "APPROVE")
  MesDailyReportingService.BatchView reject(
      @PathVariable UUID id, @Valid @RequestBody ReasonRequest request, Authentication authentication) {
    return service.reject(authentication, id, request.reason());
  }

  @PostMapping("/import-batches/{id}/portal-submissions")
  @ResponseStatus(HttpStatus.CREATED)
  @RequirePermission(resource = "MES_REPORT", action = "SUBMIT_PORTAL")
  MesDailyReportingService.PortalSubmissionView submitPortal(
      @PathVariable UUID id,
      @Valid @RequestBody PortalSubmissionRequest request,
      Authentication authentication) {
    return service.submitPortal(authentication, id, request.receiptCode(), request.note());
  }
}
