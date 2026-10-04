package br.com.fiap.dimdim.model;
import br.com.fiap.dimdim.dto.*;

import jakarta.persistence.*;
@Entity @Table(name="clientes", uniqueConstraints=@UniqueConstraint(name="uk_clientes_email", columnNames="email"))
public class Cliente {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=120) private String nome;
 @Column(nullable=false,length=160) private String email;
 public Long getId() { return id; }
 public String getNome() { return nome; }
 public String getEmail() { return email; }
 protected Cliente() {}
 public Cliente(ClienteRequest r) { atualizar(r); }
 public void atualizar(ClienteRequest r) { nome=r.nome().strip(); email=r.email().strip().toLowerCase(java.util.Locale.ROOT); }
}
