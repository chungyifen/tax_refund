package com.fox.tax;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Spring Boot 應用程式上下文載入整合測試 */
@SpringBootTest
@ActiveProfiles("test")
class TaxApplicationTests {

  @Test
  void contextLoads() {}
}

