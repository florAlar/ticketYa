package preferencias;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsvcPreferenciasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsvcPreferenciasApplication.class, args);
    }

}
