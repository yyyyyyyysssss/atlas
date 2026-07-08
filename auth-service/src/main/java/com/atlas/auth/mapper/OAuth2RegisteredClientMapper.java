package com.atlas.auth.mapper;


import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OAuth2RegisteredClientMapper {

    @Delete("DELETE FROM oauth2_registered_client WHERE id = #{id}")
    int deleteById(@Param("id") String id);


    @Delete("DELETE FROM oauth2_authorization WHERE registered_client_id = #{registeredClientId}")
    int deleteAllClientToken(@Param("registeredClientId") String registeredClientId);

}
