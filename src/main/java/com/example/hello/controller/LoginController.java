package com.example.hello.controller;

import com.example.hello.dto.CategoryStatsDTO;
import com.example.hello.dto.ProjectStatsDTO;
import com.example.hello.entity.Employee;
import com.example.hello.entity.Project;
import com.example.hello.mapper.EmployeeMapper;
import com.example.hello.service.AccountService;
import com.example.hello.service.EmployeeService;
import com.example.hello.service.MenuService;
import com.example.hello.service.ProjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

@Controller
public class LoginController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private AccountService accountService;
    
    @Autowired
    private MenuService menuService;
    
    @Autowired
    private EmployeeMapper employeeMapper;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    
    @GetMapping("/debug/session")
    public String debugSession() {
        return "debug-session";
    }

    @PostMapping("/doLogin")
    public String doLogin(@RequestParam String name, 
                          @RequestParam String password,
                          HttpSession session,
                          Model model) {
        System.out.println("=== 尝试登录：用户名=" + name);
        Employee employee = employeeService.login(name, password);
        if (employee != null) {
            System.out.println("=== 登录成功：" + employee.getName() + ", ID=" + employee.getId());
            System.out.println("=== 登录成功：roleId=" + employee.getRoleId());
            
            // 单独查询角色信息
            try {
                var roleInfo = employeeMapper.findRoleByEmployeeId(employee.getId());
                if (roleInfo != null) {
                    employee.setRoleCode((String) roleInfo.get("role_code"));
                    employee.setRoleName((String) roleInfo.get("role_name"));
                    System.out.println("=== 角色查询成功：" + employee.getRoleCode());
                } else {
                    System.out.println("=== 角色查询失败：roleInfo 为 null，员工ID=" + employee.getId());
                }
            } catch (Exception e) {
                System.err.println("=== 查询角色信息异常：" + e.getMessage());
                e.printStackTrace();
            }
            
            // 加载用户的菜单权限
            if (employee.getRoleId() != null && employee.getRoleCode() != null) {
                try {
                    var menuCodes = menuService.getMenuCodesByRoleCode(employee.getRoleCode());
                    employee.setMenuCodes(menuCodes);
                    System.out.println("=== 菜单权限加载成功：" + menuCodes.size() + " 个权限");
                    System.out.println("=== 菜单权限列表：" + menuCodes);
                } catch (Exception e) {
                    // 如果菜单权限加载失败，不影响登录
                    System.err.println("=== 加载菜单权限异常：" + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("=== 跳过菜单权限加载：roleId=" + employee.getRoleId() + ", roleCode=" + employee.getRoleCode());
            }
            session.setAttribute("currentUser", employee);
            return "redirect:/index";
        } else {
            System.out.println("=== 登录失败：用户名或密码错误");
            model.addAttribute("error", "用户名或密码错误");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/index")
    public String index(@RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "12") int pageSize,
                        @RequestParam(required = false) String timeRange,
                        Model model, 
                        HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        boolean isBoss = currentUser != null && currentUser.isBoss();
        Long userId = currentUser != null ? currentUser.getId() : null;
        
        // 计算时间范围
        DateRange dateRange = calculateDateRange(timeRange);
        String startDate = dateRange.startDate;
        String endDate = dateRange.endDate;
        
        // 获取项目统计数据
        List<ProjectStatsDTO> allProjectStats = accountService.getProjectStats(userId, isBoss, startDate, endDate);
        BigDecimal totalIncome = accountService.getTotalIncome(userId, isBoss, startDate, endDate);
        BigDecimal totalExpense = accountService.getTotalExpense(userId, isBoss, startDate, endDate);
        
        // 获取收入/支出TOP5费用分类
        List<ProjectStatsDTO> top5Income = accountService.getTop5IncomeCategories(userId, isBoss, startDate, endDate);
        List<ProjectStatsDTO> top5Expense = accountService.getTop5ExpenseCategories(userId, isBoss, startDate, endDate);
        
        // 分页处理
        int totalCount = allProjectStats.size();
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalCount);
        List<ProjectStatsDTO> projectStats = allProjectStats.subList(fromIndex, toIndex);
        
        model.addAttribute("projectStats", projectStats);
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("top5Income", top5Income);
        model.addAttribute("top5Expense", top5Expense);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("timeRange", timeRange);
        return "index";
    }
    
    // 时间范围计算类
    private static class DateRange {
        String startDate;
        String endDate;
        DateRange(String start, String end) {
            this.startDate = start;
            this.endDate = end;
        }
    }
    
    // 根据时间范围参数计算开始和结束日期
    private DateRange calculateDateRange(String timeRange) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String endDate = today.format(formatter);
        String startDate = null;
        
        if (timeRange == null || timeRange.isEmpty()) {
            // 默认全部时间，不限制
            return new DateRange(null, null);
        }
        
        switch (timeRange) {
            case "week":
                // 本周（从周一开始）
                startDate = today.with(WeekFields.of(Locale.CHINA).dayOfWeek(), 1L).format(formatter);
                break;
            case "month":
                // 本月
                startDate = today.withDayOfMonth(1).format(formatter);
                break;
            case "quarter":
                // 本季度
                int quarter = (today.getMonthValue() - 1) / 3 + 1;
                startDate = LocalDate.of(today.getYear(), (quarter - 1) * 3 + 1, 1).format(formatter);
                break;
            case "halfYear":
                // 近半年
                startDate = today.minusMonths(6).withDayOfMonth(1).format(formatter);
                break;
            case "year":
                // 近一年
                startDate = today.minusYears(1).withDayOfMonth(1).format(formatter);
                break;
            case "twoYears":
                // 近两年
                startDate = today.minusYears(2).withDayOfMonth(1).format(formatter);
                break;
            case "threeYears":
                // 近三年
                startDate = today.minusYears(3).withDayOfMonth(1).format(formatter);
                break;
            default:
                // 全部时间
                return new DateRange(null, null);
        }
        
        return new DateRange(startDate, endDate);
    }

    @GetMapping("/project/{id}/stats")
    public String projectStats(@PathVariable Long id, 
                               @RequestParam(required = false) String timeRange,
                               Model model, 
                               HttpSession session) {
        Employee currentUser = (Employee) session.getAttribute("currentUser");
        
        // 获取项目信息
        Project project = projectService.findById(id);
        if (project == null) {
            return "redirect:/index";
        }
        
        // 计算时间范围
        DateRange dateRange = calculateDateRange(timeRange);
        String startDate = dateRange.startDate;
        String endDate = dateRange.endDate;
        
        // 获取项目统计
        ProjectStatsDTO stats = accountService.getSingleProjectStats(id, startDate, endDate);
        
        // 获取项目收入/支出分类统计
        List<CategoryStatsDTO> incomeByCategory = accountService.getProjectIncomeByCategory(id, startDate, endDate);
        List<CategoryStatsDTO> expenseByCategory = accountService.getProjectExpenseByCategory(id, startDate, endDate);
        
        model.addAttribute("project", project);
        model.addAttribute("stats", stats);
        model.addAttribute("incomeByCategory", incomeByCategory);
        model.addAttribute("expenseByCategory", expenseByCategory);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("timeRange", timeRange);
        return "project/stats";
    }
}
