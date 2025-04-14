package iuh.fit.se.model.dto.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MarkConversationAsReadRequestDto {
	private String conversationId;
}
