package com.datalink.platform.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.datalink.platform.datasource.entity.Connector;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 连接器 Mapper
 */
@Mapper
public interface ConnectorMapper extends BaseMapper<Connector> {

    /**
     * 分页查询含关键字（name/host/database_name LIKE）
     */
    @Select("<script>SELECT * FROM connector WHERE enabled = 1 " +
            "<if test='keyword != null and keyword != \"\"'>AND (name LIKE CONCAT('%',#{keyword},'%') " +
            "OR host LIKE CONCAT('%',#{keyword},'%') OR database_name LIKE CONCAT('%',#{keyword},'%'))</if> " +
            "ORDER BY id DESC</script>")
    IPage<Connector> selectPageByKeyword(IPage<Connector> page, @Param("keyword") String keyword);
}
