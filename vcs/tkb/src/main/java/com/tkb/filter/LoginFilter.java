package com.tkb.filter;


import com.tkb.result.Result;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;

@Slf4j
public class LoginFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain Chain) throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // 1. 獲取請求 url , 判斷 url 中是否包含 login 包含的話才會放行
        if (req.getRequestURI().contains("/login")) {
            Chain.doFilter(request, response);
            return;
        }

        // 2. 獲取請求頭中的 Token
        String jwt = req.getHeader("token");
        System.out.println(jwt);

        // 3. 判斷 token 是否存在 不存在返回錯誤結果 (未登入)
        if (!StringUtils.hasLength(jwt)){
            log.info("request header \"token\" is empty");
            Result error = Result.error("NOT_LOGIN");
            // 手動轉換 對象-- json  --> fastJson
            String notLogin = JSONObject.toJSONString(error);
            res.getWriter().write(notLogin);
            return;
        }

        // 4. 解析 token 如果解析失敗 返回錯誤結果
        try {
            jwt.parseJWT(jwt);
        } catch (Exception e) { // jwt 解析失敗
            log.info("解析 token 失敗 ，返回未登入錯誤訊息");
            Result error = Result.error("NOT_LOGIN");
            // 手動轉換 對象-- json  --> fastJson
            String notLogin = JSONObject.toJSONString(error);
            res.getWriter().write(notLogin);
            return;
        }

        // 5. 放行
        log.info("token 合法 ，放行操作");
        Chain.doFilter(request, response);

    }
}
