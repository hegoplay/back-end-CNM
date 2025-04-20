package iuh.fit.se.mapper;

import org.mapstruct.Mapper;

import iuh.fit.se.model.Call;
import iuh.fit.se.model.dto.call.CallResponseDto;

@Mapper(componentModel = "spring")
public interface CallMapper {
	CallResponseDto toCallResponseDto(Call call);
	
	Call toCall(CallResponseDto callResponseDto);
}
