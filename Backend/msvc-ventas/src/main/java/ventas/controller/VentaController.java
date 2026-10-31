package ventas.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VentaController {

    @GetMapping("/")
    public String ok() {
        return "microservicio ventas ok!";
    }
}
