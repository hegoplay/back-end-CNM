package iuh.fit.se.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;

@Mapper(componentModel = "spring")
public interface ConversationMapper {
	ConversationMapper INSTANCE = Mappers.getMapper( ConversationMapper.class );
	
	ConversationDto fromConversationToDto(Conversation user);
	
	@Mapping(target = "messageDetails", ignore = true)
	ConversationDetailDto fromConversationToDetailDto(Conversation user);
	
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	Conversation fromDtoToConversation(ConversationDto user);
	
	

	Conversation fromDtoToConversation(ConversationDto user,@MappingTarget Conversation conversation);
	
	Conversation fromDetailDtoToConversation(ConversationDetailDto detailDto,@MappingTarget Conversation conversation);
	
}
