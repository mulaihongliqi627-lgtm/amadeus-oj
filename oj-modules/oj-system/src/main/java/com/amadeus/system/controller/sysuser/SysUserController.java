package com.amadeus.system.controller.sysuser;

import com.amadeus.common.core.constants.Constants;
import com.amadeus.common.core.controller.BaseController;
import com.amadeus.common.core.domain.Result;
import com.amadeus.common.core.utils.ThreadLocalUtil;
import com.amadeus.system.domain.sysuser.dto.LoginDTO;
import com.amadeus.system.domain.sysuser.dto.SysUserSaveDTO;
import com.amadeus.system.domain.sysuser.vo.SysUserVO;
import com.amadeus.system.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sysUser")
@Tag(name = "管理员用户API")
public class SysUserController extends BaseController {

    @Autowired
    private ISysUserService sysUserService;

    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "根据账号密码进行管理员登录")
    @ApiResponse(responseCode = "1000", description = "操作成功")
    @ApiResponse(responseCode = "2000", description = "服务繁忙请稍后重试")
    @ApiResponse(responseCode = "3102", description = "用户不存在")
    @ApiResponse(responseCode = "3103", description = "用户名或密码错误")
    public Result<String> login(@RequestBody LoginDTO loginDTO) {
        return sysUserService.login(loginDTO.getUserAccount(), loginDTO.getPassword());
    }

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息", description = "根据请求头中的 JWT Token 解析当前登录用户，返回其账号与昵称")
    @ApiResponse(responseCode = "1000", description = "操作成功")
    @ApiResponse(responseCode = "2000", description = "服务繁忙请稍后重试")
    @ApiResponse(responseCode = "3102", description = "用户不存在")
    public Result<SysUserVO> info() {
        Long userId = ThreadLocalUtil.get(Constants.USER_ID, Long.class);
        return sysUserService.getUserInfo(userId);
    }

    @DeleteMapping("/logout")
    @Operation(summary = "退出登录", description = "撤销当前 Token 对应的 Redis 登录会话，不影响其他登录会话")
    @ApiResponse(responseCode = "1000", description = "退出成功")
    @ApiResponse(responseCode = "3001", description = "登录会话已失效")
    @ApiResponse(responseCode = "401", description = "未登录或令牌无效")
    public Result<Void> logout() {
        String userKey = ThreadLocalUtil.get(Constants.USER_KEY, String.class);
        return sysUserService.logout(userKey);
    }

    @Operation(summary = "新增管理员", description = "根据提供的信息新增管理员用户")
    @PostMapping("/add")
    @ApiResponse(responseCode = "1000", description = "操作成功")
    @ApiResponse(responseCode = "2000", description = "服务繁忙请稍后重试")
    @ApiResponse(responseCode = "3002", description = "参数校验失败")
    @ApiResponse(responseCode = "3101", description = "用户已存在")
    public Result<Void> add(@Valid @RequestBody SysUserSaveDTO sysUserSaveDTO) {
        return toResult(sysUserService.add(sysUserSaveDTO));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "删除用户", description = "通过用户id删除用户")
    @Parameters(value = {
            @Parameter(name = "userId", in = ParameterIn.PATH, description = "用户ID")
    })
    @ApiResponse(responseCode = "1000", description = "成功删除用户")
    @ApiResponse(responseCode = "2000", description = "服务繁忙请稍后重试")
    @ApiResponse(responseCode = "3101", description = "用户不存在")
    public Result<Void> delete(@PathVariable Long userId) {
        return null;
    }

    @Operation(summary = "用户详情", description = "根据查询条件查询用户详情")
    @GetMapping("/detail")
    @Parameters(value = {
            @Parameter(name = "userId", in = ParameterIn.QUERY, description = "用户ID"),
            @Parameter(name = "gender", in = ParameterIn.QUERY, description = "用户性别")
    })
    @ApiResponse(responseCode = "1000", description = "成功获取用户信息")
    @ApiResponse(responseCode = "2000", description = "服务繁忙请稍后重试")
    @ApiResponse(responseCode = "3101", description = "用户不存在")
    public Result<SysUserSaveDTO> detail(Long userId, @RequestParam(required = false) String gender) {
        return null;
    }
}
