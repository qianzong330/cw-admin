package com.example.hello.mapper;

import com.example.hello.entity.AccountDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccountDetailMapper {
    
    AccountDetail findById(Long id);
    
    List<AccountDetail> findAll();
    
    List<AccountDetail> findByAccountId(@Param("accountId") Long accountId);
    
    int insert(AccountDetail accountDetail);
    
    int update(AccountDetail accountDetail);
    
    int deleteById(Long id);
}
