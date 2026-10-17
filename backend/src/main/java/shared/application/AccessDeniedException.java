package shared.application;

public final class AccessDeniedException extends BusinessException {
  public AccessDeniedException(String code, String message) {
    super(code, message);
  }
}
