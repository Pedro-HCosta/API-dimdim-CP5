package br.com.fiap.dimdim.dto;
import br.com.fiap.dimdim.model.*;

import java.math.BigDecimal;
public record ContaResponse(Long id,String numero,TipoConta tipo,BigDecimal saldo,Long clienteId) {
 public static ContaResponse of(Conta c) { return new ContaResponse(c.getId(),c.getNumero(),c.getTipo(),c.getSaldo(),c.getCliente().getId()); }
}
