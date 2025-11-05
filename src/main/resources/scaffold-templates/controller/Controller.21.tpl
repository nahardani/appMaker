package {{pkgBase}}.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("{{basePath:/api}}")
public class {{controllerName}} {
    @GetMapping("/health")
    public ResponseEntity<String> health() { return ResponseEntity.ok("OK"); }
}
