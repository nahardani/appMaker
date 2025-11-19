package com.company.appmaker.constants;

import com.company.appmaker.service.TemplateService;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import static com.company.appmaker.util.Utils.controllerEnd;
import static com.company.appmaker.util.Utils.controllerStart;

@Component
public class ControllerSkeletonFactory {

    public String create(String basePackage, String basePath, String ctrlSimple, String svcSimple) {
            // Placeholder: your real factory implementation should be used.

            return """
                   package %s.controller;

                   import org.springframework.web.bind.annotation.*;
                   import lombok.RequiredArgsConstructor;

                   @RestController
                   @RequestMapping("%s")
                   @RequiredArgsConstructor
                   public class %s {

                       private final %s %s;

                       %s
                   }
                   """.formatted(
                    basePackage, basePath, ctrlSimple, svcSimple, Character.toLowerCase(svcSimple.charAt(0)) + svcSimple.substring(1),
                    controllerStart() + "\n" + controllerEnd()
            ).trim();
        }

}
