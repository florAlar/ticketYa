package ventas.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class NivelCupoTest {

    @Test
    void fromDisponible_cero_esAgotado() {
        assertEquals("AGOTADO", NivelCupo.fromDisponible(0));
    }

    @Test
    void fromDisponible_menosDe20_esUltimosLugares() {
        assertEquals("ULTIMOS_LUGARES", NivelCupo.fromDisponible(1));
        assertEquals("ULTIMOS_LUGARES", NivelCupo.fromDisponible(19));
    }

    @Test
    void fromDisponible_20a49_esMenosDe50() {
        assertEquals("MENOS_DE_50", NivelCupo.fromDisponible(20));
        assertEquals("MENOS_DE_50", NivelCupo.fromDisponible(49));
    }

    @Test
    void fromDisponible_50a99_esMenosDe100() {
        assertEquals("MENOS_DE_100", NivelCupo.fromDisponible(50));
        assertEquals("MENOS_DE_100", NivelCupo.fromDisponible(99));
    }

    @Test
    void fromDisponible_100oMas_esOK() {
        assertEquals("OK", NivelCupo.fromDisponible(100));
        assertEquals("OK", NivelCupo.fromDisponible(1000));
    }

    @Test
    void label_porNivel() {
        assertEquals("Agotado", NivelCupo.label("AGOTADO"));
        assertEquals("Últimos lugares", NivelCupo.label("ULTIMOS_LUGARES"));
        assertEquals("Menos de 50", NivelCupo.label("MENOS_DE_50"));
        assertEquals("Menos de 100", NivelCupo.label("MENOS_DE_100"));
        assertNull(NivelCupo.label("OK"));
    }

    @Test
    void label_nuloODesconocido_esNulo() {
        assertNull(NivelCupo.label(null));
        assertNull(NivelCupo.label("DESCONOCIDO"));
    }

    @Test
    void isAgotado_porNivel() {
        assertTrue(NivelCupo.isAgotado("AGOTADO"));
        assertFalse(NivelCupo.isAgotado("OK"));
        assertFalse(NivelCupo.isAgotado("ULTIMOS_LUGARES"));
        assertFalse(NivelCupo.isAgotado(null));
    }
}
