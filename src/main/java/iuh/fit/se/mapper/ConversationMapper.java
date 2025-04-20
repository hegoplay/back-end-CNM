package iuh.fit.se.mapper;


import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;

import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.Message;
import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;
import iuh.fit.se.repo.MessageRepository;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Mapper(componentModel = "spring",injectionStrategy = InjectionStrategy.CONSTRUCTOR, uses = {MessageRepository.class,MessageMapper.class})
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
//@RequiredArgsConstructor(onConstructor = @__(@Autowired))
//@NoArgsConstructor(force = true, access = AccessLevel.PROTECTED) // Thêm dòng này
@Slf4j
public abstract class ConversationMapper {

   
	public abstract ConversationDto fromConversationToDto(Conversation user);

	
	@Mapping(target = "messageDetails", ignore = true)
	public abstract ConversationDetailDto fromConversationToDetailDto(Conversation user);

	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	public abstract Conversation fromDtoToConversation(ConversationDto user);

	public abstract Conversation fromDtoToConversation(ConversationDto user,
			@MappingTarget Conversation conversation);

	public abstract Conversation fromDetailDtoToConversation(ConversationDetailDto detailDto,
			@MappingTarget Conversation conversation);

}
