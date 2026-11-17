package preferencias.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Parámetros del motor de recomendaciones (límites y pesos configurables). */
@Component
@ConfigurationProperties(prefix = "ticketya.recomendaciones")
@Getter
@Setter
public class RecomendacionProperties {

    private int limiteSimilares = 6;
    private int limiteMe = 10;
    private int limiteMaximo = 50;
    private int candidatosMaximos = 200;
    private Pesos pesos = new Pesos();

    @Getter
    @Setter
    public static class Pesos {
        private double artista = 3.0;
        private double categoria = 2.0;
        private double ciudad = 2.0;
        private double coCompra = 1.0;
        private int coCompraTope = 5;
        private double precio = 1.0;
        private double demanda = 0.5;
    }
}
