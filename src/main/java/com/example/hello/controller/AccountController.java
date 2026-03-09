package com.example.hello.controller;

import com.example.hello.entity.Account;
import com.example.hello.entity.Category;
import com.example.hello.entity.Employee;
import com.example.hello.entity.Project;
import com.example.hello.service.AccountService;
import com.example.hello.service.CategoryService;
import com.example.hello.service.ProjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    public String list(@RequestParam(required = false) Long projectId,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       Model model,
                       HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        
        // 分页查询
        List<Account> accounts = accountService.findByConditionWithPage(
            currentUser.getId(), 
            currentUser.isBoss(), 
            projectId, 
            status,
            page,
            pageSize
        );
        int totalCount = accountService.countByCondition(
            currentUser.getId(), 
            currentUser.isBoss(), 
            projectId, 
            status
        );
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        
        // 根据权限获取项目列表：BOSS/财务看所有，普通员工只看关联的
        List<Project> projects = projectService.findByUserId(currentUser.getId(), currentUser.isBoss());
        List<Category> categories = categoryService.findAll();
        
        model.addAttribute("accounts", accounts);
        model.addAttribute("projects", projects);
        model.addAttribute("categories", categories);
        model.addAttribute("projectId", projectId);
        model.addAttribute("status", status);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isPendingPage", false);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("totalPages", totalPages);
        return "account/list";
    }

    @GetMapping("/pending")
    public String pendingList(@RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int pageSize,
                              Model model, 
                              HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        
        // 查询待审批的帐条（状态为1-审批中，且当前用户是财务对接人或BOSS）
        List<Account> accounts = accountService.findPendingByFinanceContactIdWithPage(
            currentUser.getId(), 
            currentUser.isBoss(),
            page,
            pageSize
        );
        int totalCount = accountService.countPendingByFinanceContactId(
            currentUser.getId(), 
            currentUser.isBoss()
        );
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        
        List<Project> projects = projectService.findAll();
        List<Category> categories = categoryService.findAll();
        
        model.addAttribute("accounts", accounts);
        model.addAttribute("projects", projects);
        model.addAttribute("categories", categories);
        model.addAttribute("projectId", null);
        model.addAttribute("status", 1);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isPendingPage", true);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("totalPages", totalPages);
        return "account/list";
    }

    @GetMapping("/add")
    public String addPage(Model model, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        
        // 根据权限获取项目列表：BOSS/财务看所有，普通员工只看关联的
        List<Project> projects = projectService.findByUserId(currentUser.getId(), currentUser.isBoss());
        List<Category> categories = categoryService.findAll();
        
        model.addAttribute("projects", projects);
        model.addAttribute("categories", categories);
        model.addAttribute("account", new Account());
        model.addAttribute("currentUser", currentUser);
        return "account/form";
    }

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Long id, Model model, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        Account account = accountService.findById(id);
        
        String roleCode = currentUser.getRoleCode() != null ? currentUser.getRoleCode().toLowerCase() : "";
        boolean isBoss = "boss".equals(roleCode);
        // 权限检查：只能编辑自己的帐条，且状态为审核未通过
        if (!account.getCreatorId().equals(currentUser.getId()) && !isBoss) {
            return "redirect:/account/list";
        }
        
        if (account.getStatus() != 12 && !isBoss) {
            return "redirect:/account/list";
        }
        
        // 根据权限获取项目列表：BOSS/财务看所有，普通员工只看关联的
        List<Project> projects = projectService.findByUserId(currentUser.getId(), currentUser.isBoss());
        List<Category> categories = categoryService.findAll();
        
        model.addAttribute("projects", projects);
        model.addAttribute("categories", categories);
        model.addAttribute("account", account);
        model.addAttribute("currentUser", currentUser);
        return "account/form";
    }

    @PostMapping("/save")
    public String save(Account account, 
                       @RequestParam(required = false) MultipartFile invoiceFile,
                       HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        
        // 权限检查：非BOSS角色只能在自己关联的项目中记账
        if (!currentUser.isBoss() && !currentUser.isFinance()) {
            List<Long> assignedProjectIds = projectService.findByUserId(currentUser.getId(), false)
                    .stream().map(Project::getId).toList();
            if (!assignedProjectIds.contains(account.getProjectId())) {
                return "redirect:/account/list?error=无权在该项目记账";
            }
        }
        
        // 处理文件上传
        if (invoiceFile != null && !invoiceFile.isEmpty()) {
            String fileName = UUID.randomUUID().toString() + "_" + invoiceFile.getOriginalFilename();
            String uploadPath = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";
            File dest = new File(uploadPath + fileName);
            try {
                invoiceFile.transferTo(dest);
                account.setInvoiceNo(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        accountService.save(account, currentUser);
        return "redirect:/account/list";
    }

    @GetMapping("/view/{id}")
    public String viewPage(@PathVariable Long id, Model model, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        Account account = accountService.findById(id);
        
        // 根据权限获取项目列表：BOSS/财务看所有，普通员工只看关联的
        List<Project> projects = projectService.findByUserId(currentUser.getId(), currentUser.isBoss());
        List<Category> categories = categoryService.findAll();
        
        model.addAttribute("projects", projects);
        model.addAttribute("categories", categories);
        model.addAttribute("account", account);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("viewMode", true);
        return "account/form";
    }

    @PostMapping("/approve/{id}")
    @ResponseBody
    public String approve(@PathVariable Long id, 
                          @RequestParam boolean approved,
                          @RequestParam(required = false) String remark,
                          HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        Account account = accountService.findById(id);
        
        if (account == null) {
            return "error:帐条不存在";
        }
        
        // 权限检查
        String roleCode = currentUser.getRoleCode() != null ? currentUser.getRoleCode().toLowerCase() : "";
        boolean isBoss = "boss".equals(roleCode) || "root".equals(roleCode);
        boolean isFinance = currentUser.isFinance();
        
        Integer approvalStage = account.getApprovalStage();
        if (approvalStage == null) {
            approvalStage = 1;
        }
        
        // 阶段1(待财务审批)：只有财务角色可以审批
        // 阶段2(待BOSS审批)：只有BOSS可以审批
        if (approvalStage == 1) {
            if (!isFinance && !isBoss) {
                return "error:无权限审批，当前阶段需要财务角色审批";
            }
        } else if (approvalStage == 2) {
            if (!isBoss) {
                return "error:无权限审批，当前阶段需要BOSS审批";
            }
        }
        
        // 状态检查：只有审批中(status=1)的帐条可以审批
        if (account.getStatus() == null || account.getStatus() != 1) {
            return "error:该帐条不在审批中状态";
        }
        
        boolean success = accountService.approve(id, currentUser, approved, remark);
        return success ? "success" : "error";
    }

    @PostMapping("/revoke/{id}")
    @ResponseBody
    public String revoke(@PathVariable Long id, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        Account account = accountService.findById(id);
        
        // 权限检查：只能撤销自己的帐条，且状态为审批中
        if (!account.getCreatorId().equals(currentUser.getId())) {
            return "error:无权限";
        }
        
        if (account.getStatus() != 1) {
            return "error:状态不正确";
        }
        
        boolean success = accountService.revoke(id, currentUser);
        return success ? "success" : "error";
    }
}
