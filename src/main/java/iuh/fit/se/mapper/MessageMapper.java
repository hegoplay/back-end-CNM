package iuh.fit.se.mapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import iuh.fit.se.model.Message;
import iuh.fit.se.model.dto.message.MessageRequestDTO;
import iuh.fit.se.model.dto.message.MessageResponseDTO;

@Mapper(componentModel = "spring")
public interface MessageMapper {
	
	
	MessageResponseDTO toMessageResponseDto(Message user);
	
	Message fromMessageRequestDto(MessageRequestDTO userRequestDto);
	
	Message fromMessageResponseDto(MessageResponseDTO userResponseDto);
	
//	MessageResponseDTO.ReactionDTO reactionListToReactionDTOList(List<Message.Reaction> reactions);
	
	Message fromMessageResponseDtoToMessage(MessageResponseDTO userResponseDto, @MappingTarget Message message);
	
	@AfterMapping
    default void afterFromMessageRequestDto(MessageRequestDTO dto, @MappingTarget Message message) {
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString());
        }
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }
        if (message.getReactions() == null) {
            message.setReactions(new ArrayList<>());
        }
        if (message.getSeenBy() == null) {
            message.setSeenBy(new ArrayList<>());
        }
        message.setRecalled(false);
    }
}
