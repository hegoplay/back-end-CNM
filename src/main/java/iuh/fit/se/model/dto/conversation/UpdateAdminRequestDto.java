package iuh.fit.se.model.dto.conversation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder({ "targetUserId", "isAdmin" })
public class UpdateAdminRequestDto {
	private String targetUserId;
	@JsonProperty("isAdmin")
	private boolean isAdmin;
}
