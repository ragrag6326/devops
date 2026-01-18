package com.tkb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.tkb.entity.UserEntity;
import com.tkb.mapper.UserMapper;
import com.tkb.service.UserService;
import com.tkb.utils.JwtUtils;
import com.tkb.vo.LoginVO;
import com.tkb.vo.PageBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public LoginVO login(String username , String password) {
        UserEntity authUser = this.lambdaQuery()
                .eq(UserEntity::getUsername, username)
                .eq(UserEntity::getPassword, password)
                .one();

        if (authUser != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", authUser.getId());
            map.put("name", authUser.getName());
            map.put("username", authUser.getUsername());

            String jwt = JwtUtils.generateJWT(map); // jwt 包含當前員工登入的訊息

            LoginVO loginVO = new LoginVO();
            loginVO.setId(authUser.getId());
            loginVO.setUsername(authUser.getUsername());
            loginVO.setRole(authUser.getRole());
            loginVO.setToken(jwt);
            return loginVO;
        }

        return new LoginVO();
    }

    @Override
    public PageBean page(Integer page, Integer pageSize, String name) {
        PageHelper.startPage(page, pageSize);

        List<UserEntity> list = this.lambdaQuery()
                .like(name != null && !name.isEmpty() , UserEntity::getUsername, name)
                .list();

        Page<UserEntity> pageList = (Page<UserEntity>) list;

        // 3. 封裝 pageBean 對象
        return new PageBean(pageList.getTotal(), pageList.getResult());
    }
}
