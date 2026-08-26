package com.train.ticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.train.ticket.dto.StoTTransferDTO;
import com.train.ticket.dto.TtoETransferDTO;
import com.train.ticket.entity.Train;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 车票查询自定义Mapper：直达SQL自连接 + 中转两段查询
 */
@Mapper
public interface TrainTicketMapper extends BaseMapper<Train> {

    /**
     * 直达车票查询（按站点）：train_station_route自连接
     */
    @Select("""
        <script>
        <![CDATA[
        SELECT DISTINCT t.*
        FROM train t
        INNER JOIN train_station_route r_s ON t.train_id = r_s.train_id AND r_s.station_id = #{startStationId}
        INNER JOIN train_station_route r_e ON t.train_id = r_e.train_id AND r_e.station_id = #{endStationId}
        WHERE r_s.sort < r_e.sort
          AND DATE(t.depart_datetime) = #{queryDate}
          AND t.sale_status = 1
        ORDER BY t.depart_datetime
        ]]>
        </script>
        """)
    List<Train> searchDirectTicket(
            @Param("startStationId") Long startStationId,
            @Param("endStationId") Long endStationId,
            @Param("queryDate") LocalDate queryDate);

    /**
     * 城市直达查询：始发城市任意站点到终点城市任意站点
     */
    @Select("""
        <script>
        <![CDATA[
        SELECT DISTINCT t.*
        FROM train t
        INNER JOIN train_station_route r_s ON t.train_id = r_s.train_id
        INNER JOIN station s_s ON s_s.station_id = r_s.station_id AND s_s.city = #{startCity}
        INNER JOIN train_station_route r_e ON t.train_id = r_e.train_id
        INNER JOIN station s_e ON s_e.station_id = r_e.station_id AND s_e.city = #{endCity}
        WHERE r_s.sort < r_e.sort
          AND DATE(t.depart_datetime) = #{queryDate}
          AND t.sale_status = 1
        ORDER BY t.depart_datetime
        ]]>
        </script>
        """)
    List<Train> searchDirectTicketByCity(
            @Param("startCity") String startCity,
            @Param("endCity") String endCity,
            @Param("queryDate") LocalDate queryDate);

    /**
     * 中转步骤1：从出发城市任意站点出发，当天可以到达的所有中转站T
     */
    @Select("""
        <script>
        <![CDATA[
        SELECT DISTINCT
            r_e.station_id AS transferStationId,
            t.train_id AS trainId1,
            r_e.arrive_datetime AS arriveTimeTransfer,
            t.train_no AS trainNo1,
            s_s.station_name AS startStationName,
            s_e.station_name AS transferStationName,
            r_s.depart_datetime AS departTime1
        FROM train t
        INNER JOIN train_station_route r_s ON t.train_id = r_s.train_id
        INNER JOIN station s_s ON s_s.station_id = r_s.station_id AND s_s.city = #{startCity}
        INNER JOIN train_station_route r_e ON t.train_id = r_e.train_id
        INNER JOIN station s_e ON s_e.station_id = r_e.station_id AND s_e.city != #{endCity}
        WHERE r_s.sort < r_e.sort
          AND DATE(t.depart_datetime) = #{queryDate}
          AND t.sale_status = 1
        ]]>
        </script>
        """)
    List<StoTTransferDTO> getCanArriveTransferStation(
            @Param("startCity") String startCity,
            @Param("endCity") String endCity,
            @Param("queryDate") LocalDate queryDate);

    /**
     * 中转步骤2：从一批中转站T出发，可以到达终点城市任意站点的行程
     */
    @Select("""
        <script>
        SELECT DISTINCT
            r_s.station_id AS transferStationId,
            t.train_id AS trainId2,
            r_s.depart_datetime AS departTimeTransfer,
            t.train_no AS trainNo2,
            s_s.station_name AS transferStationName,
            s_e.station_name AS endStationName,
            r_e.arrive_datetime AS arriveTime2
        FROM train t
        INNER JOIN train_station_route r_s ON t.train_id = r_s.train_id
        INNER JOIN station s_s ON s_s.station_id = r_s.station_id AND s_s.station_id IN
        <foreach collection="stationIdList" open="(" separator="," close=")">
            #{item}
        </foreach>
        INNER JOIN train_station_route r_e ON t.train_id = r_e.train_id
        INNER JOIN station s_e ON s_e.station_id = r_e.station_id AND s_e.city = #{endCity}
        WHERE r_s.sort &lt; r_e.sort
          AND DATE(t.depart_datetime) = #{queryDate}
          AND t.sale_status = 1
        </script>
        """)
    List<TtoETransferDTO> getTransferToEnd(
            @Param("stationIdList") List<Long> stationIdList,
            @Param("endCity") String endCity,
            @Param("queryDate") LocalDate queryDate);
}
