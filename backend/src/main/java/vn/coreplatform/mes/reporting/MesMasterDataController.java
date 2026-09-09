package vn.coreplatform.mes.reporting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.coreplatform.permission.RequirePermission;

@RestController
@RequestMapping("/api/v1/mes/master-data")
public class MesMasterDataController {
  private final MesMasterDataService service;

  public MesMasterDataController(MesMasterDataService service) { this.service = service; }

  public record ProductUpdateRequest(
      @NotBlank @Size(max = 100) String code,
      @NotBlank @Size(max = 240) String name,
      @NotBlank @Size(max = 40) String unitCode,
      @NotBlank @Pattern(regexp = "UNVERIFIED|RESOLVED") String resolutionStatus,
      @Min(1) int expectedVersion) {}

  public record PartnerUpdateRequest(
      @Size(max = 100) String code,
      @NotBlank @Size(max = 240) String name,
      @NotBlank @Pattern(regexp = "UNVERIFIED|RESOLVED") String resolutionStatus,
      @Min(1) int expectedVersion) {}

  @GetMapping("/products")
  @RequirePermission(resource = "MES_MASTER_DATA", action = "READ")
  MesMasterDataService.PageResult<MesMasterDataService.ProductView> products(
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "") String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      Authentication authentication) {
    return service.products(authentication, status, search, page, size);
  }

  @PatchMapping("/products/{id}")
  @RequirePermission(resource = "MES_MASTER_DATA", action = "APPROVE")
  MesMasterDataService.ProductView updateProduct(
      @PathVariable UUID id, @Valid @RequestBody ProductUpdateRequest input,
      Authentication authentication) {
    return service.updateProduct(authentication, id, new MesMasterDataService.ProductUpdate(
        input.code(), input.name(), input.unitCode(), input.resolutionStatus(), input.expectedVersion()));
  }

  @GetMapping("/partners")
  @RequirePermission(resource = "MES_MASTER_DATA", action = "READ")
  MesMasterDataService.PageResult<MesMasterDataService.PartnerView> partners(
      @RequestParam(defaultValue = "") String type,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "") String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      Authentication authentication) {
    return service.partners(authentication, type, status, search, page, size);
  }

  @PatchMapping("/partners/{id}")
  @RequirePermission(resource = "MES_MASTER_DATA", action = "APPROVE")
  MesMasterDataService.PartnerView updatePartner(
      @PathVariable UUID id, @Valid @RequestBody PartnerUpdateRequest input,
      Authentication authentication) {
    return service.updatePartner(authentication, id, new MesMasterDataService.PartnerUpdate(
        input.code(), input.name(), input.resolutionStatus(), input.expectedVersion()));
  }
}
