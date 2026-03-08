package com.example.hello.service;

import com.example.hello.entity.AccountDetail;
import com.example.hello.mapper.AccountDetailMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountDetailService {

    @Autowired
    private AccountDetailMapper accountDetailMapper;

    public AccountDetail findById(Long id) {
        return accountDetailMapper.findById(id);
    }

    public List<AccountDetail> findAll() {
        return accountDetailMapper.findAll();
    }

    public List<AccountDetail> findByAccountId(Long accountId) {
        return accountDetailMapper.findByAccountId(accountId);
    }

    public boolean save(AccountDetail accountDetail) {
        if (accountDetail.getId() == null) {
            return accountDetailMapper.insert(accountDetail) > 0;
        } else {
            return accountDetailMapper.update(accountDetail) > 0;
        }
    }

    public boolean deleteById(Long id) {
        return accountDetailMapper.deleteById(id) > 0;
    }
}
