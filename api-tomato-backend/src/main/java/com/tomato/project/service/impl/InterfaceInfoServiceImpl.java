package com.tomato.project.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tomato.apicommon.common.ErrorCode;
import com.tomato.apicommon.constant.CommonConstant;
import com.tomato.apicommon.model.dto.interfaceinfo.InterfaceInfoEditRequest;
import com.tomato.apicommon.model.dto.interfaceinfo.InterfaceInfoInvokeRequest;
import com.tomato.apicommon.model.dto.interfaceinfo.InterfaceInfoQueryRequest;
import com.tomato.apicommon.model.dto.interfaceinfo.InterfaceInfoUpdateRequest;
import com.tomato.apicommon.model.entity.InterfaceInfo;
import com.tomato.apicommon.model.entity.User;
import com.tomato.apicommon.model.entity.UserInterfaceInfo;
import com.tomato.apicommon.model.enums.InterfaceInfoStatusEnum;
import com.tomato.apicommon.model.vo.*;
import com.tomato.apicommon.utils.SqlUtils;
import com.tomato.apitomatoclientsdk.client.TomatoApiClient;
import com.tomato.apitomatoclientsdk.model.DefaultPath;
import com.tomato.project.exception.BusinessException;
import com.tomato.project.exception.ThrowUtils;
import com.tomato.project.mapper.InterfaceInfoMapper;
import com.tomato.project.service.InterfaceInfoService;
import com.tomato.project.service.UserInterfaceInfoService;
import com.tomato.project.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author tomato
 * @description 针对表【interface_info(接口信息)】的数据库操作Service实现
 */
@Service
@Slf4j
public class InterfaceInfoServiceImpl extends ServiceImpl<InterfaceInfoMapper, InterfaceInfo>
        implements InterfaceInfoService {

    @Resource
    private UserService userService;

    @Resource
    private UserInterfaceInfoService userInterfaceInfoService;

    @Autowired
    private InterfaceInfoMapper interfaceInfoMapper;

    // 创建单线程池
    private static final ExecutorService HandleInterface_EXECUTOR = Executors.newSingleThreadExecutor();


    @Override
    public void validInterfaceInfo(InterfaceInfo interfaceInfo, boolean add) {
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String name = interfaceInfo.getName();

        // String requestHeader = interfaceInfo.getRequestHeader();
        // String responseHeader = interfaceInfo.getResponseHeader();
        String method = interfaceInfo.getMethod();
        String requestParams = interfaceInfo.getRequestParams();
        String url = interfaceInfo.getUrl();
        String path = interfaceInfo.getPath();
        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(name, path, url, requestParams, method), ErrorCode.PARAMS_ERROR);
        }
        // 更新时只考虑参数是否合格
        // 有参数则校验
        if (StringUtils.isNotBlank(name) && name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称过长");
        }
        LoginUserVO loginUser = userService.getLoginUserByThreadLocal();
        if (!userService.isAdmin(loginUser)) {
            // 如果不是管理员，而接口路径又不是默认路径，抛异常
            ThrowUtils.throwIf(!interfaceInfo.getPath().equals(DefaultPath.PATH), ErrorCode.PARAMS_ERROR);
        }
        // todo 还应该校验接口url、host是否合法。
    }

    @Override
    public QueryWrapper<InterfaceInfo> getQueryWrapper(InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
        if (interfaceInfoQueryRequest == null) {
            return queryWrapper;
        }
        Long id = interfaceInfoQueryRequest.getId();
        String name = interfaceInfoQueryRequest.getName();
        String description = interfaceInfoQueryRequest.getDescription();
        Long userId = interfaceInfoQueryRequest.getUserId();
        String sortField = interfaceInfoQueryRequest.getSortField();
        String sortOrder = interfaceInfoQueryRequest.getSortOrder();
        String method = interfaceInfoQueryRequest.getMethod();
        Integer status = interfaceInfoQueryRequest.getStatus();
        String searchText = interfaceInfoQueryRequest.getSearchText();
        Date createTime = interfaceInfoQueryRequest.getCreateTime();

        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.like("name", searchText).or().like("description", searchText);
        }
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.like(StringUtils.isNotBlank(description), "description", description);

        queryWrapper.eq(ObjectUtils.isNotEmpty(status), "status", status);
        queryWrapper.gt(ObjectUtils.isNotEmpty(createTime), "createTime", createTime);
        queryWrapper.like(StringUtils.isNotBlank(method), "method", method);

        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);

        return queryWrapper;
    }


    @Override
    public InterfaceInfoVO getInterfaceInfoVO(InterfaceInfo interfaceInfo) {
        InterfaceInfoVO interfaceInfoVO = InterfaceInfoVO.objToVo(interfaceInfo);
        // 1. 关联查询用户信息
        Long userId = interfaceInfo.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userService.getById(userId);
        }
        UserVO userVO = userService.getUserVO(user);
        interfaceInfoVO.setUser(userVO);
        // 封装请求参数说明 和 响应参数说明
        List<RequestParamsRemarkVO> requestParamsRemarkVOList = JSONUtil.toList(JSONUtil.parseArray(interfaceInfo.getRequestParamsRemark()), RequestParamsRemarkVO.class);
        List<ResponseParamsRemarkVO> responseParamsRemarkVOList = JSONUtil.toList(JSONUtil.parseArray(interfaceInfo.getResponseParamsRemark()), ResponseParamsRemarkVO.class);
        interfaceInfoVO.setRequestParamsRemark(requestParamsRemarkVOList);
        interfaceInfoVO.setResponseParamsRemark(responseParamsRemarkVOList);
        return interfaceInfoVO;
    }

    @Override
    public Page<InterfaceInfoVO> getInterfaceInfoVOPage(Page<InterfaceInfo> interfaceInfoPage, HttpServletRequest request) {
        List<InterfaceInfo> interfaceInfoList = interfaceInfoPage.getRecords();
        Page<InterfaceInfoVO> interfaceInfoVOPage = new Page<>(interfaceInfoPage.getCurrent(), interfaceInfoPage.getSize(), interfaceInfoPage.getTotal());
        // 获取当前登录用户
        //User loginUser = userService.getLoginUserBySession(request);
        LoginUserVO loginUser = userService.getLoginUserByThreadLocal();
        if (CollectionUtils.isEmpty(interfaceInfoList)) {
            return interfaceInfoVOPage;
        }
        // 1. 关联查询用户信息
        Set<Long> userIdSet = interfaceInfoList.stream().map(InterfaceInfo::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        List<InterfaceInfoVO> interfaceInfoVOList = interfaceInfoList.stream()
                .map(interfaceInfo -> {
                    InterfaceInfoVO interfaceInfoVO = InterfaceInfoVO.objToVo(interfaceInfo);
                    // 创建人的用户ID
                    Long userId = interfaceInfo.getUserId();

                    // 查询当前登录用户的接口调用次数
                    UserInterfaceInfo userInterfaceInfo = userInterfaceInfoService.lambdaQuery()
                            .eq(UserInterfaceInfo::getUserId, loginUser.getId())
                            .eq(UserInterfaceInfo::getInterfaceInfoId, interfaceInfo.getId())
                            .one();

                    if (userInterfaceInfo != null) {
                        interfaceInfoVO.setTotalNum(userInterfaceInfo.getTotalNum());
                        interfaceInfoVO.setLeftNum(userInterfaceInfo.getLeftNum());
                        //是否开通了接口
                        interfaceInfoVO.setIsOpenInterface(true);
                    }

                    // 获取创建接口的用户信息
                    User user = userIdUserListMap.getOrDefault(userId, Collections.emptyList()).stream().findFirst().orElse(null);
                    interfaceInfoVO.setUser(userService.getUserVO(user));

                    // 封装请求参数说明和响应参数说明
                    List<RequestParamsRemarkVO> requestParamsRemarkVOList = JSONUtil.toList(JSONUtil.parseArray(interfaceInfo.getRequestParamsRemark()), RequestParamsRemarkVO.class);
                    List<ResponseParamsRemarkVO> responseParamsRemarkVOList = JSONUtil.toList(JSONUtil.parseArray(interfaceInfo.getResponseParamsRemark()), ResponseParamsRemarkVO.class);
                    interfaceInfoVO.setRequestParamsRemark(requestParamsRemarkVOList);
                    interfaceInfoVO.setResponseParamsRemark(responseParamsRemarkVOList);
                    // 判断并设置是否为当前用户拥有的接口
                    interfaceInfoVO.setIsOwnerByCurrentUser(isOwnedByCurrentUser(interfaceInfo, loginUser.getId()));
                    return interfaceInfoVO;
                })
                .collect(Collectors.toList());

        interfaceInfoVOPage.setRecords(interfaceInfoVOList);
        return interfaceInfoVOPage;
    }

    @Override
    public boolean updateInterfaceInfo(InterfaceInfoEditRequest interfaceInfoUpdateRequest) {
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        BeanUtils.copyProperties(interfaceInfoUpdateRequest, interfaceInfo);
        this.validInterfaceInfo(interfaceInfo, false);
        interfaceInfo.setRequestParamsRemark(JSONUtil.toJsonStr(interfaceInfoUpdateRequest.getRequestParamsRemark()));
        interfaceInfo.setResponseParamsRemark(JSONUtil.toJsonStr(interfaceInfoUpdateRequest.getResponseParamsRemark()));

        return this.updateById(interfaceInfo);
    }

    @Override
    public IPage<InterfaceInfoVO> getInterfaceInfoVOByUserIdPage(InterfaceInfoQueryRequest interfaceInfoQueryRequest) {
        //创建Page对象
        Page<InterfaceInfo> pageParam = new Page<>(interfaceInfoQueryRequest.getCurrent(),interfaceInfoQueryRequest.getPageSize());
        Long userId = userService.getLoginUserByThreadLocal().getId();
        //查询当前用户拥有的接口的wrapper
        LambdaQueryWrapper<InterfaceInfo> interfaceInfoLambdaQueryWrapper = new QueryWrapper<InterfaceInfo>()
                .lambda().inSql(InterfaceInfo::getId,
                        "select interfaceInfoId from user_interface_info where userId = " + userId);
        Page<InterfaceInfo> interfaceInfoPage = interfaceInfoMapper.selectPage(pageParam, interfaceInfoLambdaQueryWrapper);
        List<InterfaceInfo> records = interfaceInfoPage.getRecords();
        //返回的VOPage对象
        Page<InterfaceInfoVO> interfaceInfoVOPage = new Page<>(interfaceInfoPage.getCurrent(), interfaceInfoPage.getSize(), interfaceInfoPage.getTotal());
        if (CollectionUtils.isEmpty(records)) {
            return interfaceInfoVOPage;
        }
        //填充接口信息
        List<InterfaceInfoVO> interfaceInfoVOList = records.stream().map(interfaceInfo -> {
            InterfaceInfoVO interfaceInfoVO = InterfaceInfoVO.objToVo(interfaceInfo);
            UserInterfaceInfo userInterfaceInfo = userInterfaceInfoService.lambdaQuery()
                    .eq(UserInterfaceInfo::getUserId, userId)
                    .eq(UserInterfaceInfo::getInterfaceInfoId, interfaceInfo.getId())
                    .one();
            //不需要判空了 因为已经筛选过一次 循环都是此用户拥有的接口 保险起见还是判断一下
            if(userInterfaceInfo!=null){
                interfaceInfoVO.setTotalNum(userInterfaceInfo.getTotalNum());
                interfaceInfoVO.setLeftNum(userInterfaceInfo.getLeftNum());
                // 封装请求参数说明和响应参数说明
                List<RequestParamsRemarkVO> requestParamsRemarkVOList = JSONUtil.toList(JSONUtil.parseArray(interfaceInfo.getRequestParamsRemark()), RequestParamsRemarkVO.class);
                List<ResponseParamsRemarkVO> responseParamsRemarkVOList = JSONUtil.toList(JSONUtil.parseArray(interfaceInfo.getResponseParamsRemark()), ResponseParamsRemarkVO.class);
                interfaceInfoVO.setRequestParamsRemark(requestParamsRemarkVOList);
                interfaceInfoVO.setResponseParamsRemark(responseParamsRemarkVOList);
                // 看是不是本人创建的，并设置
                interfaceInfoVO.setIsOwnerByCurrentUser(isOwnedByCurrentUser(interfaceInfo, userId));
                return interfaceInfoVO;
            }else{
                return null;
            }
        }).collect(Collectors.toList());

        interfaceInfoVOPage.setRecords(interfaceInfoVOList);

        return interfaceInfoVOPage;
    }

    private static boolean isOwnedByCurrentUser(InterfaceInfo interfaceInfo, Long userId) {
        // 判断并设置是否为当前用户拥有的接口
        return interfaceInfo.getUserId().equals(userId);
    }

//    @Override
//    public Page<InterfaceInfoVO> getInterfaceInfoVOByUserIdPage(Page<InterfaceInfo> interfaceInfoPage, InterfaceInfoQueryRequest request) {
//        List<InterfaceInfo> interfaceInfoList = interfaceInfoPage.getRecords();
//        Page<InterfaceInfoVO> interfaceInfoVOPage = new Page<>(interfaceInfoPage.getCurrent(), interfaceInfoPage.getSize(), interfaceInfoPage.getTotal());
//        if (CollectionUtils.isEmpty(interfaceInfoList)) {
//            return interfaceInfoVOPage;
//        }
//        // 传入当前用户ID
//        //User loginUser = userService.getLoginUserBySession(request);
//        Long userId = userService.getLoginUserByThreadLocal().getId();
//        // 过滤掉不是当前用户的接口，并且填充信息
//        List<InterfaceInfoVO> interfaceInfoVOList = interfaceInfoList.stream()
//                .map(interfaceInfo -> {
//                    InterfaceInfoVO interfaceInfoVO = InterfaceInfoVO.objToVo(interfaceInfo);
//                    UserInterfaceInfo userInterfaceInfo = userInterfaceInfoService.lambdaQuery()
//                            .eq(UserInterfaceInfo::getUserId, userId)
//                            .eq(UserInterfaceInfo::getInterfaceInfoId, interfaceInfo.getId())
//                            .one();
//                    if (userInterfaceInfo != null) {
//                        interfaceInfoVO.setTotalNum(userInterfaceInfo.getTotalNum());
//                        interfaceInfoVO.setLeftNum(userInterfaceInfo.getLeftNum());
//                        // 封装请求参数说明和响应参数说明
//                        List<RequestParamsRemarkVO> requestParamsRemarkVOList = JSONUtil.toList(JSONUtil.parseArray(interfaceInfo.getRequestParamsRemark()), RequestParamsRemarkVO.class);
//                        List<ResponseParamsRemarkVO> responseParamsRemarkVOList = JSONUtil.toList(JSONUtil.parseArray(interfaceInfo.getResponseParamsRemark()), ResponseParamsRemarkVO.class);
//                        interfaceInfoVO.setRequestParamsRemark(requestParamsRemarkVOList);
//                        interfaceInfoVO.setResponseParamsRemark(responseParamsRemarkVOList);
//                        // 看是不是本人创建的，并设置
//                        interfaceInfoVO.setIsOwnerByCurrentUser(isOwnedByCurrentUser(interfaceInfo, userId));
//                        return interfaceInfoVO;
//                    } else {
//                        return null;
//                    }
//                })
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
////        long newCount = interfaceInfoPage.getTotal() - (interfaceInfoList.size() - interfaceInfoVOList.size())
////如果过滤掉数据的话 以下方法只适用于下一页都是有效接口的情况 所以还是先过滤后分页
////        if (interfaceInfoList.size() > interfaceInfoVOList.size() && newCount > 0) {
////            InterfaceInfoQueryRequest newRequest = new InterfaceInfoQueryRequest();
////            BeanUtils.copyProperties(interfaceInfoQueryRequest, newRequest);
////            // 如果过滤掉了n个元素 分页查询下一页的n元素
////            Page<InterfaceInfo> nextInterface = this.page(new Page<>(newRequest.getCurrent() + 1, interfaceInfoList.size()),
////                    this.getQueryWrapper(newRequest));
////            List<InterfaceInfo> nextInterfaceRecords = nextInterface.getRecords();
////            for (InterfaceInfo i :
////                    nextInterfaceRecords) {
////                InterfaceInfoVO userOpenInterface = isUserOpenInterface(i, userId);
////                if (userOpenInterface != null) {
////                    interfaceInfoVOList.add(userOpenInterface);
////                }
////            }
////            //添加了下一页的数据上去 所以减去添加的数据的数量
////            interfaceInfoVOPage.setTotal(newCount);
////        }
//        interfaceInfoVOPage.setRecords(interfaceInfoVOList);
//        return interfaceInfoVOPage;
//    }

//    public InterfaceInfoVO isUserOpenInterface(InterfaceInfo interfaceInfo, Long userId) {
//        UserInterfaceInfo userInterfaceInfo = userInterfaceInfoService.lambdaQuery()
//                .eq(UserInterfaceInfo::getUserId, userId)
//                .eq(UserInterfaceInfo::getInterfaceInfoId, interfaceInfo.getId())
//                .one();
//        InterfaceInfoVO interfaceInfoVO = InterfaceInfoVO.objToVo(interfaceInfo);
//        return getInterfaceInfoVO(interfaceInfo, userId, userInterfaceInfo, interfaceInfoVO);
//    }

    @Override
    @Transactional  //当前方法添加了事务管理
    public boolean removeByIdTranslator(long id) {
        // 查询和接口名一样的用户接口关系表
        List<Long> userInterfaceInfoIdList = userInterfaceInfoService.lambdaQuery()
                .eq(UserInterfaceInfo::getInterfaceInfoId, id).list()
                .stream().map(UserInterfaceInfo::getId).collect(Collectors.toList());
        boolean b = this.removeById(id);
        if (b) {
            return userInterfaceInfoService.removeBatchByIds(userInterfaceInfoIdList);
        }
        return false;
    }

    @Override
    public boolean removeByIdsTranslator(List<Long> ids) {
        for (Long id : ids) {
            InterfaceInfoService proxy = (InterfaceInfoService) AopContext.currentProxy();
            proxy.removeByIdTranslator(id);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean addInterface(InterfaceInfo interfaceInfo) {
        boolean success = this.save(interfaceInfo);
        if (success) {
            UserInterfaceInfo userInterfaceInfo = new UserInterfaceInfo();
            userInterfaceInfo.setInterfaceInfoId(interfaceInfo.getId());
            return userInterfaceInfoService.addUserInterface(userInterfaceInfo);
        } else return false;
    }

    @Override
    public String getInvokeResult(InterfaceInfoInvokeRequest interfaceInfoInvokeRequest, HttpServletRequest request, InterfaceInfo oldInterfaceInfo) {
        // 接口请求地址
        Long id = oldInterfaceInfo.getId();
        String url = oldInterfaceInfo.getUrl();
        String method = oldInterfaceInfo.getMethod();
        // 接口请求路径
        String path = oldInterfaceInfo.getPath();
        String requestParams = interfaceInfoInvokeRequest.getUserRequestParams();
        // 获取SDK客户端，并根据yml里配置的网关地址设置tomatoApiClient的网关地址
        TomatoApiClient tomatoApiClient = userService.getTomatoApiClient(request);
        //log.info("generate sdk {} done ",tomatoApiClient);
        String invokeResult = null;
        try {
            // 执行方法
            invokeResult = tomatoApiClient.invokeInterface(id, requestParams, url, method, path);
        } catch (Exception e) {
            // 调用失败，开子线程使用默认参数确认接口是否可用
//            tryAgainUsingOriginalParam(oldInterfaceInfo, id, url, method, path, requestParams, tomatoApiClient);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接口调用失败");
        }
        // 走到下面，接口肯定调用成功了
        // 如果调用出现了接口内部异常或者路径错误，需要下线接口（网关已经将异常结果统一处理了）
        if (StrUtil.isBlank(invokeResult)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接口返回值为空");
        } else {
            JSONObject jsonObject;
            try {
                jsonObject = JSONUtil.parseObj(invokeResult);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接口响应参数不规范");//JSON转化失败，响应数据不是JSON格式
            }
            int code = (int) Optional.ofNullable(jsonObject.get("code")).orElse("-1");//要求接口返回必须是统一响应格式
            ThrowUtils.throwIf(code == -1, ErrorCode.SYSTEM_ERROR, "接口响应参数不规范");//响应参数里不包含code

            if (code == ErrorCode.SYSTEM_ERROR.getCode()) {
                offlineInterface(id);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "接口异常，即将关闭接口");
            } else if (code == ErrorCode.NOT_FOUND_ERROR.getCode()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "接口路径不存在");
            }
            // 请求参数错误
            else if (code == ErrorCode.PARAMS_ERROR.getCode()) {
                throw new BusinessException((ErrorCode.PARAMS_ERROR));
            }
            return invokeResult;
        }
    }

    /**
     * 使用原始参数再试一遍接口是否可用，开异步
     */
    private void tryAgainUsingOriginalParam(InterfaceInfo oldInterfaceInfo, Long id, String url, String method, String path, String requestParams, TomatoApiClient tomatoApiClient) {
        HandleInterface_EXECUTOR.submit(() -> {
            try {
                // 1.查询初始参考请求参数
                String tempRequestParams = oldInterfaceInfo.getRequestParams();
                // 如果请求参数和默认参数一致，证明是发布接口请求，直接返回即可，不用关闭
                ThrowUtils.throwIf(requestParams.equals(tempRequestParams), ErrorCode.SYSTEM_ERROR, "接口验证失败");
                // 2.使用原始参考请求参数调用接口，不需要返回值，只需要看这个过程有没有抛异常
                String result = tomatoApiClient.invokeInterface(id, tempRequestParams, url, method, path);

            } catch (Exception ee) {
                // 还是抛异常，下线该接口
                offlineInterface(id);
                // 5.打印失败日志，就不抛异常了。
                ee.printStackTrace();
            }
        });
    }

    @Override
    public boolean offlineInterface(long id) {
        // 3.调用失败，关闭该接口
        InterfaceInfo interfaceInfo = new InterfaceInfo();
        interfaceInfo.setId(id);
        interfaceInfo.setStatus(InterfaceInfoStatusEnum.OFFLINE.getValue());//下线接口
        boolean success = this.updateById(interfaceInfo);
        if (!success) return false;
            // 4.输出关闭接口的日志
        else {
            log.info("下线接口{}成功...", id);
            return true;
        }
    }


}





