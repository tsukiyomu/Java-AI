package cn.bugstack.chatgpt.data.trigger.http;

import cn.bugstack.chatgpt.data.domain.account.model.entity.AdjustQuotaEntity;
import cn.bugstack.chatgpt.data.domain.account.model.valobj.AccountQuotaVO;
import cn.bugstack.chatgpt.data.domain.account.service.IAccountAdjustQuotaService;
import cn.bugstack.chatgpt.data.domain.account.service.IAccountQueryService;
import cn.bugstack.chatgpt.data.domain.auth.service.IAuthService;
import cn.bugstack.chatgpt.data.trigger.http.dto.AccountQuotaResponseDTO;
import cn.bugstack.chatgpt.data.trigger.http.dto.AdjustQuotaRequestDTO;
import cn.bugstack.chatgpt.data.trigger.http.dto.AdjustQuotaResponseDTO;
import cn.bugstack.chatgpt.data.trigger.http.dto.SaleProductDTO;
import cn.bugstack.chatgpt.data.types.common.Constants;
import cn.bugstack.chatgpt.data.types.exception.ChatGPTException;
import cn.bugstack.chatgpt.data.types.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 账户额度
 * @create 2024-10-06 09:20
 */
@Slf4j
@RestController()
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/account/")
public class AccountController {

    @Resource
    private Map<String, String> appTokenMap;
    @Resource
    private IAccountAdjustQuotaService accountAdjustQuotaService;
    @Resource
    private IAccountQueryService accountQueryService;
    @Resource
    private IAuthService authService;

    @RequestMapping(value = "query_account_quota", method = RequestMethod.POST)
    public Response<AccountQuotaResponseDTO> queryAccountQuota(@RequestHeader("Authorization") String token) {
        try {
            // 1. Token 校验
            boolean success = authService.checkToken(token);
            if (!success) {
                return Response.<AccountQuotaResponseDTO>builder()
                        .code(Constants.ResponseCode.TOKEN_ERROR.getCode())
                        .info(Constants.ResponseCode.TOKEN_ERROR.getInfo())
                        .build();
            }

            // 2. Token 解析
            String openid = authService.openid(token);
            assert null != openid;

            // 3. 查询额度
            AccountQuotaVO accountQuotaVO = accountQueryService.queryAccountQuota(openid);

            return Response.<AccountQuotaResponseDTO>builder()
                    .data(AccountQuotaResponseDTO.builder().totalQuota(accountQuotaVO.getTotalQuota()).surplusQuota(accountQuotaVO.getSurplusQuota()).build())
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("查询账户额度失败", e);
            return Response.<AccountQuotaResponseDTO>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 在 apipost/apifox 点击导入接口「curl」方式导入
     * <p>
     * curl -X POST \
     * http://localhost:8091/api/v1/account/adjust_quota \
     * -H 'Content-Type: application/json;charset=utf-8' \
     * -d '{
     * "appId": "big-market",
     * "appToken": "6ec604541f8b1ce4a",
     * "openid": "xfg",
     * "increaseQuota":10
     * }'
     */
    @RequestMapping(value = "adjust_quota", method = RequestMethod.POST)
    public Response<AdjustQuotaResponseDTO> adjustQuota(@RequestBody AdjustQuotaRequestDTO adjustQuotaRequestDTO) {
        log.info("{}账户调额开始: {}", adjustQuotaRequestDTO.getOpenid(), adjustQuotaRequestDTO.getIncreaseQuota());
        try {
            // 0. 参数校验
            if (StringUtils.isBlank(adjustQuotaRequestDTO.getOpenid()) || StringUtils.isBlank(adjustQuotaRequestDTO.getAppId()) || StringUtils.isBlank(adjustQuotaRequestDTO.getAppToken())) {
                throw new ChatGPTException(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), Constants.ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }

            // 1. appid、apptoken 校验，给调用接口的一方，配置请求参数。
            if (!adjustQuotaRequestDTO.getAppToken().equals(appTokenMap.get(adjustQuotaRequestDTO.getAppId()))) {
                throw new ChatGPTException(Constants.ResponseCode.APP_TOKEN_ERROR.getCode(), Constants.ResponseCode.APP_TOKEN_ERROR.getInfo());
            }

            // 2. 账户调额
            AccountQuotaVO accountQuotaVO = accountAdjustQuotaService.adjustQuota(AdjustQuotaEntity.builder()
                    .openid(adjustQuotaRequestDTO.getOpenid())
                    .increaseQuota(adjustQuotaRequestDTO.getIncreaseQuota())
                    .build());

            log.info("{}账户调额完成: {}", adjustQuotaRequestDTO.getOpenid(), accountQuotaVO.getTotalQuota());

            // 3. 封装参数
            return Response.<AdjustQuotaResponseDTO>builder()
                    .code(Constants.ResponseCode.SUCCESS.getCode())
                    .info(Constants.ResponseCode.SUCCESS.getInfo())
                    .data(AdjustQuotaResponseDTO.builder()
                            .totalQuota(accountQuotaVO.getTotalQuota())
                            .surplusQuota(accountQuotaVO.getSurplusQuota())
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("{}账户调额失败: {}", adjustQuotaRequestDTO.getOpenid(), adjustQuotaRequestDTO.getIncreaseQuota(), e);
            return Response.<AdjustQuotaResponseDTO>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

}
