package br.com.fiap.dimdim.model;
import br.com.fiap.dimdim.dto.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="contas",uniqueConstraints=@UniqueConstraint(name="uk_contas_numero",columnNames="numero"))
public class Conta {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=20) private String numero;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=10) private TipoConta tipo;
 @Column(nullable=false,precision=15,scale=2) private BigDecimal saldo;
 @ManyToOne(fetch=FetchType.LAZY,optional=false)
 @JoinColumn(name="cliente_id",nullable=false,foreignKey=@ForeignKey(name="fk_contas_cliente")) private Cliente cliente;
 public Long getId() { return id; }
 public String getNumero() { return numero; }
 public TipoConta getTipo() { return tipo; }
 public BigDecimal getSaldo() { return saldo; }
 public Cliente getCliente() { return cliente; }
 protected Conta() {}
 public Conta(ContaRequest r, Cliente c) { atualizar(r,c); }
 public void atualizar(ContaRequest r,Cliente c) { numero=r.numero(); tipo=r.tipo(); saldo=r.saldo(); cliente=c; }
}
