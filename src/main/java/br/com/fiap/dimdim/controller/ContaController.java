package br.com.fiap.dimdim.controller;
import br.com.fiap.dimdim.dto.*;
import br.com.fiap.dimdim.service.CadastroService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
@RestController @RequestMapping("/api/contas")
public class ContaController {
 private final CadastroService service;
 public ContaController(CadastroService service) { this.service=service; }
 @GetMapping public Page<ContaResponse> listar(@PageableDefault(size=20,sort="id") Pageable p) { return service.listarContas(p); }
 @GetMapping("/{id}") public ContaResponse buscar(@PathVariable Long id) { return service.buscarConta(id); }
 @PostMapping public ResponseEntity<ContaResponse> criar(@Valid @RequestBody ContaRequest r) {
  var result=service.criarConta(r);
  var uri=ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(result.id()).toUri();
  return ResponseEntity.created(uri).body(result);
 }
 @PutMapping("/{id}") public ContaResponse atualizar(@PathVariable Long id,@Valid @RequestBody ContaRequest r) { return service.atualizarConta(id,r); }
 @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(@PathVariable Long id) { service.excluirConta(id);return ResponseEntity.noContent().build(); }
}
