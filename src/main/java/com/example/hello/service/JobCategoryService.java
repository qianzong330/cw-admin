package com.example.hello.service;

import com.example.hello.entity.JobCategory;
import com.example.hello.mapper.JobCategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobCategoryService {

    @Autowired
    private JobCategoryMapper jobCategoryMapper;

    public JobCategory findById(Long id) {
        return jobCategoryMapper.findById(id);
    }

    public List<JobCategory> findAll() {
        return jobCategoryMapper.findAll();
    }
    
    public JobCategory findByName(String name) {
        return jobCategoryMapper.findByName(name);
    }

    public boolean save(JobCategory jobCategory) {
        if (jobCategory.getId() == null) {
            return jobCategoryMapper.insert(jobCategory) > 0;
        } else {
            return jobCategoryMapper.update(jobCategory) > 0;
        }
    }

    public boolean deleteById(Long id) {
        return jobCategoryMapper.deleteById(id) > 0;
    }
}
