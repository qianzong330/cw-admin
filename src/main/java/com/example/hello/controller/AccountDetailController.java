package com.example.hello.controller;

import com.example.hello.entity.Account;
import com.example.hello.entity.AccountDetail;
import com.example.hello.service.AccountDetailService;
import com.example.hello.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/accountDetail")
public class AccountDetailController {

    @Autowired
    private AccountDetailService accountDetailService;

    @Autowired
    private AccountService accountService;

    @GetMapping("/list/{accountId}")
    public String list(@PathVariable Long accountId, Model model) {
        Account account = accountService.findById(accountId);
        List<AccountDetail> details = accountDetailService.findByAccountId(accountId);
        
        model.addAttribute("account", account);
        model.addAttribute("details", details);
        return "account_detail/list";
    }

    @GetMapping("/api/list/{accountId}")
    @ResponseBody
    public List<AccountDetail> apiList(@PathVariable Long accountId) {
        return accountDetailService.findByAccountId(accountId);
    }
}
