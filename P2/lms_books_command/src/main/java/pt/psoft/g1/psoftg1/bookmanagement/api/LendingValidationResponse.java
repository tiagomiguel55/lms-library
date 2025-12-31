package pt.psoft.g1.psoftg1.bookmanagement.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para enviar respostas de validação de livros para o lms_lendings_command.
 * Este DTO é uma cópia local necessária para serialização de mensagens RabbitMQ.
 * O DTO original pertence ao contexto de Lending.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LendingValidationResponse {
    private String requestId;
    private String lendingNumber;
    private boolean bookExists;
    private String isbn;
    private String message;
}


