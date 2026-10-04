package br.com.fiap.dimdim.service;
import br.com.fiap.dimdim.model.*;
import br.com.fiap.dimdim.dto.*;
import br.com.fiap.dimdim.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
@Service @Transactional(readOnly=true)
public class CadastroService {
 private final ClienteRepository clientes;
 private final ContaRepository contas;
 public CadastroService(ClienteRepository clientes,ContaRepository contas) { this.clientes=clientes;this.contas=contas; }
 Cliente cliente(Long id) { return clientes.findById(id).orElseThrow(()->new ResponseStatusException(NOT_FOUND,"Cliente não encontrado")); }
 Conta conta(Long id) { return contas.findById(id).orElseThrow(()->new ResponseStatusException(NOT_FOUND,"Conta não encontrada")); }
 public Page<ClienteResponse> listarClientes(Pageable p) { return clientes.findAll(p).map(ClienteResponse::of); }
 public ClienteResponse buscarCliente(Long id) { return ClienteResponse.of(cliente(id)); }
 @Transactional public ClienteResponse criarCliente(ClienteRequest r) { return ClienteResponse.of(clientes.saveAndFlush(new Cliente(r))); }
 @Transactional public ClienteResponse atualizarCliente(Long id,ClienteRequest r) { Cliente c=cliente(id);c.atualizar(r);return ClienteResponse.of(clientes.saveAndFlush(c)); }
 @Transactional public void excluirCliente(Long id) {
  Cliente c=cliente(id);
  if(contas.existsByClienteId(id)) throw new ResponseStatusException(CONFLICT,"Exclua as contas vinculadas antes de excluir o cliente");
  clientes.delete(c);clientes.flush();
 }
 public Page<ContaResponse> listarContas(Pageable p) { return contas.findAll(p).map(ContaResponse::of); }
 public ContaResponse buscarConta(Long id) { return ContaResponse.of(conta(id)); }
 @Transactional public ContaResponse criarConta(ContaRequest r) { return ContaResponse.of(contas.saveAndFlush(new Conta(r,cliente(r.clienteId())))); }
 @Transactional public ContaResponse atualizarConta(Long id,ContaRequest r) { Conta c=conta(id);c.atualizar(r,cliente(r.clienteId()));return ContaResponse.of(contas.saveAndFlush(c)); }
 @Transactional public void excluirConta(Long id) { contas.delete(conta(id));contas.flush(); }
}
