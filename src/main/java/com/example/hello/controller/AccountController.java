package com.example.hello.controller;

import com.example.hello.entity.Account;
import com.example.hello.entity.Category;
import com.example.hello.entity.Employee;
import com.example.hello.entity.Project;
import com.example.hello.mapper.ProjectAdminMapper;
import com.example.hello.service.AccountService;
import com.example.hello.service.CategoryService;
import com.example.hello.service.EmployeeService;
import com.example.hello.service.FileUploadService;
import com.example.hello.service.ProjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ProjectAdminMapper projectAdminMapper;

    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private FileUploadService fileUploadService;

    @GetMapping("/list")
    public String list(@RequestParam(required = false) Long projectId,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) Integer type,
                       @RequestParam(required = false) String creatorName,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int pageSize,
                       Model model,
                       HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        
        // 检查是否是项目管理员
        boolean isProjectAdmin = projectAdminMapper.hasAnyProjectAdmin(currentUser.getId());
        // 检查是否是管理员角色
        boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRoleCode());
        
        // 分页查询 - BOSS看所有，管理员看管理的项目(不含BOSS记账)，项目管理员看管理的项目，普通员工看自己的
        List<Account> accounts = accountService.findByConditionWithPage(
            currentUser.getId(), 
            currentUser.isBoss(),
            isProjectAdmin,
            isAdmin,
            projectId, 
            status,
            type,
            creatorName,
            page,
            pageSize
        );
        int totalCount = accountService.countByCondition(
            currentUser.getId(), 
            currentUser.isBoss(),
            isProjectAdmin,
            isAdmin,
            projectId, 
            status,
            type,
            creatorName
        );
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        
        // 为每个帐条设置canApprove字段
        boolean isBoss = currentUser.isBoss();
        for (Account acc : accounts) {
            boolean canApprove = false;
            if (acc.getStatus() != null && acc.getStatus() == 1) {
                Integer stage = acc.getApprovalStage();
                if (stage == null) stage = 1;
                
                if (stage == 1) {
                    // 阶段1：项目管理员或BOSS可以审批
                    if (isBoss) {
                        canApprove = true;
                    } else if (acc.getProjectId() != null) {
                        canApprove = projectAdminMapper.isProjectAdmin(acc.getProjectId(), currentUser.getId());
                    }
                } else if (stage == 2) {
                    // 阶段2：只有BOSS可以审批
                    canApprove = isBoss;
                }
            }
            acc.setCanApprove(canApprove);
        }
        
        // 根据权限获取项目列表：BOSS/财务看所有，普通员工只看关联的
        List<Project> projects = projectService.findByUserId(currentUser.getId(), currentUser.isBoss(), isProjectAdmin);
        List<Category> categories = categoryService.findAll();
        
        // 获取审批人信息（用于前端展示）
        List<Employee> financeList = employeeService.findByRoleCode("finance");
        List<Employee> bossList = employeeService.findByRoleCode("boss");
        
        model.addAttribute("accounts", accounts);
        model.addAttribute("projects", projects);
        model.addAttribute("categories", categories);
        model.addAttribute("financeList", financeList);
        model.addAttribute("bossList", bossList);
        model.addAttribute("projectId", projectId);
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("creatorName", creatorName);
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
        
        // 检查是否是管理员角色
        boolean isAdmin = "admin".equalsIgnoreCase(currentUser.getRoleCode());
        
        // 查询待审批的帐条（状态为1-审批中）
        // 财务看approvalStage=1，BOSS看approvalStage=2
        List<Account> accounts = accountService.findByConditionWithPage(
            currentUser.getId(), 
            currentUser.isBoss(),
            currentUser.isFinance(),
            isAdmin,
            null, // projectId
            1,    // status = 1 审批中
            null, // type
            null, // creatorName
            page,
            pageSize
        );
        int totalCount = accountService.countByCondition(
            currentUser.getId(), 
            currentUser.isBoss(),
            currentUser.isFinance(),
            isAdmin,
            null, // projectId
            1,    // status = 1 审批中
            null, // type
            null  // creatorName
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
        
        // 检查是否是项目管理员
        boolean isProjectAdmin = projectAdminMapper.hasAnyProjectAdmin(currentUser.getId());
        
        // 根据权限获取项目列表：BOSS看所有，项目管理员看管理的，普通员工看关联的
        List<Project> projects = projectService.findByUserId(currentUser.getId(), currentUser.isBoss(), isProjectAdmin);
        List<Category> categories = categoryService.findAll();
        
        model.addAttribute("projects", projects);
        model.addAttribute("categories", categories);
        model.addAttribute("account", new Account());
        model.addAttribute("currentUser", currentUser);
        return "account/form";
    }

    @GetMapping("/detail/{id}")
    @ResponseBody
    public Map<String, Object> getDetail(@PathVariable Long id, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        Account account = accountService.findById(id);
        
        // 空值检查
        if (account == null) {
            result.put("success", false);
            result.put("message", "帐条不存在");
            return result;
        }
        
        // 预览功能：所有用户都可以查看帐条详情
        result.put("success", true);
        result.put("account", account);
        return result;
    }

    @PostMapping("/save")
    public String save(Account account, 
                       @RequestParam(required = false) List<MultipartFile> invoiceFiles,
                       @RequestParam(required = false) String existingImages,
                       HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        
        // 编辑权限检查：只能编辑自己的帐条
        if (account.getId() != null) {
            Account existing = accountService.findById(account.getId());
            if (existing == null || !existing.getCreatorId().equals(currentUser.getId())) {
                return "redirect:/account/list?error=只能编辑自己的帐条";
            }
            if (existing.getStatus() != 12) {
                return "redirect:/account/list?error=只能编辑审核未通过的帐条";
            }
        }
        
        // 权限检查：所有非BOSS角色只能在自己关联的项目中记账（包括财务）
        if (!currentUser.isBoss()) {
            // 检查是否是项目管理员
            boolean isProjectAdmin = projectAdminMapper.hasAnyProjectAdmin(currentUser.getId());
            List<Long> assignedProjectIds = projectService.findByUserId(currentUser.getId(), false, isProjectAdmin)
                    .stream().map(Project::getId).toList();
            if (!assignedProjectIds.contains(account.getProjectId())) {
                return "redirect:/account/list?error=无权在该项目记账";
            }
        }
        
        // 处理发票图片上传
        List<String> imageUrls = new java.util.ArrayList<>();
        
        // 保留已有的图片
        if (existingImages != null && !existingImages.isEmpty()) {
            imageUrls.addAll(java.util.Arrays.asList(existingImages.split(",")));
        }
        
        // 上传新图片
        if (invoiceFiles != null && !invoiceFiles.isEmpty()) {
            try {
                List<String> newUrls = fileUploadService.uploadInvoices(invoiceFiles);
                imageUrls.addAll(newUrls);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // 设置图片URL（逗号分隔）
        account.setInvoiceImages(String.join(",", imageUrls));
        
        accountService.save(account, currentUser);
        return "redirect:/account/list";
    }

    @GetMapping("/view/{id}")
    public String viewPage(@PathVariable Long id, Model model, HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        Account account = accountService.findById(id);
        
        // 检查是否是项目管理员
        boolean isProjectAdmin = projectAdminMapper.hasAnyProjectAdmin(currentUser.getId());
        
        // 根据权限获取项目列表：BOSS看所有，项目管理员看管理的，普通员工看关联的
        List<Project> projects = projectService.findByUserId(currentUser.getId(), currentUser.isBoss(), isProjectAdmin);
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
        
        // 检查是否是项目管理员
        boolean isProjectAdmin = false;
        if (account.getProjectId() != null) {
            isProjectAdmin = projectAdminMapper.isProjectAdmin(account.getProjectId(), currentUser.getId());
        }
        
        Integer approvalStage = account.getApprovalStage();
        if (approvalStage == null) {
            approvalStage = 1;
        }
        
        // 阶段1(待管理员审批)：只有项目管理员或BOSS可以审批
        // 阶段2(待BOSS审批)：只有BOSS可以审批
        if (approvalStage == 1) {
            if (!isProjectAdmin && !isBoss) {
                return "error:无权限审批，当前阶段需要项目管理员审批";
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
