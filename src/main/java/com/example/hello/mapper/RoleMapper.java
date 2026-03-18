package com.example.hello.mapper;

import com.example.hello.entity.Role;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RoleMapper {
    
    Role findById(Long id);
    
    List<Role> findAll();

    List<Role> findExcludeRoot();
    
    Role findByCode(String code);
}
