package br.com.fiap.dimdim.dto;
import br.com.fiap.dimdim.model.*;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record ContaRequest(
 @NotBlank @Pattern(regexp="[0-9]{4,20}") String numero,
 @NotNull TipoConta tipo,
 @NotNull @DecimalMin("0.00") @Digits(integer=13,fraction=2) BigDecimal saldo,
 @NotNull @Positive Long clienteId) {}
