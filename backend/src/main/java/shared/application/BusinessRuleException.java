package shared.application;

public final class BusinessRuleException extends BusinessException {
  public BusinessRuleException(String code, String message) {
    super(code, message);
  }
}
