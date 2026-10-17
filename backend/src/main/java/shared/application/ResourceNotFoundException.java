package shared.application;

public final class ResourceNotFoundException extends BusinessException {
  public ResourceNotFoundException(String code, String message) {
    super(code, message);
  }
}
