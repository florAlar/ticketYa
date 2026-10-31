package preferencias.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PreferenciaController {

    @GetMapping("/")
    public String ok() {
        return "microservicio preferencias ok!";
    }
}
