package iuh.fit.se.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import iuh.fit.se.model.Message.CallEvent;
import iuh.fit.se.model.dto.message.CallEventDTO;

@Mapper	(componentModel = "spring")
public interface CallEventMapper {
    CallEventMapper INSTANCE = Mappers.getMapper(CallEventMapper.class);
    
    CallEventDTO toCallEventDTO(CallEvent callEvent);
    
    CallEvent fromCallEventDTO(CallEventDTO callEventDTO);
}