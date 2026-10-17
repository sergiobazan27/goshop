package shared.api;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import shared.application.AccessDeniedException;
import shared.application.BusinessConflictException;
import shared.application.BusinessException;
import shared.application.ResourceNotFoundException;

@Provider
public class BusinessExceptionMapper implements ExceptionMapper<BusinessException> {
  @Override
  public Response toResponse(BusinessException exception) {
    int status = switch (exception) {
      case AccessDeniedException _ -> 403;
      case ResourceNotFoundException _ -> 404;
      case BusinessConflictException _ -> 409;
      default -> 422;
    };
    return Response.status(status).type(MediaType.APPLICATION_JSON)
      .entity(new ApiError(exception.getCode(), exception.getMessage()))
      .build();
  }
}
