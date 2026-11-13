package cn.easykits.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.easykits.crm.entity.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
