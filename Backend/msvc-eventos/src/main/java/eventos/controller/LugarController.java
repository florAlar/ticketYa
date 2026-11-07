package eventos.controller;

import eventos.dto.AsientoResponseDTO;
import eventos.dto.LugarRequestDTO;
import eventos.dto.LugarResponseDTO;
import eventos.dto.SectorEventoDTO;
import eventos.dto.SectorRequestDTO;
import eventos.exception.ForbiddenException;
import eventos.service.EventoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LugarController {

    private final EventoService eventoService;

    public LugarController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping("/lugares")
    public List<LugarResponseDTO> listar(@RequestParam(required = false) String ciudad) {
        return eventoService.listarLugares(ciudad);
    }

    @GetMapping("/lugares/{id}")
    public LugarResponseDTO uno(@PathVariable String id) {
        return eventoService.obtenerLugar(id);
    }

    @PostMapping("/lugares")
    public ResponseEntity<LugarResponseDTO> crear(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody LugarRequestDTO body) {
        requireOrgOrAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crearLugar(body));
    }

    @PutMapping("/lugares/{id}")
    public LugarResponseDTO actualizar(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String id,
            @RequestBody LugarRequestDTO body) {
        requireOrgOrAdmin(role);
        return eventoService.actualizarLugar(id, body);
    }

    @PostMapping("/lugares/{id}/sectores")
    public ResponseEntity<SectorEventoDTO> crearSector(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String id,
            @RequestBody SectorRequestDTO body) {
        requireOrgOrAdmin(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crearSector(id, body));
    }

    @PutMapping("/sectores/{id}")
    public SectorEventoDTO actualizarSector(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable String id,
            @RequestBody SectorRequestDTO body) {
        requireOrgOrAdmin(role);
        return eventoService.actualizarSector(id, body);
    }

    @GetMapping("/lugares/{id}/sectores/{sectorId}/asientos")
    public List<AsientoResponseDTO> asientos(@PathVariable String id, @PathVariable String sectorId) {
        return eventoService.listarAsientos(id, sectorId);
    }

    private static void requireOrgOrAdmin(String role) {
        if (role == null) {
            throw new ForbiddenException("Se requiere rol ORGANIZADOR o ADMIN");
        }
        String r = role.trim().toUpperCase();
        if (!"ORGANIZADOR".equals(r) && !"ADMIN".equals(r)) {
            throw new ForbiddenException("Se requiere rol ORGANIZADOR o ADMIN");
        }
    }
}
