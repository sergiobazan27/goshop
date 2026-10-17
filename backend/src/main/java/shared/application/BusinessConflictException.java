package shared.application;

public final class BusinessConflictException extends BusinessException {
  public BusinessConflictException(String code, String message) {
    super(code, message);
  }
}
