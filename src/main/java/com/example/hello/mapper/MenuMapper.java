package com.example.hello.mapper;

import com.example.hello.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单 Mapper 接口
 */
@Mapper
public interface MenuMapper {
    
    /**
     * 根据角色 ID 查询菜单权限
     */
    List<Menu> selectMenusByRoleId(@Param("roleId") Long roleId);
    
    /**
     * 根据角色编码查询菜单权限
     */
    List<Menu> selectMenusByRoleCode(@Param("roleCode") String roleCode);
    
    /**
     * 查询所有菜单
     */
    List<Menu> selectAllMenus();
    
    /**
     * 根据菜单编码查询菜单
     */
    Menu selectByMenuCode(@Param("menuCode") String menuCode);
    
    /**
     * 检查角色是否有菜单权限
     */
    boolean hasMenuPermission(@Param("roleId") Long roleId, @Param("menuCode") String menuCode);
}
