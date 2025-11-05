package ${basePackage}.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
    var errors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.groupingBy(
            fe -> fe.getField(),
            Collectors.mapping(fe -> fe.getDefaultMessage(), Collectors.toList())
        ));
    return ResponseEntity.badRequest().body(Map.of("errors", errors));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> generic(Exception ex) {
    return ResponseEntity.internalServerError().body(Map.of("message", ex.getMessage()));
  }
}
