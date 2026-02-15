package com.fox.tax.common.config;

import com.fox.tax.modules.rbac.entity.Function; // 確保有這個 Entity
import com.fox.tax.modules.rbac.entity.Role;
import com.fox.tax.modules.rbac.entity.User;
import com.fox.tax.modules.rbac.repository.FunctionRepository; // 需新增 Repository
import com.fox.tax.modules.rbac.repository.RoleRepository;
import com.fox.tax.modules.rbac.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FunctionRepository functionRepository; // 新增注入
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println(">>> 開始檢查並初始化系統資料...");

        // 1. 初始化功能 (Functions) - 這是解決 403 的關鍵
        // 定義系統所有的權限代碼
        Map<String, Function> functionMap = new HashMap<>();
        functionMap.put("USER_VIEW", initFunction("USER_VIEW", "檢視使用者"));
        functionMap.put("USER_EDIT", initFunction("USER_EDIT", "編輯使用者"));
        functionMap.put("ROLE_VIEW", initFunction("ROLE_VIEW", "檢視角色"));
        functionMap.put("ROLE_EDIT", initFunction("ROLE_EDIT", "編輯角色"));
        functionMap.put("FUNCTION_VIEW", initFunction("FUNCTION_VIEW", "檢視功能"));
        functionMap.put("FUNCTION_EDIT", initFunction("FUNCTION_EDIT", "編輯功能"));
        functionMap.put("BOM_VIEW", initFunction("BOM_VIEW", "檢視退稅標準"));
        functionMap.put("BOM_EDIT", initFunction("BOM_EDIT", "編輯退稅標準"));
        // 進口報單
        functionMap.put("IMPORT_DECLARATION_VIEW", initFunction("IMPORT_DECLARATION_VIEW", "檢視進口報單"));
        functionMap.put("IMPORT_DECLARATION_EDIT", initFunction("IMPORT_DECLARATION_EDIT", "編輯進口報單"));
        // 出口報單
        functionMap.put("EXPORT_DECLARATION_VIEW", initFunction("EXPORT_DECLARATION_VIEW", "檢視出口報單"));
        functionMap.put("EXPORT_DECLARATION_EDIT", initFunction("EXPORT_DECLARATION_EDIT", "編輯出口報單"));
        // 退稅清單
        functionMap.put("TAX_REFUND_VIEW", initFunction("TAX_REFUND_VIEW", "檢視退稅清單"));
        functionMap.put("TAX_REFUND_EDIT", initFunction("TAX_REFUND_EDIT", "編輯退稅清單"));

        // 2. 初始化角色 (Roles) 並授權
        Role adminRole = initRole("ROLE_ADMIN", "系統管理員", new ArrayList<>(functionMap.values()));

        // OP 只有部分權限
        List<Function> opFunctions = Arrays.asList(
                functionMap.get("BOM_VIEW"),
                functionMap.get("BOM_EDIT"));
        initRole("ROLE_OP", "操作人員", opFunctions);

        // 3. 初始化預設管理員帳號
        initAdminUser(adminRole);

        System.out.println(">>> 系統資料初始化完成！");
    }

    private Function initFunction(String code, String name) {
        return functionRepository.findByCode(code)
                .orElseGet(() -> {
                    Function f = new Function();
                    f.setCode(code);
                    f.setName(name); // 假設你的 Function Entity 有 name 欄位
                    return functionRepository.save(f);
                });
    }

    private Role initRole(String roleName, String description, List<Function> functions) {
        return roleRepository.findByName(roleName)
                .map(role -> {
                    // 如果角色已存在，更新它的權限 (確保新加的功能會被寫入)
                    if (role.getFunctions() == null) {
                        role.setFunctions(new HashSet<>());
                    }
                    // 加入所有缺失的權限
                    for (Function f : functions) {
                        if (role.getFunctions().stream()
                                .noneMatch(existing -> existing.getCode().equals(f.getCode()))) {
                            role.getFunctions().add(f);
                        }
                    }
                    return roleRepository.save(role);
                })
                .orElseGet(() -> {
                    // 建立新角色
                    Role role = new Role();
                    role.setName(roleName);
                    role.setDescription(description);
                    role.setFunctions(new HashSet<>(functions));
                    return roleRepository.save(role);
                });
    }

    private void initAdminUser(Role adminRole) {
        String adminUsername = "admin";
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@fox-tech.com");
            admin.setEnabled(true);
            admin.setRoles(new HashSet<>(Collections.singletonList(adminRole)));
            userRepository.save(admin);
            System.out.println("   + 建立管理員帳號: " + adminUsername);
        }
    }
}