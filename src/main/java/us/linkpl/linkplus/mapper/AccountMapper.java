package us.linkpl.linkplus.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.ResponseBody;
import us.linkpl.linkplus.entity.Account;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author samsara
 * @since 2021-06-03
 */
@Repository
public interface AccountMapper extends BaseMapper<Account> {

    @Select("SELECT * FROM account as u JOIN (SELECT ROUND(RAND() * (SELECT MAX(id) FROM account)) AS id ) AS u2 WHERE u.id >= u2.id ORDER BY u.id DESC LIMIT #{num}")
    List<Account> selectRandomAccount(@Param("num") String num);
}
