package {{pkgBase}}.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("error", "validation");
        var errors = new ArrayList<Map<String,String>>();
        ex.getBindingResult().getFieldErrors().forEach(fe -> {
            var m = new LinkedHashMap<String,String>();
            m.put("field", fe.getField());
            m.put("message", fe.getDefaultMessage());
            errors.add(m);
        });
        body.put("details", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> handleGeneral(Exception ex) {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("error", "internal_error");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
