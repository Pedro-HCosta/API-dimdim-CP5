package br.com.fiap.dimdim.dto;
import br.com.fiap.dimdim.model.*;

import jakarta.validation.constraints.*;
public record ClienteRequest(
 @NotBlank @Size(max=120) String nome,
 @NotBlank @Email @Size(max=160) String email) {}
