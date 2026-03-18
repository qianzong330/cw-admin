package com.example.hello.service;

import com.example.hello.entity.Role;
import com.example.hello.mapper.RoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {

    @Autowired
    private RoleMapper roleMapper;

    public Role findById(Long id) {
        return roleMapper.findById(id);
    }

    public List<Role> findAll() {
        return roleMapper.findAll();
    }

    public List<Role> findExcludeRoot() {
        return roleMapper.findExcludeRoot();
    }

    public Role findByCode(String code) {
        return roleMapper.findByCode(code);
    }
}
