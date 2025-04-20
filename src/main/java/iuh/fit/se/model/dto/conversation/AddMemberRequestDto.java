package iuh.fit.se.model.dto.conversation;

import java.util.List;

import lombok.Data;

@Data
public class AddMemberRequestDto {
	private List<String> newMembersPhone;
}
