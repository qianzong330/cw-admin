package com.example.hello.mapper;

import com.example.hello.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {
    
    Category findById(Long id);
    
    Category findByName(@Param("name") String name);
    
    List<Category> findAll();
    
    int insert(Category category);
    
    int update(Category category);
    
    int deleteById(Long id);
}
