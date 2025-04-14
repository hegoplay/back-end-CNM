package iuh.fit.se.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.model.dto.user.UserUpdateRequest;
import iuh.fit.se.model.dto.user.UserUpdateRequestJSON;

@Mapper(componentModel = "spring")
public interface UserMapper {
	
	UserMapper INSTANCE = Mappers.getMapper( UserMapper.class );
	
	@Mapping(target = "isMale", source = "male")  // Rõ ràng mapping boolean
	@Mapping(target = "isOnline", source = "online")  // Rõ ràng mapping boolean
	UserResponseDto toUserResponseDto(User user);
	
	@Mapping(target = "male", source = "isMale")  // Rõ ràng mapping boolean
	@Mapping(target = "baseImg", ignore = true)
	@Mapping(target = "backgroundImg", ignore = true)
	User fromUserUpdateRequestMapToUser(UserUpdateRequest userRequest, @MappingTarget User user);
	
	@Mapping(target = "male", source = "isMale")  // Rõ ràng mapping boolean
	User fromUserUpdateRequestJSONMapToUser(UserUpdateRequestJSON userRequest, @MappingTarget User user);
}
