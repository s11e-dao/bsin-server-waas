package me.flyray.bsin.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import me.flyray.bsin.domain.entity.WalletAccount;
import me.flyray.bsin.mybatis.utils.Pagination;


@Data
public class WalletAccountDTO extends WalletAccount {

    @NotNull(message = "分页不能为空！")
    private Pagination pagination;
}
