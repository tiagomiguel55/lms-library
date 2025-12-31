package pt.psoft.g1.psoftg1.bookmanagement.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para receber pedidos de validação de livros vindos do lms_lendings_command.
 * Este DTO é uma cópia local necessária para deserialização de mensagens RabbitMQ.
 * O DTO original pertence ao contexto de Lending.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LendingValidationRequest {
    private String requestId;
    private String isbn;
    private String lendingNumber;
}