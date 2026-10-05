package br.com.fiap.dimdim;
import br.com.fiap.dimdim.repository.ClienteRepository;
import br.com.fiap.dimdim.repository.ContaRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class CadastroIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper mapper;
 @Autowired ClienteRepository clientes;
 @Autowired ContaRepository contas;
 @BeforeEach void limpar() { contas.deleteAll(); clientes.deleteAll(); }
 long cliente(String email) throws Exception {
  var result=mvc.perform(post("/api/clientes").contentType("application/json").content("{\"nome\":\"Pedro Demo\",\"email\":\""+email+"\"}"))
   .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
  return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
 }
 String conta(long clienteId,String numero,String saldo) {
  return "{\"numero\":\""+numero+"\",\"tipo\":\"CORRENTE\",\"saldo\":"+saldo+",\"clienteId\":"+clienteId+"}";
 }
 @Test void crudCompletoComPersistenciaERelacionamento() throws Exception {
  long id=cliente("pedro@example.com");
  assertThat(clientes.findById(id)).isPresent();
  mvc.perform(get("/api/clientes")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(id));
  mvc.perform(get("/api/clientes/"+id)).andExpect(status().isOk());
  mvc.perform(put("/api/clientes/"+id).contentType("application/json").content("{\"nome\":\"Pedro Atualizado\",\"email\":\"novo@example.com\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Pedro Atualizado"));
  assertThat(clientes.findById(id).orElseThrow().getNome()  ).isEqualTo("Pedro Atualizado");
  var created=mvc.perform(post("/api/contas").contentType("application/json").content(conta(id,"10001","100.00")))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.clienteId").value(id)).andReturn();
  long cid=mapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
  assertThat(contas.findById(cid)).isPresent();
  mvc.perform(get("/api/contas")).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(cid)).andExpect(jsonPath("$.content[0].clienteId").value(id));
  mvc.perform(get("/api/contas/"+cid)).andExpect(status().isOk()).andExpect(jsonPath("$.clienteId").value(id));
  mvc.perform(put("/api/contas/"+cid).contentType("application/json").content(conta(id,"10002","250.00")))
   .andExpect(status().isOk()).andExpect(jsonPath("$.saldo").value(250.00));
  assertThat(contas.findById(cid).orElseThrow().getSaldo()).isEqualByComparingTo("250.00");
  mvc.perform(delete("/api/clientes/"+id)).andExpect(status().isConflict());
  mvc.perform(delete("/api/contas/"+cid)).andExpect(status().isNoContent());
  assertThat(contas.findById(cid)).isEmpty();
  mvc.perform(delete("/api/clientes/"+id)).andExpect(status().isNoContent());
  assertThat(clientes.findById(id)).isEmpty();
 }
 @Test void rejeitaDuplicidadeENaoAlteraRegistroOriginal() throws Exception {
  long id=cliente("demo@example.com");
  mvc.perform(post("/api/clientes").contentType("application/json").content("{\"nome\":\"Outro\",\"email\":\"DEMO@example.com\"}"))
   .andExpect(status().isConflict());
  long outro=cliente("outro@example.com");
  mvc.perform(put("/api/clientes/"+outro).contentType("application/json").content("{\"nome\":\"Alterado\",\"email\":\"demo@example.com\"}"))
   .andExpect(status().isConflict());
  assertThat(clientes.findById(outro).orElseThrow().getEmail()).isEqualTo("outro@example.com");
  mvc.perform(post("/api/contas").contentType("application/json").content(conta(id,"10001","0"))).andExpect(status().isCreated());
  mvc.perform(post("/api/contas").contentType("application/json").content(conta(outro,"10001","0"))).andExpect(status().isConflict());
  assertThat(contas.count()).isEqualTo(1);
 }
 @Test void validaDadosReferenciasEJson() throws Exception {
  long id=cliente("demo@example.com");
  mvc.perform(post("/api/clientes").contentType("application/json").content("{\"nome\":\" \",\"email\":\"invalido\"}"))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.nome").exists());
  mvc.perform(post("/api/contas").contentType("application/json").content(conta(id,"10001","-1"))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/contas").contentType("application/json").content(conta(id,"10001","0.001"))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/contas").contentType("application/json").content(conta(999999,"10001","0"))).andExpect(status().isNotFound());
  mvc.perform(post("/api/contas").contentType("application/json").content("{}" )).andExpect(status().isBadRequest());
  mvc.perform(post("/api/contas").contentType("application/json").content("{invalid" )).andExpect(status().isBadRequest());
  mvc.perform(get("/api/clientes/999999")).andExpect(status().isNotFound());
  mvc.perform(put("/api/contas/999999").contentType("application/json").content(conta(id,"10001","0"))).andExpect(status().isNotFound());
  mvc.perform(delete("/api/contas/999999")).andExpect(status().isNotFound());
  mvc.perform(get("/api/clientes/abc")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/clientes?sort=inexistente")).andExpect(status().isBadRequest());
 }
 @Test void permiteReatribuirContaParaOutroCliente() throws Exception {
  long id=cliente("um@example.com"),outro=cliente("dois@example.com");
  var response=mvc.perform(post("/api/contas").contentType("application/json").content(conta(id,"10001","0"))).andReturn();
  long cid=mapper.readTree(response.getResponse().getContentAsString()).get("id").asLong();
  mvc.perform(put("/api/contas/"+cid).contentType("application/json").content(conta(outro,"10001","0"))).andExpect(status().isOk()).andExpect(jsonPath("$.clienteId").value(outro));
  mvc.perform(delete("/api/clientes/"+id)).andExpect(status().isNoContent());
  mvc.perform(delete("/api/clientes/"+outro)).andExpect(status().isConflict());
 }
}
