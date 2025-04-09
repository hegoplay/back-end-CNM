package iuh.fit.se.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.UserResponseDto;

@Mapper
public interface UserMapper {
	
	UserMapper INSTANCE = Mappers.getMapper( UserMapper.class );
	
	UserResponseDto toUserResponseDto(User user);
}
