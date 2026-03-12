package com.example.hello.service;

import com.example.hello.entity.Menu;
import com.example.hello.mapper.MenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜单权限服务
 */
@Service
public class MenuService {
    
    @Autowired
    private MenuMapper menuMapper;
    
    /**
     * 根据角色编码查询菜单权限（一律走权限表）
     */
    public List<Menu> getMenusByRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        try {
            return menuMapper.selectMenusByRoleCode(roleCode);
        } catch (Exception e) {
            System.err.println("查询菜单权限失败：" + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * 检查是否有菜单权限
     */
    public boolean hasMenuPermission(Long roleId, String menuCode) {
        return menuMapper.hasMenuPermission(roleId, menuCode);
    }
    
    /**
     * 获取角色的菜单权限码集合
     */
    public Set<String> getMenuCodesByRoleCode(String roleCode) {
        List<Menu> menus = getMenusByRoleCode(roleCode);
        return menus.stream()
                .map(Menu::getMenuCode)
                .collect(Collectors.toSet());
    }
    
    /**
     * 检查是否有按钮权限
     */
    public boolean hasButtonPermission(Long roleId, String buttonCode) {
        return menuMapper.hasMenuPermission(roleId, buttonCode);
    }
}
