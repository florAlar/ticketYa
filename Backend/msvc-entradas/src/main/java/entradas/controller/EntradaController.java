package entradas.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EntradaController {

    @GetMapping("/")
    public String ok() {
        return "microservicio entradas ok!";
    }
}
