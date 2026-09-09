package vn.coreplatform.mes.reporting;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Ranh giới cấu hình Portal của MES.
 *
 * <p>Giai đoạn vận hành nội bộ luôn dùng {@link Mode#DISABLED}. MANUAL_TRACKING chỉ
 * ghi nhận một lần người dùng đã tự nộp trên Portal; API được giữ như trạng thái
 * tương lai và tuyệt đối chưa tạo kết nối ra ngoài khi chưa có hợp đồng tích hợp.
 */
@Component
public class MesPortalConfiguration {
  public enum Mode { DISABLED, MANUAL_TRACKING, API }

  public record Capability(
      String mode,
      String status,
      boolean manualTrackingEnabled,
      boolean automaticDispatchEnabled,
      String message) {}

  private final Mode mode;

  public MesPortalConfiguration(@Value("${mes.portal.mode:DISABLED}") String configuredMode) {
    try {
      this.mode = Mode.valueOf(configuredMode.trim().toUpperCase(Locale.ROOT));
    } catch (Exception error) {
      throw new IllegalArgumentException(
          "mes.portal.mode chỉ nhận DISABLED, MANUAL_TRACKING hoặc API", error);
    }
  }

  public Mode mode() {
    return mode;
  }

  public boolean manualTrackingEnabled() {
    return mode == Mode.MANUAL_TRACKING;
  }

  public Capability capability() {
    return switch (mode) {
      case DISABLED -> new Capability(
          mode.name(), "DISABLED", false, false,
          "Tích hợp Portal đang tạm dừng; dữ liệu đã khóa chỉ được phát hành và sử dụng nội bộ.");
      case MANUAL_TRACKING -> new Capability(
          mode.name(), "MANUAL_TRACKING", true, false,
          "MES chỉ ghi nhận việc người dùng tự nộp Portal; không tự động truyền dữ liệu.");
      case API -> new Capability(
          mode.name(), "NOT_CONFIGURED", false, false,
          "Chưa có hợp đồng API, thông tin xác thực và mapping Portal; hệ thống không gửi dữ liệu.");
    };
  }
}
