package identity.infrastructure.keycloak;

public class IdentityProviderException extends RuntimeException {
  public IdentityProviderException(Throwable cause) {
    super("No se pudo consultar el proveedor de identidad.", cause);
  }
}
