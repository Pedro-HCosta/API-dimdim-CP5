package br.com.fiap.dimdim.exception;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.data.mapping.PropertyReferenceException;
import java.util.*;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(MethodArgumentNotValidException.class)
 ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e) {
  var p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Verifique os campos enviados");
  Map<String,String> fields=new TreeMap<>();
  e.getBindingResult().getFieldErrors().forEach(f->fields.put(f.getField(),f.getDefaultMessage()));
  p.setProperty("campos",fields);return ResponseEntity.badRequest().body(p);
 }
 @ExceptionHandler(ResponseStatusException.class)
 ResponseEntity<ProblemDetail> business(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(ProblemDetail.forStatusAndDetail(e.getStatusCode(),e.getReason())); }
 @ExceptionHandler(DataIntegrityViolationException.class)
 ResponseEntity<ProblemDetail> integrity() { return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"E-mail ou número de conta duplicado, ou registro com vínculo existente")); }
 @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,PropertyReferenceException.class})
 ResponseEntity<ProblemDetail> malformed() { return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"JSON, identificador ou parâmetro inválido")); }
}
