package br.com.fiap.dimdim.dto;
import br.com.fiap.dimdim.model.*;

public record ClienteResponse(Long id,String nome,String email) {
 public static ClienteResponse of(Cliente c) { return new ClienteResponse(c.getId(),c.getNome(),c.getEmail()); }
}
