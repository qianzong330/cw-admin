package com.example.hello.mapper;

import com.example.hello.entity.JobCategory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface JobCategoryMapper {
    
    JobCategory findById(Long id);
    
    JobCategory findByName(String name);
    
    List<JobCategory> findAll();
    
    int insert(JobCategory jobCategory);
    
    int update(JobCategory jobCategory);
    
    int deleteById(Long id);
}
