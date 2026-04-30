package com.example.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.demo.common.dto.UserLoginDTO;
import com.example.demo.common.dto.UserRegisterDTO;
import com.example.demo.common.dto.UserInfoUpdateDTO;
import com.example.demo.common.vo.UserVO;
import com.example.demo.entity.User;

public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    void register(UserRegisterDTO dto);

    /**
     * 用户登录
     */
    UserVO login(UserLoginDTO dto);

    /**
     * 修改用户资料
     */
    void updateInfo(UserInfoUpdateDTO dto);

    /**
     * 查询用户信息（带Redis缓存）
     */
    UserVO getUserInfo(Long userId);
}