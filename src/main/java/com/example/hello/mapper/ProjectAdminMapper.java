package com.example.hello.mapper;

import com.example.hello.entity.ProjectAdmin;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectAdminMapper {

    @Insert("INSERT INTO tb_project_admin (project_id, employee_id) VALUES (#{projectId}, #{employeeId})")
    void insert(ProjectAdmin projectAdmin);

    @Delete("DELETE FROM tb_project_admin WHERE project_id = #{projectId}")
    void deleteByProjectId(Long projectId);

    @Select("SELECT pa.*, e.name as employee_name FROM tb_project_admin pa " +
            "LEFT JOIN tb_employee e ON pa.employee_id = e.id " +
            "WHERE pa.project_id = #{projectId}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "projectId", column = "project_id"),
        @Result(property = "employeeId", column = "employee_id"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time"),
        @Result(property = "employeeName", column = "employee_name")
    })
    List<ProjectAdmin> findByProjectId(Long projectId);

    @Select("SELECT employee_id FROM tb_project_admin WHERE project_id = #{projectId}")
    List<Long> findEmployeeIdsByProjectId(Long projectId);
}
