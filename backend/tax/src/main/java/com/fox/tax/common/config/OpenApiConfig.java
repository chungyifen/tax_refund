package com.fox.tax.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "進口退稅核銷系統 API", version = "1.0", description = "提供報單上傳、BOM 維護與退稅核銷計算之 API 介面"), security = @SecurityRequirement(name = "Bearer Authentication") // 全域套用
                                                                                                                                                                                        // JWT
)
@SecurityScheme(name = "Bearer Authentication", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", scheme = "bearer")
public class OpenApiConfig {
    // 這裡不需要寫程式碼，透過註解即可完成設定
}