package iuh.fit.se.model.dto.search;

import iuh.fit.se.model.dto.UserResponseDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FindPeopleByNameKeywordResponse {

    List<UserResponseDto> friends;

    List<UserWithSharedGroups> othersWithSharedGroups; // Other users and the groups this user share with them

    List<UserResponseDto> contacted;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserWithSharedGroups {
        UserResponseDto user;
        List<String> sharedGroups;
    }
}
