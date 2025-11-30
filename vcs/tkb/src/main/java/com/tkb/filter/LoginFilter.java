package com.tkb.filter;


import com.tkb.utils.JwtUtils;
import com.tkb.utils.result.Result;
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
            log.warn("未帶 token");
            res.setStatus(401);
            res.getWriter().write(JSONObject.toJSONString(Result.error("NOT_LOGIN")));
            return;
        }

        // 4. 解析 token 如果解析失敗 返回錯誤結果
        try {
            JwtUtils.parseJWT(jwt);
        } catch (Exception e) { // jwt 解析失敗
            log.warn("token 解析或驗證失敗: {}", e.getMessage());
            res.setStatus(401);
            res.getWriter().write(JSONObject.toJSONString(Result.error("TOKEN_INVALID")));
            return;
        }

        // 5. 放行
        log.debug("token 通過驗證");
        Chain.doFilter(request, response);

    }
}
