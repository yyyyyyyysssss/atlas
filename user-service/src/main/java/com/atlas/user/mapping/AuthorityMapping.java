package com.atlas.user.mapping;

import com.atlas.common.core.mapping.LocalDateMapper;
import com.atlas.common.core.mapping.LocalDateTimeMapper;

import com.atlas.user.domain.dto.AuthorityCreateDTO;
import com.atlas.user.domain.dto.AuthorityUpdateDTO;
import com.atlas.user.domain.dto.MenuCreateDTO;
import com.atlas.user.domain.dto.MenuUpdateDTO;
import com.atlas.user.domain.entity.Authority;
import com.atlas.user.domain.vo.AuthorityVO;
import com.atlas.user.domain.vo.MenuVO;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @Description
 * @Author ys
 * @Date 2025/5/17 11:11
 */
@Mapper(builder = @Builder(disableBuilder = true),uses = {LocalDateTimeMapper.class, LocalDateMapper.class})
public interface AuthorityMapping {

    AuthorityMapping INSTANCE = Mappers.getMapper(AuthorityMapping.class);

    Authority toAuthority(AuthorityCreateDTO authorityAddDTO);

    //部分更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mappings({
            @Mapping(target = "urls", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    })
    void updateAuthority(AuthorityUpdateDTO authorityUpdateDTO, @MappingTarget Authority authority);

    //全量更新
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void overwriteAuthority(AuthorityUpdateDTO dto, @MappingTarget Authority entity);

    AuthorityVO toAuthorityVO(Authority authority);

    @IterableMapping(elementTargetType = AuthorityVO.class)
    List<AuthorityVO> toAuthorityVO(List<Authority> authorities);


    Authority toAuthority(MenuCreateDTO menuCreateDTO);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateAuthority(MenuUpdateDTO menuUpdateDTO, @MappingTarget Authority authority);

    MenuVO toMenuVo(AuthorityVO authorityVO);

    MenuVO toMenuVo(Authority authority);

    @IterableMapping(elementTargetType = MenuVO.class)
    List<MenuVO> toMenuVo(List<Authority> authorities);

}
