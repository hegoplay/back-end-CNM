package iuh.fit.se.model.dto.message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import iuh.fit.se.model.dto.user.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.experimental.FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class MessageReactionDto {
	String messageId;
//	tat ca (all), tung reaction
	Map<String, List<UserReactionInfo>> reactions; // userId -> reaction
	
	@NoArgsConstructor
	@AllArgsConstructor
	@Data
	public static class UserReactionInfo{
		UserResponseDto user;
		int count;
	}
	
	Map<String, Integer> reactionCounts = new HashMap<>(); // reaction -> count
	
	public void addReactionCount(String reaction, int count) {
		this.reactionCounts.put(reaction, count);
	}
	
}
