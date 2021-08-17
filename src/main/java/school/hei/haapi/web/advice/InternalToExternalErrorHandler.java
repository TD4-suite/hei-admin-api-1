package school.hei.haapi.web.advice;

import javax.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import school.hei.haapi.exception.BadRequestException;
import school.hei.haapi.exception.ForbiddenException;
import school.hei.haapi.exception.NotFoundException;
import school.hei.haapi.exception.NotImplementedException;
import school.hei.haapi.exception.TooManyRequestsException;
import school.hei.haapi.mapper.ErrorMapper;
import school.hei.haapi.web.model.ErrorResource;

@RestControllerAdvice
@Slf4j
public class InternalToExternalErrorHandler {

  private final ErrorMapper errorMapper;

  public InternalToExternalErrorHandler(ErrorMapper errorMapper) {
    this.errorMapper = errorMapper;
  }

  @ExceptionHandler(value = {BadRequestException.class})
  ResponseEntity<ErrorResource> handleBadRequest(BadRequestException e) {
    log.info("Bad request", e);
    return new ResponseEntity<>(errorMapper.mapToExternal(e), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(value = {MissingServletRequestParameterException.class})
  ResponseEntity<ErrorResource> handleBadRequest(MissingServletRequestParameterException e) {
    log.info("Missing parameter", e);
    return handleBadRequest(new BadRequestException(e.getMessage()));
  }

  @ExceptionHandler(value = {MethodArgumentTypeMismatchException.class})
  ResponseEntity<ErrorResource> handleConversionFailed(MethodArgumentTypeMismatchException e) {
    log.info("Conversion failed", e);
    String message = e.getCause().getCause().getMessage();
    return handleBadRequest(new BadRequestException(message));
  }

  @ExceptionHandler(value = {TooManyRequestsException.class})
  ResponseEntity<ErrorResource> handleTooManyRequests(TooManyRequestsException e) {
    log.info("Too many requests", e);
    return new ResponseEntity<>(errorMapper.mapToExternal(e), HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(
      value = {
        LockAcquisitionException.class,
        CannotAcquireLockException.class,
        OptimisticLockException.class
      })
  ResponseEntity<ErrorResource> handleLockAcquisitionException(Exception e) {
    log.warn("Database lock could not be acquired: too many requests assumed", e);
    return handleTooManyRequests(new TooManyRequestsException(e));
  }

  @ExceptionHandler(value = {BadCredentialsException.class, ForbiddenException.class})
  ResponseEntity<ErrorResource> handleForbidden(Exception e) {
    /* _not_ HttpsStatus.UNAUTHORIZED because, counter-intuitively, it's just for authentication
     * https://stackoverflow.com/questions/3297048/403-forbidden-vs-401-unauthorized-http-responses */
    log.info("Forbidden", e);
    return new ResponseEntity<>(
        new ErrorResource(ErrorResource.Type.FORBIDDEN, e.getMessage()), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(value = {NotFoundException.class})
  ResponseEntity<ErrorResource> handleNotFound(NotFoundException e) {
    log.info("Not found", e);
    return new ResponseEntity<>(errorMapper.mapToExternal(e), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(value = {NotImplementedException.class})
  ResponseEntity<ErrorResource> handleNotImplemented(NotImplementedException e) {
    log.error("Not implemented", e);
    return new ResponseEntity<>(errorMapper.mapToExternal(e), HttpStatus.NOT_IMPLEMENTED);
  }

  @ExceptionHandler(value = {Exception.class})
  ResponseEntity<ErrorResource> handleDefault(Exception e) {
    log.error("Internal error", e);
    return new ResponseEntity<>(errorMapper.mapToExternal(e), HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
