package cn.bugstack.chatgpt.data.domain.account.service;

import cn.bugstack.chatgpt.data.domain.account.model.valobj.AccountQuotaVO;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 账户查询服务
 * @create 2024-10-19 09:20
 */
public interface IAccountQueryService {

    AccountQuotaVO queryAccountQuota(String openid);

}
