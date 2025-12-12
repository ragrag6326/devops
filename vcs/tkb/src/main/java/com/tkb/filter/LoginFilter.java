package com.tkb.filter;


import com.tkb.utils.JwtUtils;
import com.tkb.utils.result.Result;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;


@Slf4j
@WebFilter(urlPatterns = "/*")
public class LoginFilter implements Filter {

    // 1. 定義白名單：包含登入接口、Swagger/Knife4j 資源、靜態資源
    private static final String[] WHITE_LIST = {
            "/login",                 // vcs 登入驗證
            "/doc.html",              // Knife4j 主頁面
            "/webjars",               // Knife4j/Swagger 依賴的靜態資源 (CSS/JS)
            "/v3/api-docs",           // OpenAPI 3 的 JSON 數據接口
            "/swagger-ui",            // Swagger 原生頁面資源
            "/swagger-resources",     // Swagger 資源配置
            "/favicon.ico",           // 瀏覽器圖標
            "/error"                  // Spring Boot 預設錯誤頁 (避免無限循環)
    };


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain Chain) throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String url = req.getRequestURI();
        log.debug("請求路徑: {}", url);

        // 2. 檢查是否在白名單中
        // 只要路徑 "包含" 白名單中的任意字串，就直接放行
        for (String whiteUrl : WHITE_LIST) {
            if (url.contains(whiteUrl)) {
                log.debug("該 url 無須驗證 放行: {}", url);
                Chain.doFilter(request, response);
                return;
            }
        }

        // 3. 獲取請求頭中的 Token
        String jwt = req.getHeader("token");
        //System.out.println(jwt);

        // 4. 判斷 token 是否存在 不存在返回錯誤結果 (未登入)
        if (!StringUtils.hasLength(jwt)){
            log.warn("未帶 token");
            res.setStatus(401);
            res.getWriter().write(JSONObject.toJSONString(Result.error("NOT_LOGIN")));
            return;
        }

        // 5. 解析 token 如果解析失敗 返回錯誤結果
        try {
            JwtUtils.parseJWT(jwt);
        } catch (Exception e) { // jwt 解析失敗
            log.warn("token 解析或驗證失敗: {}", e.getMessage());
            res.setStatus(401);
            res.setContentType("application/json");
            res.getWriter().write(JSONObject.toJSONString(Result.error("TOKEN_INVALID")));
            return;
        }

        // 6. 放行
        log.debug("token 通過驗證");
        Chain.doFilter(request, response);

    }
}
