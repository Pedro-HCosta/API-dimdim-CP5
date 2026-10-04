package br.com.fiap.dimdim.controller;
import br.com.fiap.dimdim.dto.*;
import br.com.fiap.dimdim.service.CadastroService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
@RestController @RequestMapping("/api/clientes")
public class ClienteController {
 private final CadastroService service;
 public ClienteController(CadastroService service) { this.service=service; }
 @GetMapping public Page<ClienteResponse> listar(@PageableDefault(size=20,sort="id") Pageable p) { return service.listarClientes(p); }
 @GetMapping("/{id}") public ClienteResponse buscar(@PathVariable Long id) { return service.buscarCliente(id); }
 @PostMapping public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest r) {
  var result=service.criarCliente(r);
  var uri=ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(result.id()).toUri();
  return ResponseEntity.created(uri).body(result);
 }
 @PutMapping("/{id}") public ClienteResponse atualizar(@PathVariable Long id,@Valid @RequestBody ClienteRequest r) { return service.atualizarCliente(id,r); }
 @DeleteMapping("/{id}") public ResponseEntity<Void> excluir(@PathVariable Long id) { service.excluirCliente(id);return ResponseEntity.noContent().build(); }
}
