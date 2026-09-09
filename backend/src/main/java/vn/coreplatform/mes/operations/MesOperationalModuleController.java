package vn.coreplatform.mes.operations;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.coreplatform.permission.RequirePermission;

@RestController
@RequestMapping("/api/v1/mes/modules")
public class MesOperationalModuleController {
  private final MesOperationalModuleService service;
  public MesOperationalModuleController(MesOperationalModuleService service){this.service=service;}

  public record CreateRequest(
      @NotBlank @Size(max=120) String tab,
      @NotBlank @Size(min=3,max=300) String title,
      @NotBlank @Size(max=80) String organization,
      @NotBlank @Size(max=30) String period,
      @DecimalMin("0") BigDecimal value,
      @Size(max=40) String unit,
      @Size(max=160) String owner,
      @Pattern(regexp="LOW|MEDIUM|HIGH|CRITICAL") String severity,
      @Pattern(regexp="MANUAL|EXCEL|API|DATALAKE") String source,
      @Size(max=1000) String note) {}
  public record WorkflowRequest(
      @NotBlank @Pattern(regexp="START|SUBMIT|APPROVE|REJECT|CLOSE|REOPEN") String action,
      @Min(1) int expectedVersion) {}

  @GetMapping("/{module}/overview")
  @RequirePermission(resource="MES_OPERATION",action="READ")
  MesOperationalModuleService.Overview overview(
      @PathVariable String module,@RequestParam String tab,
      @RequestParam(defaultValue="") String search,@RequestParam(defaultValue="") String status,
      Authentication authentication){return service.overview(authentication,module,tab,search,status);}

  @PostMapping("/{module}/records")
  @ResponseStatus(HttpStatus.CREATED)
  @RequirePermission(resource="MES_OPERATION",action="CREATE")
  MesOperationalModuleService.RecordView create(
      @PathVariable String module,@Valid @RequestBody CreateRequest input,Authentication authentication){
    return service.create(authentication,module,new MesOperationalModuleService.CreateInput(
        input.tab(),input.title(),input.organization(),input.period(),input.value(),input.unit(),
        input.owner(),input.severity(),input.source(),input.note()));
  }

  @PatchMapping("/{module}/records/{id}/workflow")
  @RequirePermission(resource="MES_OPERATION",action="UPDATE")
  MesOperationalModuleService.RecordView transition(
      @PathVariable String module,@PathVariable UUID id,@Valid @RequestBody WorkflowRequest input,
      Authentication authentication){return service.transition(authentication,module,id,input.action(),input.expectedVersion());}
}
