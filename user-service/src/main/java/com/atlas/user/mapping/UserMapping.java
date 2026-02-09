package com.atlas.user.mapping;


import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;

import com.atlas.user.domain.dto.UserCreateDTO;
import com.atlas.user.domain.dto.UserUpdateDTO;
import com.atlas.user.domain.entity.User;
import com.atlas.user.domain.vo.UserVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(builder = @Builder(disableBuilder = true),uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface UserMapping {

    UserMapping INSTANCE = Mappers.getMapper(UserMapping.class);


    User toUser(UserCreateDTO userCreateDTO);

    //部分更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUser(UserUpdateDTO userUpdateDTO, @MappingTarget User user);

    //全量更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void overwriteUser(UserUpdateDTO userUpdateDTO, @MappingTarget User user);

    UserVO toUserVO(User user);

    @IterableMapping(elementTargetType = UserVO.class)
    List<UserVO> toUserVO(List<User> users);

}
